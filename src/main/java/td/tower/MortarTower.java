package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.StackingDebuff;
import td.enemy.EnemyMob;
import td.projectile.CannonballProjectile;
import td.projectile.ProjectileStats;
import td.stat.StatPart;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.mortar.AirburstMechanic;
import td.tower.mortar.BarrageMechanic;
import td.tower.mortar.BlastMark;
import td.tower.mortar.BombletSpec;
import td.tower.mortar.BracketTracker;
import td.tower.mortar.BracketingMechanic;
import td.tower.mortar.BunkerBusterMechanic;
import td.tower.mortar.CarpetBombingMechanic;
import td.tower.mortar.ClusterShellMechanic;
import td.tower.mortar.FragmentationMechanic;
import td.tower.mortar.HeavyShellMechanic;
import td.tower.mortar.MortarMechanic;
import td.tower.mortar.MortarSpec;
import td.tower.mortar.NukeFlash;
import td.tower.mortar.PathLine;
import td.tower.mortar.PredictiveFireMechanic;
import td.tower.mortar.SalvoSpec;
import td.tower.mortar.ShellPlan;
import td.tower.mortar.ShellType;
import td.tower.mortar.ShellTypeMechanic;
import td.tower.mortar.ShrapnelBoostMechanic;
import td.tower.mortar.ShrapnelSpec;
import td.tower.mortar.ShrapnelStormMechanic;
import td.tower.mortar.TacticalNukeMechanic;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitKind;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.NodeEffects;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.RangeNodes;
import td.tower.upgrade.UpgradeCluster;
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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The Artillery: lobs a slow, unguided shell at the visible enemy furthest along the path, beyond a
 * dead zone around it. The shell flies to where the enemy was when fired, so a fast enemy dodges it.
 * The blast deals physical damage with distance falloff and cracks the plating of everything it
 * reaches.
 * <p>
 * Attuned, a shell that lands near the last one hits harder and wider (Bracketing). What each owned
 * node does lives in a {@link MortarMechanic}: the tower reads one spec from them whenever it looks for a
 * target or lands a shell.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class MortarTower extends AbstractTower {

    public static final int PRICE = 30;
    public static final float DAMAGE_POINTS = 32f;
    public static final float RANGE = 4.5f;
    public static final float SPLASH_RADIUS_BASE = 1.75f;
    /** Nothing closer than this can be shelled. */
    private static final float BASE_DEAD_ZONE_CELLS = 1.5f;
    /** Pixels a tick: slow enough to watch a shell fly and for a fast enemy to dodge it. */
    public static final float SHELL_SPEED = 8f;
    /** Ticks a nuke's flash and ring last. */
    public static final int NUKE_FLASH_TICKS = 10;
    /** Ticks the mark of any blast stays on the board. */
    public static final int BLAST_MARK_TICKS = 6;

    private static final int COOLDOWN_MAX = 70;
    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final int CRACKED_TICKS = 100;
    /** A shell's drawn size grows with the square root of how much harder than base it hits. */
    private static final float SHELL_SIZE_PER_DAMAGE_ROOT = 1f;
    private static final float MAX_SHELL_SIZE = 2.5f;
    private static final float NUKE_SHELL_SIZE = 1.5f;
    /** Shrapnel reaches this far past the main blast, as a multiple of its radius. */
    private static final float SHRAPNEL_RING_MULTIPLIER = 1.75f;
    /** What a bleed costs per cell travelled, as a share of the Mortar's damage. */
    private static final float BLEED_SHARE_PER_CELL = 0.08f;
    private static final int BLEED_TICKS = 80;
    /** Flight time depends on where the shell is going, so the aim is refined this many times. */
    private static final int PREDICTION_PASSES = 3;
    /** How far apart bomblets lie along the path, in cells, and how far from the impact the first one is. */
    private static final float BOMBLET_SPACING_CELLS = 0.8f;
    private static final float BOMBLET_SCATTER_CELLS = 0.8f;


    private static final UpgradeNode ATTUNE = UpgradeNode.of("base.attune", "Attune", 24);

    private static final UpgradeNode SIEGE_ROUNDS_1 = UpgradeNode.of("mortar.head.siege_rounds.1", "Siege Rounds", 30)
            .withModifiers(TowerModifiers.damage(9.6f).plus(TowerModifiers.bracketDamageStep(0.05f)));
    private static final UpgradeNode SIEGE_ROUNDS_2 = UpgradeNode.of("mortar.head.siege_rounds.2", "Siege Rounds II", 45)
            .withModifiers(TowerModifiers.damage(8f).plus(TowerModifiers.critChance(0.1f))
                    .plus(TowerModifiers.areaRadius(0.4375f)))
            .after(SIEGE_ROUNDS_1)
            .inTier(1);
    private static final UpgradeNode HEAVY_SHELL = UpgradeNode.of("mortar.head.siege_rounds.3", "Heavy Shell", 75)
            .withModifiers(TowerModifiers.damage(16f).plus(TowerModifiers.attackSpeed(-0.2f))
                    .plus(TowerModifiers.projectileSpeed(-0.25f)).plus(TowerModifiers.projectileSize(0.3f)))
            .after(SIEGE_ROUNDS_2)
            .inTier(2);
    private static final UpgradeNode TACTICAL_NUKE = UpgradeNode.of("mortar.head.siege_rounds.4a", "Tactical Nuke", 120)
            .after(HEAVY_SHELL)
            .inTier(3);
    private static final UpgradeNode BUNKER_BUSTER = UpgradeNode.of("mortar.head.siege_rounds.4b", "Bunker Buster", 120)
            .withModifiers(TowerModifiers.areaRadius(-0.525f))
            .after(HEAVY_SHELL)
            .inTier(3);

    private static final UpgradeNode FRAGMENTATION_ROUNDS_1 = UpgradeNode.of("mortar.head.fragmentation_rounds.1", "Fragmentation Rounds", 30);
    private static final UpgradeNode FRAGMENTATION_ROUNDS_2 = UpgradeNode.of("mortar.head.fragmentation_rounds.2", "Fragmentation Rounds II", 45)
            .after(FRAGMENTATION_ROUNDS_1)
            .inTier(1);
    private static final UpgradeNode CLUSTER_SHELL = UpgradeNode.of("mortar.head.fragmentation_rounds.3", "Cluster Shell", 75)
            .after(FRAGMENTATION_ROUNDS_2)
            .inTier(2);
    private static final UpgradeNode CARPET_BOMBING = UpgradeNode.of("mortar.head.fragmentation_rounds.4a", "Carpet Bombing", 120)
            .after(CLUSTER_SHELL)
            .inTier(3);
    private static final UpgradeNode SHRAPNEL_STORM = UpgradeNode.of("mortar.head.fragmentation_rounds.4b", "Shrapnel Storm", 120)
            .after(CLUSTER_SHELL)
            .inTier(3);

    private static final UpgradeNode RIFLED_BARREL = UpgradeNode.of("mortar.extra.ballistics.1", "Rifled Barrel", 24)
            .withModifiers(TowerModifiers.projectileSpeed(0.4f).plus(TowerModifiers.projectileSize(-0.3f)));
    private static final UpgradeNode PREDICTIVE_FIRE = UpgradeNode.of("mortar.extra.ballistics.2", "Predictive Fire", 36)
            .after(RIFLED_BARREL)
            .inTier(1);
    private static final UpgradeNode AIRBURST = UpgradeNode.of("mortar.extra.ballistics.3", "Airburst", 60)
            .withModifiers(TowerModifiers.areaRadius(0.4375f))
            .after(PREDICTIVE_FIRE)
            .inTier(2);
    private static final UpgradeNode BARRAGE = UpgradeNode.of("mortar.extra.ballistics.4", "Barrage", 90)
            .after(AIRBURST)
            .inTier(3);

    private static final UpgradeNode NAPALM = UpgradeNode.of("mortar.special.napalm", "Napalm", 120)
            .inTier(2);
    private static final UpgradeNode TAR = UpgradeNode.of("mortar.special.tar", "Tar", 120)
            .inTier(2);
    private static final UpgradeNode CRYO_SHELLS = UpgradeNode.of("mortar.special.cryo_shells", "Cryo Shells", 120)
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(RangeNodes.of(PRICE, RANGE, 0.3f,
                    NodeEffects.none().withModifiers(TowerModifiers.deadZone(1f))))
            .with(ATTUNE, SIEGE_ROUNDS_1, FRAGMENTATION_ROUNDS_1, RIFLED_BARREL,
                    SIEGE_ROUNDS_2, FRAGMENTATION_ROUNDS_2, PREDICTIVE_FIRE,
                    HEAVY_SHELL, CLUSTER_SHELL, NAPALM, TAR, AIRBURST,
                    TACTICAL_NUKE, BUNKER_BUSTER, CARPET_BOMBING, SHRAPNEL_STORM, CRYO_SHELLS, BARRAGE)
            .withCluster(UpgradeCluster.of("Rounds", SIEGE_ROUNDS_1, FRAGMENTATION_ROUNDS_1))
            .withCluster(UpgradeCluster.of("Special", NAPALM, TAR))
            .withCluster(UpgradeCluster.of("Mastery", TACTICAL_NUKE, BUNKER_BUSTER))
            .withCluster(UpgradeCluster.of("Mastery", CARPET_BOMBING, SHRAPNEL_STORM));

    private static final MechanicCatalogue<MortarMechanic> MECHANICS = MechanicCatalogue.<MortarMechanic>empty()
            .with(ATTUNE.id(), BracketingMechanic::new)
            .with(HEAVY_SHELL.id(), HeavyShellMechanic::new)
            .with(TACTICAL_NUKE.id(), TacticalNukeMechanic::new)
            .with(BUNKER_BUSTER.id(), BunkerBusterMechanic::new)
            .with(FRAGMENTATION_ROUNDS_1.id(), FragmentationMechanic::new)
            .with(FRAGMENTATION_ROUNDS_2.id(), ShrapnelBoostMechanic::new)
            .with(CLUSTER_SHELL.id(), ClusterShellMechanic::new)
            .with(CARPET_BOMBING.id(), CarpetBombingMechanic::new)
            .with(SHRAPNEL_STORM.id(), ShrapnelStormMechanic::new)
            .with(PREDICTIVE_FIRE.id(), PredictiveFireMechanic::new)
            .with(AIRBURST.id(), AirburstMechanic::new)
            .with(BARRAGE.id(), BarrageMechanic::new)
            .with(NAPALM.id(), () -> new ShellTypeMechanic(ShellType.NAPALM))
            .with(TAR.id(), () -> new ShellTypeMechanic(ShellType.TAR))
            .with(CRYO_SHELLS.id(), () -> new ShellTypeMechanic(ShellType.CRYO));

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK, this.restHeading());
    private final OwnedMechanics<MortarMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private BracketTracker bracketing = new BracketTracker();
    private final ZoneOwner zoneOwner = this::applyEffect;
    private final List<BlastMark> blastMarks = new ArrayList<>();
    private FireClock clock = new FireClock(COOLDOWN_MAX);
    private volatile NukeFlash nukeFlash;
    private int shellsFired;
    private int tickNow;
    private int salvoLeft = 0;
    private int salvoGap = 0;
    private EnemyMob currentTarget;

    public MortarTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.MORTAR, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .with(TowerStat.AREA_RADIUS, SPLASH_RADIUS_BASE).with(TowerStat.PROJECTILE_SPEED, 1f)
                .with(TowerStat.PROJECTILE_SIZE, 1f).with(TowerStat.DEAD_ZONE, BASE_DEAD_ZONE_CELLS)
                .with(TowerStat.BRACKET_DAMAGE_STEP, 0.1f), context, x, y);
    }

    @Override
    protected OwnedMechanics<MortarMechanic> ownedMechanics() {
        return this.mechanics;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /** Whom it may shell and how its shells fly and land, as the mechanics in {@code owned} make it. */
    private MortarSpec spec(List<MortarMechanic> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        MortarSpec spec = MortarSpec.from(view);
        for (MortarMechanic mechanic : owned) {
            spec = mechanic.refineSpec(spec);
        }
        return spec.withReach(spec.reach().withDeadZone(this.stats().value(TowerStat.DEAD_ZONE) * view.cellSize()));
    }

    private EnemyMob findTarget(MortarSpec spec) {
        List<EnemyMob> inReach = spec.reach().matching(this.context.enemies());
        return PreferringSelector.priority(new FurthestAlongPathSelector()).selectFrom(inReach).orElse(null);
    }

    public void doTick(int gameTime) {
        this.tickNow = gameTime;
        this.blastMarks.removeIf(mark -> gameTime - mark.startedAtTick() >= BLAST_MARK_TICKS);
        MortarSpec spec = this.spec(this.mechanics.all());
        if (this.salvoLeft > 0 && --this.salvoGap <= 0) {
            EnemyMob next = this.findTarget(spec);
            if (next != null) {
                this.currentTarget = next;
                this.fireAt(next, spec);
            }
            this.salvoLeft--;
            this.salvoGap = spec.salvo().gapTicks();
        }
        if (this.clock.isReady()) {
            this.currentTarget = this.findTarget(spec);
            if (this.currentTarget != null) {
                this.fireAt(this.currentTarget, spec);
                this.clock.spend(spec.salvo().reloadFactor());
                this.salvoLeft = spec.salvo().shells() - 1;
                this.salvoGap = spec.salvo().gapTicks();
            }
        }
        this.clock.advance(this.attackRate());
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        } else if (this.betweenWaves()) {
            this.turretAim.settle();
        }
    }

    @Override
    protected void endFight() {
        this.bracketing = new BracketTracker();
        this.clock = new FireClock(COOLDOWN_MAX);
        this.shellsFired = 0;
        this.salvoLeft = 0;
        this.salvoGap = 0;
        this.currentTarget = null;
    }

    @Override
    protected void takeRestPose() {
        this.turretAim.rest();
    }

    private void fireAt(EnemyMob target, MortarSpec spec) {
        Shell shell = new Shell(spec.shells().typeOf(this.shellsFired + 1), this.beginShot());
        this.shellsFired++;
        Vec2 aim = this.aimPoint(target, spec, shell.stats());
        this.context.projectiles().add(new CannonballProjectile(this.centerX, this.centerY, aim.x(), aim.y(),
                this.shellStats(shell), shell.type().look(), (x, y) -> this.onImpact(x, y, shell)));
    }

    /** What was fired: the kind of shell, and the stats it lands with however the Mortar changes meanwhile. */
    private record Shell(ShellType type, TowerStats stats) {
    }

    /** Where the shell is sent: the enemy where it stands, or with Predictive Fire where it will be on landing. */
    private Vec2 aimPoint(EnemyMob target, MortarSpec spec, TowerStats shot) {
        Vec2 aim = new Vec2(target.getX(), target.getY());
        if (!spec.leadsTarget()) {
            return aim;
        }
        float speed = SHELL_SPEED * shot.value(TowerStat.PROJECTILE_SPEED);
        for (int i = 0; i < PREDICTION_PASSES; i++) {
            int flightTicks = (int) Math.ceil(Math.hypot(aim.x() - this.centerX, aim.y() - this.centerY) / speed);
            aim = target.positionAfter(flightTicks);
        }
        return aim;
    }

    /** The shell as it flies and is drawn: its speed and size are stats, and its size follows how hard it hits. */
    private ProjectileStats shellStats(Shell shell) {
        TowerStats shot = shell.stats();
        float size = Math.min(MAX_SHELL_SIZE, SHELL_SIZE_PER_DAMAGE_ROOT
                * (float) Math.sqrt((double) shot.damage() / this.damageBase));
        float typeSize = shell.type() == ShellType.NUKE ? NUKE_SHELL_SIZE : 1f;
        float speedScale = shot.value(TowerStat.PROJECTILE_SPEED);
        return ProjectileStats.of(SHELL_SPEED * speedScale)
                .withSize(size * shot.value(TowerStat.PROJECTILE_SIZE) * typeSize)
                .withStreak(speedScale > 1f);
    }

    private float blastRadius(TowerStats shot, float stepScale) {
        return shot.value(TowerStat.AREA_RADIUS) * this.context.getBoard().scale() * stepScale;
    }

    /** One shell's landing: where, what was fired, and how wide and hard it hits after Bracketing. */
    private record Landing(Vec2 at, Shell shell, MortarSpec spec, Strength strength) {

        ShellType type() {
            return this.shell.type();
        }

        TowerStats stats() {
            return this.shell.stats();
        }

        double x() {
            return this.at.x();
        }

        double y() {
            return this.at.y();
        }

        int step() {
            return this.strength.step();
        }

        float radius() {
            return this.strength.radius();
        }

        float damage() {
            return this.strength.damage();
        }
    }

    /** How hard and wide a landing hits: its Bracketing step, blast radius in pixels and damage in units. */
    private record Strength(int step, float radius, float damage) {
    }

    private void onImpact(double x, double y, Shell shell) {
        MortarSpec spec = this.spec(this.mechanics.all());
        int scale = this.context.getBoard().scale();
        int step = this.bracketing.land(x, y, spec.bracket(), scale);
        ShellType type = shell.type();
        TowerStats shot = shell.stats();
        boolean nuke = type == ShellType.NUKE;
        float radius = this.blastRadius(shot, (1f + step * spec.bracket().radiusStep())
                * (nuke ? spec.nuke().radiusFactor() : 1f));
        float damage = shot.damage() * (1f + step * shot.value(TowerStat.BRACKET_DAMAGE_STEP))
                * (nuke ? spec.nuke().damageFactor() : 1f);
        Landing landing = new Landing(new Vec2(x, y), shell, spec, new Strength(step, radius, damage));
        List<EnemyMob> hit = this.blast(landing);
        this.dazeAround(landing, hit);
        if (spec.shrapnel().active()) {
            this.shrapnel(landing, hit);
        }
        if (spec.bomblets().isActive()) {
            this.bomblets(landing);
        }
        if (nuke) {
            this.nukeFlash = new NukeFlash(x, y, radius, this.tickNow + 1);
        }
        this.blastMarks.add(new BlastMark(new Vec2(x, y), radius, this.tickNow + 1, type.look()));
        type.zone().ifPresent(zone -> this.context.zones().add(new Zone(zone.kind(), x, y,
                zone.radius(scale, radius), this.lengthened(zone.lifetimeTicks()), zoneStrength(shot, zone.damageShare(), 1f),
                this.zoneOwner)));
    }

    private static int zoneStrength(TowerStats shot, float damageShare, float scale) {
        return Math.round(shot.damage() * damageShare * scale);
    }

    /** The main blast: damage with falloff and Cracked on everything inside, the centre hit harder. */
    private List<EnemyMob> blast(Landing landing) {
        MortarSpec spec = landing.spec();
        int scale = this.context.getBoard().scale();
        List<EnemyMob> hit = InRangeTargetQuery.everyone((int) Math.round(landing.x()), (int) Math.round(landing.y()),
                landing.radius()).matching(this.context.enemies());
        EnemyMob centre = spec.centre().isActive()
                ? this.enemyAtCentre(hit, landing.x(), landing.y(), spec.centre().radiusCells() * scale) : null;
        for (EnemyMob enemy : hit) {
            float falloff = falloff(landing.x(), landing.y(), landing.radius(), enemy, spec.flatCore());
            float centreFactor = enemy == centre ? spec.centre().damageFactor() : 1f;
            this.hit(landing, enemy, landing.damage() * falloff * centreFactor, HitKind.BLAST);
            this.applyEffect(enemy, sink -> Effect.cracked(CRACKED_TICKS, sink));
            if (enemy == centre) {
                this.applyStacks(enemy, StackingDebuff.SUNDERED, spec.centre().sunderStacks());
            }
        }
        return hit;
    }

    /**
     * One hit of {@code amount} on {@code enemy} in the landing shell's damage type, with the hit bonuses for
     * its kind, reported to the procs.
     */
    private void hit(Landing landing, EnemyMob enemy, float amount, HitKind kind) {
        HitAdjustment adjustment = this.hitAdjustment(enemy, kind, 0f, landing.stats());
        boolean critical = this.dealDamage(enemy,
                Damage.of(landing.type().damageType(), Math.round(amount * adjustment.damageFactor())),
                adjustment.applyTo(landing.stats().attack()));
        this.report(enemy, kind, critical, enemy.isDead());
    }

    /** How much of the damage reaches {@code enemy}: less the further from the centre, none past the radius. */
    private static float falloff(double x, double y, float radius, EnemyMob enemy, float flatCore) {
        double dx = x - enemy.getX();
        double dy = y - enemy.getY();
        double share = (dx * dx + dy * dy) / ((double) radius * radius);
        return share <= (double) flatCore * flatCore ? 1f : 1f - (float) share;
    }

    private void dazeAround(Landing landing, List<EnemyMob> hit) {
        if (!landing.spec().daze().isActive()) {
            return;
        }
        float dazeRadius = landing.spec().daze().radiusCells() * this.context.getBoard().scale();
        for (EnemyMob enemy : hit) {
            double dx = landing.x() - enemy.getX();
            double dy = landing.y() - enemy.getY();
            if (dx * dx + dy * dy <= (double) dazeRadius * dazeRadius) {
                this.applyEffect(enemy, sink -> Effect.dazed(landing.spec().daze().ticks(), sink));
            }
        }
    }

    /**
     * The ring of shrapnel past the main blast: a share of the shell's damage with no falloff, Cracked,
     * and, as the mechanics make it, the shell's own effect and a bleed. A shrapnel piece never leaves a zone.
     */
    private void shrapnel(Landing landing, List<EnemyMob> alreadyHit) {
        ShrapnelSpec shrapnel = landing.spec().shrapnel();
        int scale = this.context.getBoard().scale();
        float reach = landing.radius() * SHRAPNEL_RING_MULTIPLIER + shrapnel.reachCellsPerStep() * scale * landing.step();
        float piece = landing.damage() * shrapnel.damageShare() * shrapnel.scale();
        int crackedTicks = Math.round(CRACKED_TICKS * shrapnel.scale());
        for (EnemyMob enemy : InRangeTargetQuery.everyone((int) Math.round(landing.x()), (int) Math.round(landing.y()),
                reach).matching(this.context.enemies())) {
            if (alreadyHit.contains(enemy)) {
                continue;
            }
            this.hit(landing, enemy, piece, HitKind.SHRAPNEL);
            this.applyEffect(enemy, sink -> Effect.cracked(crackedTicks, sink));
            if (shrapnel.carriesShell()) {
                landing.type().zone().ifPresent(zone -> ZoneEffects.touch(zone.kind(),
                        zoneStrength(landing.stats(), zone.damageShare(), shrapnel.scale()), this.zoneOwner, enemy));
            }
            if (shrapnel.bleeds()) {
                int perCell = Math.round(landing.stats().damage() * BLEED_SHARE_PER_CELL * shrapnel.scale());
                AttackProfile bleed = landing.stats().attack().pastArmorAndPlating().asPeriodic();
                this.applyEffect(enemy, _ -> Effect.bleeding(perCell, BLEED_TICKS, bled -> this.dealDamage(enemy, bled, bleed)));
            }
        }
    }

    /** Small blasts along the path: scattered around the impact, or in a line ahead of it. */
    private void bomblets(Landing landing) {
        BombletSpec bomblets = landing.spec().bomblets();
        int scale = this.context.getBoard().scale();
        double[] offsets = new double[bomblets.count()];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = bomblets.inLine() ? (i + 1) * BOMBLET_SPACING_CELLS * scale
                    : scatterOffset(i, offsets.length) * BOMBLET_SCATTER_CELLS * scale;
        }
        List<Path> paths = this.context.level().paths().stream().map(PathRuntime::path).toList();
        float radius = bomblets.radiusCells() * scale;
        float damage = Math.round(landing.stats().damage() * bomblets.damageShare());
        for (Vec2 at : PathLine.along(paths, landing.x(), landing.y(), offsets)) {
            for (EnemyMob enemy : InRangeTargetQuery.everyone((int) Math.round(at.x()), (int) Math.round(at.y()), radius)
                    .matching(this.context.enemies())) {
                this.hit(landing, enemy, damage * falloff(at.x(), at.y(), radius, enemy, 0f), HitKind.BLAST);
                this.applyEffect(enemy, sink -> Effect.cracked(CRACKED_TICKS, sink));
            }
            this.blastMarks.add(new BlastMark(at, radius, this.tickNow + 1, landing.type().look()));
        }
    }

    /** Bomblet {@code index} of {@code count} scattered either side of the impact: -2, -1, 1, 2 for four. */
    private static double scatterOffset(int index, int count) {
        int half = count / 2;
        return index < half ? index - half : index - half + 1;
    }

    /** The enemy closest to where the shell landed, if it is within {@code reach} pixels of it. */
    private EnemyMob enemyAtCentre(List<EnemyMob> hit, double x, double y, float reach) {
        EnemyMob closest = null;
        double closestDistance2 = (double) reach * reach;
        for (EnemyMob enemy : hit) {
            double dx = x - enemy.getX();
            double dy = y - enemy.getY();
            double distance2 = dx * dx + dy * dy;
            if (distance2 <= closestDistance2) {
                closest = enemy;
                closestDistance2 = distance2;
            }
        }
        return closest;
    }

    /** The blast's radius in pixels before Bracketing. */
    float getSplashRadius() {
        return this.blastRadius(this.stats(), 1f);
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    /** The last landing and how tight its bracket is, for the ranging marker; empty when not attuned. */
    public Optional<BracketTracker.Marker> getRangingMarker() {
        return this.bracketing.marker();
    }

    /** The flash of the last nuke, for the frame build; empty before the first one. */
    public Optional<NukeFlash> getNukeFlash() {
        return Optional.ofNullable(this.nukeFlash);
    }

    /** Blasts that went off in the last few ticks, for the fading flash the board draws. */
    public List<BlastMark> getBlastMarks() {
        return List.copyOf(this.blastMarks);
    }

    /** A Barrage fires a salvo for a longer reload, so its rate is shells a second over the whole cycle. */
    @Override
    protected TowerStatLine cadence(TowerStats stats, Function<TowerStat, List<StatPart>> parts) {
        SalvoSpec salvo = this.spec(this.mechanics.all()).salvo();
        if (!salvo.isSalvo()) {
            return super.cadence(stats, parts);
        }
        return this.derivedRow(TowerStatRow.FIRE_RATE, TowerStat.ATTACK_SPEED, stats, parts,
                at -> (float) (salvo.shells() * TICKS_PER_SECOND * at.attackRate()
                        / ((this.coolDownMax + 1) * salvo.reloadFactor())));
    }

    @Override
    protected List<TowerStatLine> ownStats(TowerStats stats) {
        ProjectileStats shell = this.shellStats(new Shell(ShellType.PLAIN, stats));
        float cellsPerSecond = shell.speed() * TICKS_PER_SECOND / this.context.getBoard().scale();
        return List.of(TowerStatLine.fixed(TowerStatRow.PROJECTILE_SPEED, TowerStat.PROJECTILE_SPEED, cellsPerSecond),
                TowerStatLine.fixed(TowerStatRow.PROJECTILE_SIZE, TowerStat.PROJECTILE_SIZE, shell.size()));
    }

    @Override
    protected List<TowerBuffState> ownConditions() {
        List<TowerBuffState> states = new ArrayList<>();
        MortarSpec spec = this.spec(this.mechanics.all());
        this.bracketing.marker().filter(marker -> marker.step() > 0).ifPresent(marker -> states.add(
                TowerBuffState.of(TowerTerm.BRACKETING, TermText.of(
                        TermNumbers.signedPercent(this.stats().value(TowerStat.BRACKET_DAMAGE_STEP)) + " damage and "
                                + TermNumbers.signedPercent(spec.bracket().radiusStep()) + " blast radius a step, up to "
                                + marker.maxSteps() + " steps")).withStacks(marker.step())));
        ShellType primed = spec.shells().typeOf(this.shellsFired + 1);
        if (primed != ShellType.PLAIN) {
            states.add(TowerBuffState.of(primed.term(), primed.describe()));
        }
        this.mechanics.all().forEach(mechanic -> mechanic.condition().ifPresent(states::add));
        return states;
    }

    @Override
    protected List<NodeProgress> ownProgress() {
        List<NodeProgress> all = new ArrayList<>();
        MortarSpec spec = this.spec(this.mechanics.all());
        if (spec.nuke().every() > 0) {
            this.countingNode(TACTICAL_NUKE.id(), this.shellsFired % spec.nuke().every(), spec.nuke().every())
                    .ifPresent(all::add);
        }
        for (UpgradeNode node : List.of(NAPALM, TAR, CRYO_SHELLS)) {
            this.countingNode(node.id(), this.shellsFired % ShellPlan.CYCLE, ShellPlan.CYCLE).ifPresent(all::add);
        }
        return all;
    }

    @Override
    protected List<AppliedEffect> baseApplies() {
        return List.of(new AppliedEffect(EffectKind.CRACKED, List.of(new AppliedEffect.Source(this.displayName(),
                TermText.of("every shell applies ").plus(EffectKind.CRACKED)
                        .plus(" for " + TermNumbers.seconds((CRACKED_TICKS) / TickRate.TICKS_PER_SECOND))))));
    }

    @Override
    protected String targetingLabel() {
        return "first";
    }

    @Override
    protected String description() {
        return "Lobs a slow, unguided shell.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitMortarTower(this);
    }
}
