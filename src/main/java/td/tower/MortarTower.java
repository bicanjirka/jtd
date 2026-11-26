package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.projectile.CannonballProjectile;
import td.projectile.ProjectileStats;
import td.projectile.ShellLook;
import td.stat.StatPart;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.mortar.BarrageMechanic;
import td.tower.mortar.BigBoreMechanic;
import td.tower.mortar.BlastMark;
import td.tower.mortar.CarpetBombingMechanic;
import td.tower.mortar.ConcussionMechanic;
import td.tower.mortar.CrackingShellsMechanic;
import td.tower.mortar.DeepCrackMechanic;
import td.tower.mortar.FlareMechanic;
import td.tower.mortar.FusionMechanic;
import td.tower.mortar.HeavyRider;
import td.tower.mortar.HeldShell;
import td.tower.mortar.LastingMixMechanic;
import td.tower.mortar.LinedUpShell;
import td.tower.mortar.Magazine;
import td.tower.mortar.MagazineSpec;
import td.tower.mortar.MagazineView;
import td.tower.mortar.MortarMechanic;
import td.tower.mortar.MortarSpec;
import td.tower.mortar.PathLine;
import td.tower.mortar.PayloadMechanic;
import td.tower.mortar.PredictiveFireMechanic;
import td.tower.mortar.RichMixMechanic;
import td.tower.mortar.ShellKind;
import td.tower.mortar.ShellProfile;
import td.tower.mortar.ShrapnelMechanic;
import td.tower.mortar.ThickMixMechanic;
import td.tower.mortar.VolleySpec;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitKind;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.PickContext;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.TargetPriority;
import td.tower.targeting.TargetSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.RangeNodes;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.PathRuntime;
import td.util.ThreadConfined;
import td.util.TickRate;
import td.wave.Path;
import td.wave.Vec2;
import td.zone.Zone;
import td.zone.ZoneEffects;
import td.zone.ZoneOwner;
import td.zone.ZonePulse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;

import static td.tower.upgrade.MechanicCatalogue.entry;

/**
 * The area cannon: it lobs a slow, unguided shell at the visible enemy furthest along the path, beyond its minimum
 * range, and the shell bursts where it lands, at full damage in its core and less at its edge, hidden enemies
 * included. It fires the shells of its magazine in order, each kind drawn by lot as it joins
 * the back: Normals, Heavies, and the kinds its nodes add.
 * <p>
 * What each owned node does lives in its data or in a {@link MortarMechanic}: the tower reads one spec from them
 * whenever it draws, fires or lands a shell.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class MortarTower extends AbstractTower {

    public static final int PRICE = 20;
    public static final float DAMAGE_POINTS = 32f;
    public static final float RANGE = 4.5f;
    public static final float CORE_CELLS = 1f;
    public static final float EDGE_CELLS = 1.75f;
    /** Pixels a tick: slow enough to watch a shell fly and for a fast enemy to dodge it. */
    public static final float SHELL_SPEED = 6.4f;

    private static final float MINIMUM_RANGE_CELLS = 1.5f;
    /** What a Heavy shell deals past the Mortar's damage, in points. */
    private static final float HEAVY_POINTS = 24f;
    /** A rate below this counts as it, so a stalled tower's reload is long rather than endless. */
    private static final double MINIMUM_RATE = 1e-3;
    /** A shot every 3.5 s. */
    private static final int COOLDOWN_MAX = 69;
    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    static final int CRACKED_TICKS = 100;
    /** Flight time depends on where the shell is going, so the aim is refined this many times. */
    private static final int PREDICTION_PASSES = 3;

    private static final UpgradeNode HEAVY_CHARGE = UpgradeNode.of("mortar.heavy_charge", "Heavy Charge", 5)
            .withModifiers(TowerModifiers.damage(12f));
    private static final UpgradeNode PACKED_CHARGE = UpgradeNode.of("mortar.packed_charge", "Packed Charge", 5)
            .withModifiers(TowerModifiers.normalDamage(0f));
    private static final UpgradeNode WIDE_CRATER = UpgradeNode.of("mortar.wide_crater", "Wide Crater", 5)
            .withModifiers(TowerModifiers.heavyBlast(0f));
    private static final UpgradeNode BIG_BORE = UpgradeNode.of("mortar.big_bore", "Big Bore", 5)
            .withModifiers(TowerModifiers.heavyDamage(14f));
    private static final UpgradeNode CRACKING_SHELLS = UpgradeNode.of("mortar.cracking_shells", "Cracking Shells", 5);
    private static final UpgradeNode PREDICTIVE_FIRE = UpgradeNode.of("mortar.predictive_fire", "Predictive Fire", 5)
            .withTargeting(TargetPriority.BIGGEST_GROUP_AHEAD);

    private static final UpgradeNode CONCUSSION = UpgradeNode.of("mortar.concussion", "Concussion", 10)
            .inTier(1);
    private static final UpgradeNode SHRAPNEL = UpgradeNode.of("mortar.shrapnel", "Shrapnel", 10)
            .inTier(1);
    private static final UpgradeNode NAPALM = UpgradeNode.of("mortar.napalm", "Napalm", 15)
            .inTier(1);
    private static final UpgradeNode TAR_SHELL = UpgradeNode.of("mortar.tar_shell", "Tar Shell", 15)
            .inTier(1);
    private static final UpgradeNode CRYO = UpgradeNode.of("mortar.cryo", "Cryo", 15)
            .inTier(1);
    private static final UpgradeNode FLARE = UpgradeNode.of("mortar.flare", "Flare", 5)
            .inTier(1);
    private static final UpgradeCondition A_PAYLOAD = UpgradeCondition.owns(NAPALM.id())
            .or(UpgradeCondition.owns(TAR_SHELL.id())).or(UpgradeCondition.owns(CRYO.id()));

    private static final UpgradeNode BARRAGE = UpgradeNode.of("mortar.barrage", "Barrage", 50)
            .inTier(3);
    private static final UpgradeNode CARPET_BOMBING = UpgradeNode.of("mortar.carpet_bombing", "Carpet Bombing", 50)
            .inTier(3);
    private static final UpgradeNode QUICK_LOAD = UpgradeNode.of("mortar.quick_load", "Quick Load", 50)
            .withModifiers(TowerModifiers.normalLoad(-0.5f))
            .inTier(3);
    private static final UpgradeNode ELEVATION = UpgradeNode.of(RangeNodes.RANGE_ID, "Elevation", 20)
            .withModifiers(TowerModifiers.range(1.6f))
            .inTier(2);
    private static final UpgradeNode OVERCHARGE = UpgradeNode.of("mortar.overcharge", "Overcharge", 25)
            .withModifiers(TowerModifiers.heavyDamage(64f))
            .inTier(2);
    private static final UpgradeNode BIG_BORE_2 = UpgradeNode.of("mortar.big_bore_2", "Big Bore II", 20)
            .withModifiers(TowerModifiers.heavyDamage(42f))
            .inTier(2);
    private static final UpgradeNode THICK_MIX = UpgradeNode.of("mortar.thick_mix", "Thick Mix", 20)
            .withRequires(A_PAYLOAD)
            .inTier(2);

    private static final UpgradeNode FUSION = UpgradeNode.of("mortar.fusion", "Fusion", 40)
            .inTier(3);
    private static final UpgradeNode RICH_MIX = UpgradeNode.of("mortar.rich_mix", "Rich Mix", 40)
            .inTier(3);
    private static final UpgradeNode LASTING_MIX = UpgradeNode.of("mortar.lasting_mix", "Lasting Mix", 30)
            .after(THICK_MIX)
            .withOwnArt()
            .inTier(3);
    private static final UpgradeNode DEEP_CRACK = UpgradeNode.of("mortar.deep_crack", "Deep Crack", 15)
            .after(CRACKING_SHELLS)
            .withOwnArt()
            .inTier(2);
    private static final UpgradeNode QUICK_RELOAD = UpgradeNode.of("mortar.quick_reload", "Quick Reload", 40)
            .withModifiers(TowerModifiers.attackSpeed(3.5f / 3f - 1f))
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(HEAVY_CHARGE, BIG_BORE, PREDICTIVE_FIRE, CRACKING_SHELLS,
                    PACKED_CHARGE, WIDE_CRATER))
            .with(CONCUSSION, SHRAPNEL, NAPALM, TAR_SHELL, CRYO, FLARE,
                    BARRAGE, CARPET_BOMBING, QUICK_LOAD, ELEVATION, OVERCHARGE, BIG_BORE_2, THICK_MIX,
                    FUSION, RICH_MIX, LASTING_MIX, DEEP_CRACK, QUICK_RELOAD)
            .withCluster(UpgradeCluster.of("Shell charge", PACKED_CHARGE, WIDE_CRATER))
            .withCluster(UpgradeCluster.of("Heavy shell", CONCUSSION, SHRAPNEL))
            .withCluster(UpgradeCluster.of("Payload", NAPALM, TAR_SHELL, CRYO))
            .withCluster(UpgradeCluster.of("Loading", BARRAGE, CARPET_BOMBING, QUICK_LOAD))
            .withCluster(UpgradeCluster.of("Order", FUSION, RICH_MIX));

    private static final MechanicCatalogue<MortarMechanic> MECHANICS = MechanicCatalogue.of(
            entry(BIG_BORE.id(), () -> new BigBoreMechanic(1)),
            entry(PREDICTIVE_FIRE.id(), PredictiveFireMechanic::new),
            entry(CRACKING_SHELLS.id(), CrackingShellsMechanic::new),
            entry(CONCUSSION.id(), ConcussionMechanic::new),
            entry(SHRAPNEL.id(), ShrapnelMechanic::new),
            entry(NAPALM.id(), () -> new PayloadMechanic(ShellKind.NAPALM)),
            entry(TAR_SHELL.id(), () -> new PayloadMechanic(ShellKind.TAR)),
            entry(CRYO.id(), () -> new PayloadMechanic(ShellKind.CRYO)),
            entry(FLARE.id(), FlareMechanic::new),
            entry(BARRAGE.id(), BarrageMechanic::new),
            entry(CARPET_BOMBING.id(), CarpetBombingMechanic::new),
            entry(BIG_BORE_2.id(), () -> new BigBoreMechanic(1)),
            entry(THICK_MIX.id(), ThickMixMechanic::new),
            entry(FUSION.id(), FusionMechanic::new),
            entry(RICH_MIX.id(), RichMixMechanic::new),
            entry(LASTING_MIX.id(), LastingMixMechanic::new),
            entry(DEEP_CRACK.id(), DeepCrackMechanic::new));

    private static final TowerVisitor<Optional<MortarTower>> AS_MORTAR = new TowerVisitor<>() {
        @Override
        public Optional<MortarTower> visitSniperTower(SniperTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<MortarTower> visitSplashTower(SplashTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<MortarTower> visitPulseTower(PulseTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<MortarTower> visitAuraTower(AuraTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<MortarTower> visitMortarTower(MortarTower tower) {
            return Optional.of(tower);
        }

        @Override
        public Optional<MortarTower> visitSeekerTower(SeekerTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<MortarTower> visitCinderTower(CinderTower tower) {
            return Optional.empty();
        }
    };

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK, this.restHeading());
    private final OwnedMechanics<MortarMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private final ZoneOwner zoneOwner = new ZoneOwner() {
        @Override
        public void applyEffect(EnemyMob target, Function<DamageSink, Effect> effect) {
            MortarTower.this.applyEffect(target, effect);
        }

        @Override
        public void dealPeriodicDamage(EnemyMob target, Damage damage) {
            MortarTower.this.dealPeriodicDamage(target, damage);
        }
    };
    private final List<BlastMark> blastMarks = new ArrayList<>();
    private final List<Zone> flares = new ArrayList<>();
    private final List<VolleyShell> volleyLeft = new ArrayList<>();
    /** Build state, not fight state: a wave starts from the magazine the last one left, and the save keeps it. */
    private volatile Magazine magazine = Magazine.opener();
    private volatile int loading = 1;
    private FireClock clock = new FireClock(COOLDOWN_MAX);
    /** How many periods the reload under way takes: the volley's factor times the load time of what it loads. */
    private double reloadPeriods = 1.0;
    /** The chances last worked out, and the draw they were worked out for. */
    private volatile Optional<ShellOdds> odds = Optional.empty();
    /** The kinds it can load, and the mechanics they were worked out for. */
    private volatile Optional<LoadableKinds> loadable = Optional.empty();
    /** What each kind does, and the stats and mechanics it was worked out for. */
    private volatile Optional<ShellProfiles> profiles = Optional.empty();
    private OptionalInt firedAt = OptionalInt.empty();
    private Optional<EnemyMob> target = Optional.empty();

    public MortarTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.MORTAR, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .with(TowerStat.AREA_RADIUS, CORE_CELLS).with(TowerStat.BLAST_EDGE, EDGE_CELLS)
                .with(TowerStat.PROJECTILE_SPEED, 1f).with(TowerStat.PROJECTILE_SIZE, 1f)
                .with(TowerStat.MINIMUM_RANGE, MINIMUM_RANGE_CELLS).with(TowerStat.HEAVY_DAMAGE, DamageUnits.ofPoints(HEAVY_POINTS))
                .with(TowerStat.NORMAL_DAMAGE, 0f).with(TowerStat.HEAVY_BLAST, 0f)
                .with(TowerStat.NORMAL_LOAD, 1f).with(TowerStat.CHAIN_LOAD, 1f).with(TowerStat.PAYLOAD_LOAD, 1f).with(TowerStat.HEAVY_LOAD, 1f)
                .with(TowerStat.NUKE_LOAD, 1f),
                context, x, y);
    }

    @Override
    protected OwnedMechanics<MortarMechanic> ownedMechanics() {
        return this.mechanics;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /** Whom it may shell and how its shells are drawn, fly and land, as the mechanics in {@code owned} make it. */
    private MortarSpec spec(List<MortarMechanic> owned) {
        Viewpoint view = this.viewpoint();
        MortarSpec spec = MortarSpec.from(view);
        for (MortarMechanic mechanic : owned) {
            spec = mechanic.refineSpec(spec);
        }
        return spec.withReach(spec.reach().withDeadZone(this.stats().value(TowerStat.MINIMUM_RANGE) * view.cellSize()));
    }

    private Optional<EnemyMob> findTarget(MortarSpec spec) {
        List<EnemyMob> inReach = spec.reach().matching(this.context.enemies());
        TargetSelector own = this.upgrades().targeting().map(priority -> priority.selector(PickContext.of(this.cellPixels())))
                .orElseGet(FurthestAlongPathSelector::new);
        return PreferringSelector.priority(own).selectFrom(inReach);
    }

    @Override
    protected void act(int gameTime) {
        this.blastMarks.removeIf(mark -> MomentMemory.forgets(mark.startedAtTick(), gameTime));
        MortarSpec spec = this.spec(this.mechanics.all());
        this.magazine = this.magazine.keptTo(spec.magazine(), this.context.random());
        this.loading = spec.volley().shells();
        this.fireVolleyLeft(spec);
        if (this.clock.isReady() && this.volleyLeft.isEmpty()) {
            this.findTarget(spec).ifPresent(target -> {
                this.target = Optional.of(target);
                this.load(target, spec);
                this.clock.spend(this.reloadPeriods);
            });
        }
        this.clock.advance(this.attackRate());
        this.turnTurret(this.turretAim, this.target);
    }

    @Override
    protected void endFight() {
        this.clock = new FireClock(COOLDOWN_MAX);
        this.reloadPeriods = 1.0;
        this.volleyLeft.clear();
        this.flares.clear();
        this.target = Optional.empty();
    }

    @Override
    protected void takeRestPose() {
        this.turretAim.rest();
    }

    /**
     * Takes the front shells off the magazine, as many as a firing loads, and fires the first at {@code target}; a
     * volley's others wait their turn, laid along the path behind it with Carpet Bombing, Normals sneaked between.
     */
    private void load(EnemyMob target, MortarSpec spec) {
        VolleySpec volley = spec.volley();
        List<HeldShell> held = this.magazine.held();
        List<HeldShell> front = held.subList(0, Math.min(volley.shells(), held.size()));
        this.magazine = this.magazine.fired(front.size(), spec.magazine(), this.context.random());
        this.reloadPeriods = volley.reloadFactor()
                * loadTime(front.getLast().kind(), this.magazine.front(volley.shells()), this.stats());
        List<HeldShell> loaded = volley.sequence(front).stream().map(shell -> asFired(shell.shell(), spec)).toList();
        List<Optional<Vec2>> laid = this.laidAlongThePath(target, volley, loaded.size());
        HeldShell first = loaded.getFirst();
        this.fireAt(first, laid.getFirst().orElseGet(() -> this.aimPoint(target, spec, first.kind())));
        for (int i = 1; i < loaded.size(); i++) {
            this.volleyLeft.add(new VolleyShell(loaded.get(i), laid.get(i), this.tickNow() + i * volley.gapTicks()));
        }
    }

    /** Where each shell of a volley lands with Carpet Bombing, centred on the target along the path; empty to aim. */
    private List<Optional<Vec2>> laidAlongThePath(EnemyMob target, VolleySpec volley, int shells) {
        List<Optional<Vec2>> laid = new ArrayList<>();
        if (!volley.alongPath()) {
            for (int i = 0; i < shells; i++) {
                laid.add(Optional.empty());
            }
            return laid;
        }
        double[] offsets = new double[shells];
        for (int i = 0; i < shells; i++) {
            offsets[i] = ((shells - 1) / 2.0 - i) * volley.spacingCells() * this.cellPixels();
        }
        List<Path> paths = this.context.level().paths().stream().map(PathRuntime::path).toList();
        List<Vec2> points = PathLine.along(paths, target.getX(), target.getY(), offsets);
        Vec2 standing = target.position();
        for (int i = 0; i < shells; i++) {
            laid.add(Optional.of(i < points.size() ? points.get(i) : standing));
        }
        return laid;
    }

    /** Fires the volley's shells whose turn has come: a Barrage's at its target, a new one if that one died. */
    private void fireVolleyLeft(MortarSpec spec) {
        while (!this.volleyLeft.isEmpty() && this.volleyLeft.getFirst().firesAt() <= this.tickNow()) {
            VolleyShell next = this.volleyLeft.removeFirst();
            next.laidAt().ifPresentOrElse(laid -> this.fireAt(next.shell(), laid), () -> {
                this.target = this.target.filter(enemy -> !enemy.isDead()).or(() -> this.findTarget(spec));
                this.target.ifPresent(enemy -> this.fireAt(next.shell(), this.aimPoint(enemy, spec, next.shell().kind())));
            });
        }
    }

    /** The frame reads the kinds every frame and they change only with the mechanics, so they are kept until then. */
    private record LoadableKinds(List<MortarMechanic> owned, List<ShellKind> kinds) {
    }

    private record ShellOdds(MagazineSpec spec, Map<ShellKind, Float> chances, float chained) {
    }

    private record ShellProfiles(TowerStats stats, List<MortarMechanic> owned, List<ShellProfile> profiles) {
    }

    /** A shell of a volley still to fire: the shell, where it is laid with Carpet Bombing, and the tick it fires. */
    private record VolleyShell(HeldShell shell, Optional<Vec2> laidAt, int firesAt) {
    }

    /**
     * How long loading {@code next} takes after {@code fired}, as a share of the reload: the mean of each shell's load
     * time, a light one's chained when the shell in front of it is light too.
     */
    private static double loadTime(ShellKind fired, List<ShellKind> next, TowerStats stats) {
        double total = 0.0;
        ShellKind before = fired;
        for (ShellKind kind : next) {
            total += loadTimeOf(before, kind, stats);
            before = kind;
        }
        return next.isEmpty() ? 1.0 : total / next.size();
    }

    private static double loadTimeOf(ShellKind before, ShellKind kind, TowerStats stats) {
        float own = stats.value(kind.loadStat());
        return before.isLight() && kind.isLight() ? own * stats.value(TowerStat.CHAIN_LOAD) : own;
    }

    /**
     * The load time of a shell it fires, over a long run of draws: each kind's by its share of the shells, and a
     * chained light one's by the share of chained shells.
     */
    private static double averageLoadTime(ShellOdds odds, TowerStats stats) {
        double unchained = odds.chances().entrySet().stream()
                .mapToDouble(entry -> entry.getValue() * stats.value(entry.getKey().loadStat())).sum();
        return unchained + odds.chained() * stats.value(TowerStat.NORMAL_LOAD) * (stats.value(TowerStat.CHAIN_LOAD) - 1f);
    }

    /** {@code shell} as this Mortar fires it: a Normal carries the payload in front of it only with Thick Mix. */
    private static HeldShell asFired(HeldShell shell, MortarSpec spec) {
        return spec.payload().normalsCarry() > 0f ? shell : shell.plain();
    }

    /** Fires {@code held} at {@code aim}; a Normal that carries a payload shows it. */
    private void fireAt(HeldShell held, Vec2 aim) {
        Shell shell = new Shell(held.kind(), held.carried(), this.beginShot());
        this.firedAt = OptionalInt.of(this.tickNow());
        this.context.projectiles().add(new CannonballProjectile(this.centerX, this.centerY, aim.x(), aim.y(),
                this.shellStats(shell), held.kind().look(), held.carried().map(ShellKind::look),
                (x, y, gameTime) -> this.onImpact(x, y, shell, gameTime)));
    }

    /**
     * What was fired: the kind of shell, the payload it carries, and the stats it lands with however the Mortar changes
     * meanwhile.
     */
    private record Shell(ShellKind kind, Optional<ShellKind> carried, TowerStats stats) {

        /** How it goes off: a Normal that carries a payload goes off as that payload, at its own blast. */
        ShellLook blastLook() {
            return this.carried.map(ShellKind::look).orElse(this.kind.look());
        }
    }

    /**
     * Where a shell of {@code kind} is sent: the enemy where it stands, or with Predictive Fire where it will be when
     * that shell lands.
     */
    private Vec2 aimPoint(EnemyMob target, MortarSpec spec, ShellKind kind) {
        Vec2 aim = target.position();
        if (!spec.leadsTarget()) {
            return aim;
        }
        float speed = SHELL_SPEED * this.stats().value(TowerStat.PROJECTILE_SPEED) * kind.speedFactor();
        for (int i = 0; i < PREDICTION_PASSES; i++) {
            int flightTicks = (int) Math.ceil(Math.hypot(aim.x() - this.centerX, aim.y() - this.centerY) / speed);
            aim = target.positionAfter(flightTicks);
        }
        return aim;
    }

    /** The shell as it flies: its speed, its kind's share of it, and its size are stats. */
    private ProjectileStats shellStats(Shell shell) {
        TowerStats shot = shell.stats();
        float speedScale = shot.value(TowerStat.PROJECTILE_SPEED);
        return ProjectileStats.of(SHELL_SPEED * speedScale * shell.kind().speedFactor())
                .withSize(shot.value(TowerStat.PROJECTILE_SIZE))
                .withStreak(speedScale > 1f);
    }

    /** One shell's blast, in pixels: its full-damage core, its edge, and what the edge deals. */
    private record BlastRadii(float core, float edge, float edgeShare) {
    }

    private BlastRadii blastOf(Shell shell, MortarSpec spec) {
        TowerStats stats = shell.stats();
        ShellKind kind = shell.kind();
        float extra = kind.wideningCells(stats.value(TowerStat.HEAVY_BLAST), spec.payload().extraCells());
        float core = (stats.value(TowerStat.AREA_RADIUS) + kind.blast().extraCoreCells() + extra) * this.cellPixels();
        float edge = (stats.value(TowerStat.BLAST_EDGE) + kind.blast().extraEdgeCells() + extra) * this.cellPixels();
        return new BlastRadii(core, Math.max(core, edge), kind.blast().edgeShare());
    }

    /**
     * A shell lands with the stats it was fired with: everything within its edge takes a hit, the core at full damage,
     * and carries Crack, and a Heavy's pick; it leaves its kind's ground.
     */
    private void onImpact(double x, double y, Shell shell, int gameTime) {
        MortarSpec spec = this.spec(this.mechanics.all());
        ShellKind kind = shell.kind();
        TowerStats stats = shell.stats();
        BlastRadii blast = this.blastOf(shell, spec);
        int damage = kind.damage(stats.damage(), stats.value(TowerStat.NORMAL_DAMAGE),
                stats.value(TowerStat.HEAVY_DAMAGE));
        float core2 = blast.core() * blast.core();
        for (EnemyMob enemy : InRangeTargetQuery.everyone((int) Math.round(x), (int) Math.round(y), blast.edge())
                .matching(this.context.enemies())) {
            double dx = enemy.getX() - x;
            double dy = enemy.getY() - y;
            boolean inCore = dx * dx + dy * dy <= core2;
            HitAdjustment adjustment = this.hitAdjustment(enemy, HitKind.BLAST, 0f, stats);
            int amount = Math.round(damage * (inCore ? 1f : blast.edgeShare()) * adjustment.damageFactor());
            HitResult hit = this.landHit(enemy, Damage.of(kind.damageType(), amount), adjustment.applyTo(stats.attack()));
            if (hit.dodged()) {
                continue;
            }
            this.report(enemy, HitKind.BLAST, hit.critical(), enemy.isDead());
            this.crack(enemy, spec);
            this.carryPayload(enemy, shell, spec);
            if (kind == ShellKind.HEAVY) {
                this.rideHeavy(enemy, spec.heavy(), inCore, stats, hit.credited());
            }
        }
        if (kind.irradiatesTheBoard()) {
            this.irradiateTheBoard();
        }
        this.blastMarks.add(BlastMark.of(new Vec2(x, y), blast.core(), blast.edge(), gameTime, shell.blastLook())
                .withRider(kind == ShellKind.HEAVY ? spec.heavy() : HeavyRider.NONE));
        this.leaveGround(x, y, shell, blast, spec);
    }

    /** A Nuke's flash: every enemy on the board, hidden or not, takes a dose of Radiation. */
    private void irradiateTheBoard() {
        for (EnemyMob enemy : this.context.enemies().getEnemies()) {
            if (enemy.validTarget()) {
                this.applyEffect(enemy, sink -> Effect.irradiated(ShellKind.RADIATION_POINTS_PER_SECOND,
                        ShellKind.RADIATION_HALF_LIFE_TICKS, sink));
            }
        }
    }

    /** A Normal that carries a payload puts a share of a pulse of that payload's ground on what its blast reaches. */
    private void carryPayload(EnemyMob enemy, Shell shell, MortarSpec spec) {
        shell.carried().flatMap(ShellKind::ground).ifPresent(ground -> ZoneEffects.touch(ground.kind(),
                carriedShare(ground, spec), this.zoneOwner, enemy));
    }

    private static ZonePulse carriedShare(ShellKind.Ground ground, MortarSpec spec) {
        return ground.pulse().shareOf(spec.payload().normalsCarry());
    }

    private void crack(EnemyMob enemy, MortarSpec spec) {
        if (spec.crackStacks() == 0) {
            return;
        }
        this.applyEffect(enemy, sink -> Effect.cracked(spec.crackStacks(), CRACKED_TICKS, sink));
    }

    /**
     * What a Heavy's pick puts on an enemy its blast reached: a Daze by zone, or a Bleed from {@code dealt}, what the
     * blast did to it after its defences.
     */
    private void rideHeavy(EnemyMob enemy, HeavyRider rider, boolean inCore, TowerStats stats, int dealt) {
        switch (rider) {
            case NONE -> {
            }
            case CONCUSSION -> {
                int ticks = inCore ? ConcussionMechanic.CORE_DAZE_TICKS : ConcussionMechanic.EDGE_DAZE_TICKS;
                this.applyEffect(enemy, sink -> Effect.dazed(ticks, sink));
            }
            case SHRAPNEL -> {
                AttackProfile bleed = stats.attack().pastArmorAndPlating().asPeriodic();
                this.applyEffect(enemy, _ -> Effect.bleeding(ShrapnelMechanic.bleedPerCell(dealt), ShrapnelMechanic.BLEED_TICKS,
                        bled -> this.dealDamage(enemy, bled, bleed)));
            }
        }
    }

    /** The ground a shell's kind leaves where it lands; only the newest flares keep burning. */
    private void leaveGround(double x, double y, Shell shell, BlastRadii blast, MortarSpec spec) {
        ShellKind kind = shell.kind();
        kind.ground().ifPresent(ground -> {
            Zone zone = new Zone(ground.kind(), x, y, this.groundRadius(kind, ground, blast, spec),
                    this.groundLifetime(kind, ground, spec), ground.pulse(), this.zoneOwner);
            this.context.zones().add(zone);
            if (kind == ShellKind.FLARE) {
                this.keepNewestFlares(zone);
            }
        });
    }

    /** How wide, in pixels, the ground a {@code kind} leaves is: a payload's widened by its Payload spec. */
    private float groundRadius(ShellKind kind, ShellKind.Ground ground, BlastRadii blast, MortarSpec spec) {
        return ground.blastShare() > 0f ? ground.blastShare() * blast.edge()
                : (ground.radiusCells() + (kind.isPayload() ? spec.payload().extraCells() : 0f)) * this.cellPixels();
    }

    /** How long the ground a {@code kind} leaves lasts: a payload's lengthened by its Payload spec. */
    private int groundLifetime(ShellKind kind, ShellKind.Ground ground, MortarSpec spec) {
        return ground.lastsTheWave() ? Zone.UNTIL_WAVE_END
                : this.lengthened(ground.lifetimeTicks() + (kind.isPayload() ? spec.payload().extraTicks() : 0));
    }

    /** {@code pulse} as an enemy gets it from this Mortar: its slow and freeze last as long as its effects do. */
    private ZonePulse lengthenedPulse(ZonePulse pulse) {
        ZonePulse.Slow slow = pulse.slow();
        ZonePulse.Freeze freeze = pulse.freeze();
        return pulse.withSlow(new ZonePulse.Slow(slow.strength(), this.lengthened(slow.holdTicks())))
                .withFreeze(new ZonePulse.Freeze(freeze.pulses(), this.lengthened(freeze.ticks())));
    }

    private void keepNewestFlares(Zone flare) {
        this.flares.removeIf(old -> !old.isBurning());
        this.flares.add(flare);
        while (this.flares.size() > FlareMechanic.FLARES_KEPT) {
            this.flares.removeFirst().putOut();
        }
    }

    /** The shells it has ready, front first. Any thread. */
    public Magazine magazine() {
        return this.magazine;
    }

    /** How many front shells one firing loads: one, or a volley's. Any thread. */
    public int loadingCount() {
        return this.loading;
    }

    /** Every kind of shell it can load, in the kinds' order: each kind it draws, and the Nuke while Heavies fuse. */
    public List<ShellKind> loadableKinds() {
        List<MortarMechanic> owned = this.mechanics.all();
        return this.loadable.filter(last -> last.owned().equals(owned)).orElseGet(() -> {
            MagazineSpec spec = this.spec(owned).magazine();
            LoadableKinds fresh = new LoadableKinds(owned, Arrays.stream(ShellKind.values()).filter(spec::owns).toList());
            this.loadable = Optional.of(fresh);
            return fresh;
        }).kinds();
    }

    /** What share of the shells it fires are each kind, over a long run of draws; worked out again only as it changes. */
    public Map<ShellKind, Float> shellChances() {
        return this.shellOdds().chances();
    }

    private ShellOdds shellOdds() {
        MagazineSpec spec = this.spec(this.mechanics.all()).magazine();
        return this.odds.filter(last -> last.spec().equals(spec)).orElseGet(() -> {
            ShellOdds fresh = new ShellOdds(spec, spec.chances(), spec.chainedShare());
            this.odds = Optional.of(fresh);
            return fresh;
        });
    }

    /**
     * What {@code node} says, and, for one it doesn't own, how the share of each kind it fires would change: a node
     * taking a cluster's place leaves out what the member it replaces adds.
     */
    @Override
    public NodeWords wordsOf(UpgradeNode node) {
        NodeWords words = super.wordsOf(node);
        List<UpgradeNode> owned = this.upgrades().owned();
        if (owned.stream().anyMatch(it -> it.id().equals(node.id()))) {
            return words;
        }
        Optional<UpgradeCluster> cluster = this.upgradeTree().clusterOf(node);
        List<MortarMechanic> after = new ArrayList<>();
        for (UpgradeNode kept : owned) {
            if (cluster.filter(replaced -> replaced.contains(kept.id())).isEmpty()) {
                after.addAll(MECHANICS.mechanicsOf(kept));
            }
        }
        after.addAll(MECHANICS.mechanicsOf(node));
        return chanceChange(this.shellChances(), this.spec(after).magazine().chances()).map(change -> {
            List<TermText> sentences = new ArrayList<>(words.sentences());
            sentences.add(change);
            return NodeWords.of(words.name(), words.stats(), sentences).withChosenOver(words.chosenOver());
        }).orElse(words);
    }

    /** "Heavy 18% → 31%, Normal 82% → 69%" for every kind whose shown share moves; empty when none does. */
    static Optional<TermText> chanceChange(Map<ShellKind, Float> before, Map<ShellKind, Float> after) {
        TermText text = TermText.empty();
        for (ShellKind kind : ShellKind.values()) {
            String was = TermNumbers.percent(before.getOrDefault(kind, 0f));
            String will = TermNumbers.percent(after.getOrDefault(kind, 0f));
            if (was.equals(will)) {
                continue;
            }
            if (!text.isEmpty()) {
                text = text.plus(", ");
            }
            text = text.plus(kind.mention()).plus(" " + was + " → " + will);
        }
        return Optional.of(text).filter(said -> !said.isEmpty());
    }

    /** Its magazine as the pane shows it. */
    public MagazineView magazineView() {
        MortarSpec spec = this.spec(this.mechanics.all());
        List<HeldShell> held = this.magazine.held();
        int loads = Math.min(spec.volley().shells(), held.size());
        List<LinedUpShell> next = spec.volley().sequence(held.subList(0, loads));
        List<LinedUpShell> lined = new ArrayList<>(next);
        held.subList(loads, held.size()).forEach(shell -> lined.add(LinedUpShell.held(shell)));
        return new MagazineView(lined.stream().map(shell -> shell.withShell(asFired(shell.shell(), spec))).toList(),
                next.size(), this.shellChances(), this.shellProfiles());
    }

    /** What each kind it can load does as it would fire it now; worked out again only as its stats or nodes change. */
    public List<ShellProfile> shellProfiles() {
        List<MortarMechanic> owned = this.mechanics.all();
        TowerStats stats = this.stats();
        return this.profiles.filter(last -> last.stats() == stats && last.owned().equals(owned)).orElseGet(() -> {
            MortarSpec spec = this.spec(owned);
            ShellProfiles fresh = new ShellProfiles(stats, owned,
                    this.profiledShells(spec).stream().map(shell -> this.profileOf(shell, stats, spec)).toList());
            this.profiles = Optional.of(fresh);
            return fresh;
        }).profiles();
    }

    /** Every kind it can load, and with Thick Mix a Normal carrying each payload it can load. */
    private List<HeldShell> profiledShells(MortarSpec spec) {
        List<ShellKind> kinds = this.loadableKinds();
        List<HeldShell> shells = new ArrayList<>(kinds.stream().map(kind -> new HeldShell(kind, Optional.empty())).toList());
        kinds.stream().filter(ShellKind::isPayload).map(payload -> HeldShell.behind(ShellKind.NORMAL, Optional.of(payload)))
                .map(shell -> asFired(shell, spec)).filter(shell -> shell.carried().isPresent()).forEach(shells::add);
        return List.copyOf(shells);
    }

    private ShellProfile profileOf(HeldShell held, TowerStats stats, MortarSpec spec) {
        ShellKind kind = held.kind();
        Shell shell = new Shell(kind, held.carried(), stats);
        BlastRadii radii = this.blastOf(shell, spec);
        float cell = this.cellPixels();
        int damage = kind.damage(stats.damage(), stats.value(TowerStat.NORMAL_DAMAGE),
                stats.value(TowerStat.HEAVY_DAMAGE));
        Set<EffectKind> applies = EnumSet.noneOf(EffectKind.class);
        if (spec.crackStacks() > 0) {
            applies.add(EffectKind.CRACKED);
        }
        applies.addAll(kind.applies());
        held.carried().flatMap(ShellKind::ground).ifPresent(ground -> {
            ground.kind().fuels().ifPresent(applies::add);
            ground.kind().slows().ifPresent(applies::add);
        });
        if (kind == ShellKind.HEAVY) {
            spec.heavy().applies().ifPresent(applies::add);
        }
        ShellProfile profile = ShellProfile.of(kind, Damage.of(kind.damageType(), damage),
                this.shellStats(shell).speed() * TICKS_PER_SECOND / cell,
                new ShellProfile.Blast(radii.core() / cell, radii.edge() / cell, radii.edgeShare(), List.copyOf(applies)));
        ShellProfile carrying = held.carried().flatMap(payload -> payload.ground().map(ground -> profile.withCarry(
                new ShellProfile.Carry(payload, ground.kind(), this.lengthenedPulse(carriedShare(ground, spec))))))
                .orElse(profile);
        return kind.ground().map(ground -> carrying.withGround(new ShellProfile.Ground(ground.kind(),
                        this.groundRadius(kind, ground, radii, spec) / cell, this.groundLifetime(kind, ground, spec),
                        this.lengthenedPulse(ground.pulse()))))
                .orElse(carrying);
    }

    /** {@code tower}'s magazine as the pane shows it, when it is a Mortar. */
    public static Optional<MagazineView> magazineViewOf(Tower tower) {
        return tower.accept(AS_MORTAR).map(MortarTower::magazineView);
    }

    /** {@code tower}'s magazine, when it is a Mortar. */
    public static Optional<Magazine> magazineOf(Tower tower) {
        return tower.accept(AS_MORTAR).map(MortarTower::magazine);
    }

    /**
     * Loads {@code magazine} into {@code tower}, a Mortar, as a save kept it: only while no tick runs, as a level is
     * rebuilt. A shell it no longer owns leaves at its next tick.
     *
     * @return whether {@code tower} is a Mortar
     */
    public static boolean loadMagazine(Tower tower, Magazine magazine) {
        Optional<MortarTower> mortar = tower.accept(AS_MORTAR);
        mortar.ifPresent(it -> it.magazine = magazine);
        return mortar.isPresent();
    }

    /**
     * Draws {@code tower}'s magazine full from what it owns, when it is a Mortar, so that a fight starts with the shells
     * its build draws rather than the ones it held before its nodes: a bench fight stands for a wave long after them.
     * Only while no tick runs.
     */
    public static void drawMagazineAfresh(Tower tower) {
        tower.accept(AS_MORTAR).ifPresent(it -> it.magazine = Magazine.drawn(it.spec(it.mechanics.all()).magazine(),
                it.context.random()));
    }

    /** How much of the reload under way is still to run, however long: 1 as a shell leaves, 0 once the next is ready. */
    public float getCoolDownFraction() {
        return this.clock.remaining(this.reloadPeriods);
    }

    /** The ticks since its last shell left, a volley's included, while they are within {@code window}. */
    public OptionalInt ticksSinceFired(int gameTime, int window) {
        return within(this.firedAt, gameTime, window);
    }

    /** How many ticks the reload under way takes at its rate now: the cooldown fraction falls by one over that many. */
    public double reloadTicks() {
        return this.clock.period() * this.reloadPeriods / Math.max(MINIMUM_RATE, this.attackRate());
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    /** Blasts that went off in the last few ticks, for the detonation the board draws. */
    public List<BlastMark> getBlastMarks() {
        return List.copyOf(this.blastMarks);
    }

    /**
     * Shells a second over a long run: a volley loads several shells for a longer reload, and each kind takes its own
     * time to load, so the rate is over the whole cycle and every kind by its share of the shells.
     */
    @Override
    protected TowerStatLine cadence(TowerStats stats, Function<TowerStat, List<StatPart>> parts) {
        VolleySpec volley = this.spec(this.mechanics.all()).volley();
        double loadTime = averageLoadTime(this.shellOdds(), stats);
        return this.derivedRow(TowerStatRow.FIRE_RATE, TowerStat.ATTACK_SPEED, stats, parts,
                at -> (float) (volley.fired() * TICKS_PER_SECOND * at.attackRate()
                        / ((this.coolDownMax + 1) * volley.reloadFactor() * loadTime)));
    }

    @Override
    protected List<TowerStatLine> ownStats(TowerStats stats) {
        ProjectileStats shell = this.shellStats(new Shell(ShellKind.NORMAL, Optional.empty(), stats));
        float cellsPerSecond = shell.speed() * TICKS_PER_SECOND / this.cellPixels();
        return List.of(TowerStatLine.fixed(TowerStatRow.PROJECTILE_SPEED, TowerStat.PROJECTILE_SPEED, cellsPerSecond),
                TowerStatLine.fixed(TowerStatRow.PROJECTILE_SIZE, TowerStat.PROJECTILE_SIZE, shell.size()));
    }

    /** The shell loading, when it is any kind but a Normal. */
    @Override
    protected List<TowerBuffState> ownConditions() {
        return this.magazine.front().term().map(term -> List.of(TowerBuffState.of(term, term.describe())))
                .orElse(List.of());
    }

    @Override
    protected List<AppliedEffect> baseApplies() {
        int stacks = this.spec(this.mechanics.all()).crackStacks();
        if (stacks == 0) {
            return List.of();
        }
        return List.of(new AppliedEffect(EffectKind.CRACKED, List.of(new AppliedEffect.Source(this.displayName(),
                TermText.of("every shell gives a share of " + (stacks == 1 ? "a stack" : stacks + " stacks") + " of ")
                        .plus(EffectKind.CRACKED)
                        .plus(" for " + TermNumbers.seconds(CRACKED_TICKS / TickRate.TICKS_PER_SECOND))))));
    }

    @Override
    protected String targetingLabel() {
        return this.upgrades().targeting().map(TargetPriority::label).orElse("first");
    }

    @Override
    protected String description() {
        return "Lobs a slow, unguided shell from its magazine, which draws each new shell by lot.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitMortarTower(this);
    }
}
