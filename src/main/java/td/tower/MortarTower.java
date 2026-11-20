package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.projectile.CannonballProjectile;
import td.projectile.ProjectileStats;
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
import td.tower.mortar.LastingMixMechanic;
import td.tower.mortar.Magazine;
import td.tower.mortar.MagazineSpec;
import td.tower.mortar.MagazineView;
import td.tower.mortar.MortarMechanic;
import td.tower.mortar.MortarSpec;
import td.tower.mortar.NukeFlash;
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
    /** Ticks a nuke's flash and ring last. */
    public static final int NUKE_FLASH_TICKS = 10;
    /** Ticks the mark of any blast stays on the board. */
    public static final int BLAST_MARK_TICKS = 6;

    private static final float MINIMUM_RANGE_CELLS = 1.5f;
    private static final float HEAVY_TIMES = 1.75f;
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
    private static final UpgradeNode BIG_BORE = UpgradeNode.of("mortar.big_bore", "Big Bore", 5)
            .withModifiers(TowerModifiers.heavyMultiplier(HEAVY_TIMES * 0.25f));
    private static final UpgradeNode CRACKING_SHELLS = UpgradeNode.of("mortar.cracking_shells", "Cracking Shells", 5);
    private static final UpgradeNode PREDICTIVE_FIRE = UpgradeNode.of("mortar.predictive_fire", "Predictive Fire", 5)
            .withTargeting(TargetPriority.MOST_NEIGHBOURS);

    private static final UpgradeNode CONCUSSION = UpgradeNode.of("mortar.concussion", "Concussion", 10)
            .inTier(1);
    private static final UpgradeNode SHRAPNEL = UpgradeNode.of("mortar.shrapnel", "Shrapnel", 10)
            .inTier(1);
    private static final UpgradeNode NAPALM = UpgradeNode.of("mortar.napalm", "Napalm", 15)
            .inTier(1);
    private static final UpgradeNode TAR = UpgradeNode.of("mortar.tar", "Tar", 15)
            .inTier(1);
    private static final UpgradeNode CRYO = UpgradeNode.of("mortar.cryo", "Cryo", 15)
            .inTier(1);
    private static final UpgradeNode FLARE = UpgradeNode.of("mortar.flare", "Flare", 15)
            .inTier(1);
    private static final UpgradeCondition A_PAYLOAD = UpgradeCondition.owns(NAPALM.id())
            .or(UpgradeCondition.owns(TAR.id())).or(UpgradeCondition.owns(CRYO.id()));

    private static final UpgradeNode BARRAGE = UpgradeNode.of("mortar.barrage", "Barrage", 31)
            .inTier(3);
    private static final UpgradeNode CARPET_BOMBING = UpgradeNode.of("mortar.carpet_bombing", "Carpet Bombing", 31)
            .inTier(3);
    private static final UpgradeNode ELEVATION = UpgradeNode.of(RangeNodes.RANGE_ID, "Elevation", 20)
            .withModifiers(TowerModifiers.range(1.6f))
            .inTier(2);
    private static final UpgradeNode OVERCHARGE = UpgradeNode.of("mortar.overcharge", "Overcharge", 25)
            .withModifiers(TowerModifiers.heavyMultiplier(2f))
            .inTier(2);
    private static final UpgradeNode BIG_BORE_2 = UpgradeNode.of("mortar.big_bore_2", "Big Bore II", 20)
            .withModifiers(TowerModifiers.heavyMultiplier(HEAVY_TIMES * 0.75f))
            .inTier(2);
    private static final UpgradeNode THICK_MIX = UpgradeNode.of("mortar.thick_mix", "Thick Mix", 20)
            .withRequires(A_PAYLOAD)
            .inTier(2);

    private static final UpgradeNode FUSION = UpgradeNode.of("mortar.fusion", "Fusion", 8)
            .inTier(3);
    private static final UpgradeNode RICH_MIX = UpgradeNode.of("mortar.rich_mix", "Rich Mix", 8)
            .inTier(3);
    private static final UpgradeNode LASTING_MIX = UpgradeNode.of("mortar.lasting_mix", "Lasting Mix", 8)
            .after(THICK_MIX)
            .inTier(3);
    private static final UpgradeNode DEEP_CRACK = UpgradeNode.of("mortar.deep_crack", "Deep Crack", 15)
            .after(CRACKING_SHELLS)
            .inTier(2);
    private static final UpgradeNode QUICK_RELOAD = UpgradeNode.of("mortar.quick_reload", "Quick Reload", 39)
            .withModifiers(TowerModifiers.attackSpeed(3.5f / 3f - 1f))
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(HEAVY_CHARGE, BIG_BORE, PREDICTIVE_FIRE, CRACKING_SHELLS))
            .with(CONCUSSION, SHRAPNEL, NAPALM, TAR, CRYO, FLARE,
                    BARRAGE, CARPET_BOMBING, ELEVATION, OVERCHARGE, BIG_BORE_2, THICK_MIX,
                    FUSION, RICH_MIX, LASTING_MIX, DEEP_CRACK, QUICK_RELOAD)
            .withCluster(UpgradeCluster.of("Heavy shell", CONCUSSION, SHRAPNEL))
            .withCluster(UpgradeCluster.of("Payload", NAPALM, TAR, CRYO))
            .withCluster(UpgradeCluster.of("Volley", BARRAGE, CARPET_BOMBING))
            .withCluster(UpgradeCluster.of("Order", FUSION, RICH_MIX));

    private static final MechanicCatalogue<MortarMechanic> MECHANICS = MechanicCatalogue.of(
            entry(BIG_BORE.id(), () -> new BigBoreMechanic(1)),
            entry(PREDICTIVE_FIRE.id(), PredictiveFireMechanic::new),
            entry(CRACKING_SHELLS.id(), CrackingShellsMechanic::new),
            entry(CONCUSSION.id(), ConcussionMechanic::new),
            entry(SHRAPNEL.id(), ShrapnelMechanic::new),
            entry(NAPALM.id(), () -> new PayloadMechanic(ShellKind.NAPALM)),
            entry(TAR.id(), () -> new PayloadMechanic(ShellKind.TAR)),
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
    private final ZoneOwner zoneOwner = this::applyEffect;
    private final List<BlastMark> blastMarks = new ArrayList<>();
    private final List<Zone> flares = new ArrayList<>();
    private final List<VolleyShell> volleyLeft = new ArrayList<>();
    /** Build state, not fight state: a wave starts from the magazine the last one left, and the save keeps it. */
    private volatile Magazine magazine = Magazine.opener();
    private volatile int loading = 1;
    private FireClock clock = new FireClock(COOLDOWN_MAX);
    private volatile Optional<NukeFlash> nukeFlash = Optional.empty();
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
                .with(TowerStat.MINIMUM_RANGE, MINIMUM_RANGE_CELLS).with(TowerStat.HEAVY_MULTIPLIER, HEAVY_TIMES),
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
        TargetSelector own = this.upgrades().targeting().map(priority -> priority.selector(this.cellPixels()))
                .orElseGet(FurthestAlongPathSelector::new);
        return PreferringSelector.priority(own).selectFrom(inReach);
    }

    @Override
    protected void act(int gameTime) {
        this.blastMarks.removeIf(mark -> gameTime - mark.startedAtTick() >= BLAST_MARK_TICKS);
        MortarSpec spec = this.spec(this.mechanics.all());
        this.magazine = this.magazine.keptTo(spec.magazine(), this.context.random());
        this.loading = spec.volley().shells();
        this.fireVolleyLeft(spec);
        if (this.clock.isReady() && this.volleyLeft.isEmpty()) {
            this.findTarget(spec).ifPresent(target -> {
                this.target = Optional.of(target);
                this.load(target, spec);
                this.clock.spend(spec.volley().reloadFactor());
            });
        }
        this.clock.advance(this.attackRate());
        this.turnTurret(this.turretAim, this.target);
    }

    @Override
    protected void endFight() {
        this.clock = new FireClock(COOLDOWN_MAX);
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
        List<ShellKind> front = this.magazine.front(volley.shells());
        this.magazine = this.magazine.fired(front.size(), spec.magazine(), this.context.random());
        List<ShellKind> loaded = volley.sequence(front);
        List<Optional<Vec2>> laid = this.laidAlongThePath(target, volley, loaded.size());
        ShellKind first = loaded.getFirst();
        this.fireAt(first, laid.getFirst().orElseGet(() -> this.aimPoint(target, spec, first)));
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
            next.laidAt().ifPresentOrElse(laid -> this.fireAt(next.kind(), laid), () -> {
                this.target = this.target.filter(enemy -> !enemy.isDead()).or(() -> this.findTarget(spec));
                this.target.ifPresent(enemy -> this.fireAt(next.kind(), this.aimPoint(enemy, spec, next.kind())));
            });
        }
    }

    /** The frame reads the kinds every frame and they change only with the mechanics, so they are kept until then. */
    private record LoadableKinds(List<MortarMechanic> owned, List<ShellKind> kinds) {
    }

    private record ShellOdds(MagazineSpec spec, Map<ShellKind, Float> chances) {
    }

    private record ShellProfiles(TowerStats stats, List<MortarMechanic> owned, List<ShellProfile> profiles) {
    }

    /** A shell of a volley still to fire: its kind, where it is laid with Carpet Bombing, and the tick it fires. */
    private record VolleyShell(ShellKind kind, Optional<Vec2> laidAt, int firesAt) {
    }

    private void fireAt(ShellKind kind, Vec2 aim) {
        Shell shell = new Shell(kind, this.beginShot());
        this.firedAt = OptionalInt.of(this.tickNow());
        this.context.projectiles().add(new CannonballProjectile(this.centerX, this.centerY, aim.x(), aim.y(),
                this.shellStats(shell), kind.look(), (x, y, gameTime) -> this.onImpact(x, y, shell, gameTime)));
    }

    /** What was fired: the kind of shell, and the stats it lands with however the Mortar changes meanwhile. */
    private record Shell(ShellKind kind, TowerStats stats) {
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
        float extra = kind.isPayload() ? spec.payload().extraCells() : 0f;
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
        float damage = stats.damage() * kind.damageFactor(stats.value(TowerStat.HEAVY_MULTIPLIER));
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
            if (kind == ShellKind.NORMAL) {
                this.carryPayload(enemy, spec, stats);
            }
            if (kind == ShellKind.HEAVY) {
                this.rideHeavy(enemy, spec.heavy(), inCore, Math.round(damage * ShrapnelMechanic.SHARE_PER_CELL), stats);
            }
        }
        if (kind == ShellKind.NUKE) {
            this.nukeFlash = Optional.of(new NukeFlash(new Vec2(x, y), blast.edge(), gameTime));
        }
        boolean fragments = kind == ShellKind.HEAVY && spec.heavy() == HeavyRider.SHRAPNEL;
        this.blastMarks.add(BlastMark.of(new Vec2(x, y), blast.core(), blast.edge(), gameTime, kind.look())
                .withFragments(fragments));
        this.leaveGround(x, y, shell, blast, spec);
    }

    /** With Thick Mix, a Normal's blast puts a share of a pulse of the payload's ground on what it reaches. */
    private void carryPayload(EnemyMob enemy, MortarSpec spec, TowerStats stats) {
        float share = spec.payload().normalsCarry();
        carriedPayload(spec).flatMap(ShellKind::ground)
                .ifPresent(ground -> ZoneEffects.touch(ground.kind(), Math.round(groundStrength(stats, ground) * share),
                        this.zoneOwner, enemy));
    }

    /** The payload a Normal shell carries with Thick Mix: the one the Mortar loads, if it carries any. */
    private static Optional<ShellKind> carriedPayload(MortarSpec spec) {
        if (spec.payload().normalsCarry() <= 0f) {
            return Optional.empty();
        }
        return Arrays.stream(ShellKind.values()).filter(ShellKind::isPayload).filter(spec.magazine()::owns).findFirst();
    }

    private void crack(EnemyMob enemy, MortarSpec spec) {
        if (spec.crackStacks() == 0) {
            return;
        }
        this.applyEffect(enemy, sink -> Effect.cracked(spec.crackStacks(), CRACKED_TICKS, sink));
    }

    /** What a Heavy's pick puts on an enemy its blast reached: a Daze by zone, or a Bleed by the shell's damage. */
    private void rideHeavy(EnemyMob enemy, HeavyRider rider, boolean inCore, int bleedPerCell, TowerStats stats) {
        switch (rider) {
            case NONE -> {
            }
            case CONCUSSION -> {
                int ticks = inCore ? ConcussionMechanic.CORE_DAZE_TICKS : ConcussionMechanic.EDGE_DAZE_TICKS;
                this.applyEffect(enemy, sink -> Effect.dazed(ticks, sink));
            }
            case SHRAPNEL -> {
                AttackProfile bleed = stats.attack().pastArmorAndPlating().asPeriodic();
                this.applyEffect(enemy, _ -> Effect.bleeding(bleedPerCell, ShrapnelMechanic.BLEED_TICKS,
                        bled -> this.dealDamage(enemy, bled, bleed)));
            }
        }
    }

    /** The ground a shell's kind leaves where it lands; only the newest flares keep burning. */
    private void leaveGround(double x, double y, Shell shell, BlastRadii blast, MortarSpec spec) {
        ShellKind kind = shell.kind();
        kind.ground().ifPresent(ground -> {
            boolean payload = kind.isPayload();
            float radius = ground.blastShare() > 0f ? ground.blastShare() * blast.edge()
                    : (ground.radiusCells() + (payload ? spec.payload().extraCells() : 0f)) * this.cellPixels();
            int lifetime = ground.lastsTheWave() ? Zone.UNTIL_WAVE_END
                    : this.lengthened(ground.lifetimeTicks() + (payload ? spec.payload().extraTicks() : 0));
            Zone zone = new Zone(ground.kind(), x, y, radius, lifetime, groundStrength(shell.stats(), ground),
                    this.zoneOwner);
            this.context.zones().add(zone);
            if (kind == ShellKind.FLARE) {
                this.keepNewestFlares(zone);
            }
        });
    }

    /** What each pulse of {@code ground} adds to a burn or a poison, for a shell fired with {@code stats}. */
    private static int groundStrength(TowerStats stats, ShellKind.Ground ground) {
        return Math.round(stats.damage() * ground.damageShare());
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
        MagazineSpec spec = this.spec(this.mechanics.all()).magazine();
        return this.odds.filter(last -> last.spec().equals(spec)).orElseGet(() -> {
            ShellOdds fresh = new ShellOdds(spec, spec.chances());
            this.odds = Optional.of(fresh);
            return fresh;
        }).chances();
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
        return new MagazineView(this.magazine.shells(), this.loading, this.shellChances(), this.shellProfiles());
    }

    /** What each kind it can load does as it would fire it now; worked out again only as its stats or nodes change. */
    public List<ShellProfile> shellProfiles() {
        List<MortarMechanic> owned = this.mechanics.all();
        TowerStats stats = this.stats();
        return this.profiles.filter(last -> last.stats() == stats && last.owned().equals(owned)).orElseGet(() -> {
            MortarSpec spec = this.spec(owned);
            ShellProfiles fresh = new ShellProfiles(stats, owned,
                    this.loadableKinds().stream().map(kind -> this.profileOf(kind, stats, spec)).toList());
            this.profiles = Optional.of(fresh);
            return fresh;
        }).profiles();
    }

    private ShellProfile profileOf(ShellKind kind, TowerStats stats, MortarSpec spec) {
        Shell shell = new Shell(kind, stats);
        BlastRadii radii = this.blastOf(shell, spec);
        float cell = this.cellPixels();
        int damage = Math.round(stats.damage() * kind.damageFactor(stats.value(TowerStat.HEAVY_MULTIPLIER)));
        Set<EffectKind> applies = EnumSet.noneOf(EffectKind.class);
        if (spec.crackStacks() > 0) {
            applies.add(EffectKind.CRACKED);
        }
        applies.addAll(kind.applies());
        if (kind == ShellKind.NORMAL) {
            carriedPayload(spec).ifPresent(payload -> applies.addAll(payload.applies()));
        }
        if (kind == ShellKind.HEAVY) {
            spec.heavy().applies().ifPresent(applies::add);
        }
        ShellProfile profile = ShellProfile.of(kind, Damage.of(kind.damageType(), damage),
                this.shellStats(shell).speed() * TICKS_PER_SECOND / cell,
                new ShellProfile.Blast(radii.core() / cell, radii.edge() / cell, radii.edgeShare(), List.copyOf(applies)));
        return kind.ground().filter(ground -> ground.damageShare() > 0f)
                .map(ground -> profile.withGroundPulse(Damage.magic(groundStrength(stats, ground))))
                .orElse(profile);
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

    /** How much of the reload is still to run: 1 as a shell leaves, 0 once the next is ready. */
    public float getCoolDownFraction() {
        return this.clock.remaining();
    }

    /** The ticks since its last shell left, a volley's included, while they are within {@code window}. */
    public OptionalInt ticksSinceFired(int gameTime, int window) {
        return within(this.firedAt, gameTime, window);
    }

    /** How many ticks a whole reload takes at its rate now: the cooldown fraction falls by one over that many. */
    public double reloadTicks() {
        return this.clock.period() / Math.max(MINIMUM_RATE, this.attackRate());
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    /** The flash of the last nuke, for the frame build; empty before the first one. */
    public Optional<NukeFlash> getNukeFlash() {
        return this.nukeFlash;
    }

    /** Blasts that went off in the last few ticks, for the fading flash the board draws. */
    public List<BlastMark> getBlastMarks() {
        return List.copyOf(this.blastMarks);
    }

    /** A volley loads several shells for a longer reload, so its rate is shells a second over the whole cycle. */
    @Override
    protected TowerStatLine cadence(TowerStats stats, Function<TowerStat, List<StatPart>> parts) {
        VolleySpec volley = this.spec(this.mechanics.all()).volley();
        if (!volley.isVolley()) {
            return super.cadence(stats, parts);
        }
        return this.derivedRow(TowerStatRow.FIRE_RATE, TowerStat.ATTACK_SPEED, stats, parts,
                at -> (float) (volley.fired() * TICKS_PER_SECOND * at.attackRate()
                        / ((this.coolDownMax + 1) * volley.reloadFactor())));
    }

    @Override
    protected List<TowerStatLine> ownStats(TowerStats stats) {
        ProjectileStats shell = this.shellStats(new Shell(ShellKind.NORMAL, stats));
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
