package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitBonus;
import td.tower.proc.HitCondition;
import td.tower.proc.HitKind;
import td.tower.proc.HitScaling;
import td.tower.proc.Proc;
import td.tower.proc.ProcCondition;
import td.tower.proc.ProcPayload;
import td.tower.splash.ArcMechanic;
import td.tower.splash.ArcPlanner;
import td.tower.splash.ArcSpec;
import td.tower.splash.ArcStrike;
import td.tower.splash.BlightHex;
import td.tower.splash.ChainLightningMechanic;
import td.tower.splash.ConductorMechanic;
import td.tower.splash.ContagionHex;
import td.tower.splash.CovenHex;
import td.tower.splash.DoomHex;
import td.tower.splash.FireControlMechanic;
import td.tower.splash.Hex;
import td.tower.splash.HexActions;
import td.tower.splash.HexCaster;
import td.tower.splash.HexKeeper;
import td.tower.splash.HexMechanic;
import td.tower.splash.HexPick;
import td.tower.splash.HexScene;
import td.tower.splash.HexSpec;
import td.tower.splash.HexTurn;
import td.tower.splash.InversionHex;
import td.tower.splash.LightningRodMechanic;
import td.tower.splash.MasteryMechanic;
import td.tower.splash.MineSpec;
import td.tower.splash.MinesMechanic;
import td.tower.splash.OverloadMechanic;
import td.tower.splash.PotencyMechanic;
import td.tower.splash.ReckoningHex;
import td.tower.splash.SaturationRule;
import td.tower.splash.ShotContext;
import td.tower.splash.ShotResult;
import td.tower.splash.SplashMechanic;
import td.tower.splash.SplashShot;
import td.tower.splash.SplashSpec;
import td.tower.splash.SympathyHex;
import td.tower.splash.ThunderclapMechanic;
import td.tower.splash.ThunderstrikeMechanic;
import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.MostNeighboursSelector;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.RandomSelector;
import td.tower.targeting.TargetSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.NodeEffects;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.RangeNodes;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.zone.Zone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Cheap, instant, never misses: it picks a visible enemy in range and blasts everything around it,
 * hidden enemies included, the damage falling off from the centre. Forked, the blast carries a
 * payload: lightning that arcs on past it (the Stormcaller) or curses (the Hexer).
 * <p>
 * What each owned node does lives in a {@link SplashMechanic}: the tower reads one spec from them for
 * every shot.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SplashTower extends AbstractTower {

    public static final int PRICE = 15;
    public static final float DAMAGE_POINTS = 14f;
    public static final float RANGE = 3.2f;
    public static final float BLAST_RADIUS_CELLS = 1.5f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.25;
    /** One shot a second. */
    private static final int COOLDOWN_MAX = 19;
    /** Saturation fades this long after the last blast that caught the enemy. */
    private static final float SATURATION_SECONDS = 1.5f;
    /** How far above its target a Thunderstrike's bolt is drawn from. */
    private static final float THUNDERSTRIKE_HEIGHT_CELLS = 2f;
    private static final TargetSelector THUNDERSTRIKE_AIM = PreferringSelector.priority(new HighestHealthSelector());
    private static final float CLOUD_RADIUS_CELLS = 1.5f;
    private static final int CLOUD_TICKS = 80;

    private static final UpgradeNode ATTUNE = UpgradeNode.of("base.attune", "Attune", 12)
            .withHitBonus(HitBonus.of(new HitCondition.HitKindIn(Set.of(HitKind.BLAST)),
                    new HitScaling.PerStackOf(EffectKind.SATURATED), HitAdjustment.damage(0.05f)));

    private static final UpgradeNode ARC = UpgradeNode.of("splash.head.arc.1", "Arc", 15)
            .inTier(1);
    private static final UpgradeNode CONDUCTOR = UpgradeNode.of("splash.head.arc.2", "Conductor", 23)
            .withModifiers(TowerModifiers.critChance(0.1f))
            .after(ARC)
            .inTier(1);
    private static final UpgradeNode OVERLOAD = UpgradeNode.of("splash.head.arc.3", "Overload", 38)
            .after(CONDUCTOR)
            .inTier(2);
    private static final UpgradeNode CHAIN_LIGHTNING = UpgradeNode.of("splash.head.arc.4a", "Chain Lightning", 60)
            .after(OVERLOAD)
            .inTier(3);
    private static final UpgradeNode LIGHTNING_ROD = UpgradeNode.of("splash.head.arc.4b", "Lightning Rod", 60)
            .after(OVERLOAD)
            .inTier(3);
    private static final UpgradeNode HEX = UpgradeNode.of("splash.head.hex.1", "Hex", 15)
            .inTier(1);
    private static final UpgradeNode WITCHS_BREW = UpgradeNode.of("splash.head.hex.2", "Witch's Brew", 23)
            .after(HEX)
            .inTier(1);
    private static final UpgradeNode SPREADING_CURSE = UpgradeNode.of("splash.head.hex.3", "Spreading Curse", 38)
            .after(WITCHS_BREW)
            .inTier(2);
    private static final UpgradeNode RIME_COVEN = UpgradeNode.of("splash.head.hex.4a", "Rime Coven", 60)
            .after(SPREADING_CURSE)
            .inTier(3);
    private static final UpgradeNode ASH_COVEN = UpgradeNode.of("splash.head.hex.4b", "Ash Coven", 60)
            .after(SPREADING_CURSE)
            .inTier(3);
    private static final UpgradeNode INVERSION = UpgradeNode.of("splash.special.inversion", "Hex of Inversion", 60)
            .after(HEX)
            .inTier(2);
    private static final UpgradeNode SYMPATHY = UpgradeNode.of("splash.special.sympathy", "Hex of Sympathy", 60)
            .after(HEX)
            .inTier(2);
    private static final UpgradeNode RECKONING = UpgradeNode.of("splash.special.reckoning", "Hex of Reckoning", 60)
            .after(HEX)
            .inTier(3);
    private static final UpgradeCondition A_CHAIN = UpgradeCondition.owns(ARC.id()).or(UpgradeCondition.owns(HEX.id()));
    private static final UpgradeNode WIDE_CHARGE = UpgradeNode.of("splash.extra.blast_engineering.1", "Wide Charge", 12)
            .withModifiers(TowerModifiers.areaRadius(0.375f).plus(TowerModifiers.blastEdge(0.25f)));
    private static final UpgradeNode SHAPED_CHARGE = UpgradeNode.of("splash.extra.blast_engineering.2", "Shaped Charge", 18)
            .withModifiers(TowerModifiers.saturationInnerStacks(1).plus(TowerModifiers.saturationCap(1)))
            .after(WIDE_CHARGE)
            .inTier(1);
    private static final UpgradeNode POTENCY = UpgradeNode.of("splash.extra.blast_engineering.3", "Potency", 30)
            .withRequires(A_CHAIN)
            .after(SHAPED_CHARGE)
            .inTier(2);
    private static final UpgradeNode MASTERY = UpgradeNode.of("splash.extra.blast_engineering.4", "Mastery", 45)
            .after(POTENCY)
            .inTier(3);

    private static final UpgradeNode THUNDERCLAP = UpgradeNode.of("splash.special.thunderclap", "Thunderclap", 60)
            .after(ARC)
            .inTier(2);
    private static final UpgradeNode STATIC_CHARGE = UpgradeNode.of("splash.special.static_charge", "Static Charge", 60)
            .withProc(Proc.onHit(new ProcPayload.Charge(60)).when(new ProcCondition.HitKindIn(Set.of(HitKind.ARC))))
            .after(ARC)
            .inTier(2);
    private static final UpgradeNode THUNDERSTRIKE = UpgradeNode.of("splash.special.thunderstrike", "Thunderstrike", 60)
            .after(ARC)
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(RangeNodes.of(PRICE, RANGE, 0.1f,
                    NodeEffects.none().withModifiers(TowerModifiers.areaRadius(0.15f))))
            .with(ATTUNE, WIDE_CHARGE, ARC, HEX, SHAPED_CHARGE, CONDUCTOR, WITCHS_BREW,
                    POTENCY, OVERLOAD, THUNDERCLAP, STATIC_CHARGE, SPREADING_CURSE, INVERSION, SYMPATHY,
                    MASTERY, CHAIN_LIGHTNING, LIGHTNING_ROD, THUNDERSTRIKE, RIME_COVEN, ASH_COVEN, RECKONING)
            .withFork(UpgradeCluster.of("Calling", ARC, HEX))
            .withCluster(UpgradeCluster.of("Special", THUNDERCLAP, STATIC_CHARGE))
            .withCluster(UpgradeCluster.of("Special", INVERSION, SYMPATHY))
            .withCluster(UpgradeCluster.of("Mastery", CHAIN_LIGHTNING, LIGHTNING_ROD))
            .withCluster(UpgradeCluster.of("Mastery", RIME_COVEN, ASH_COVEN));

    private static final MechanicCatalogue<SplashMechanic> MECHANICS = MechanicCatalogue.<SplashMechanic>empty()
            .with(ATTUNE.id(), FireControlMechanic::new)
            .with(RangeNodes.RANGE_3_ID, MinesMechanic::new)
            .with(ARC.id(), ArcMechanic::new)
            .with(CONDUCTOR.id(), ConductorMechanic::new)
            .with(OVERLOAD.id(), OverloadMechanic::new)
            .with(CHAIN_LIGHTNING.id(), ChainLightningMechanic::new)
            .with(LIGHTNING_ROD.id(), LightningRodMechanic::new)
            .with(POTENCY.id(), PotencyMechanic::new)
            .with(MASTERY.id(), MasteryMechanic::new)
            .with(THUNDERCLAP.id(), ThunderclapMechanic::new)
            .with(THUNDERSTRIKE.id(), ThunderstrikeMechanic::new)
            .with(HEX.id(), () -> HexMechanic.founding(new DoomHex()))
            .with(WITCHS_BREW.id(), () -> HexMechanic.adding(new BlightHex()).andCursing(3))
            .with(SPREADING_CURSE.id(), () -> HexMechanic.adding(new ContagionHex()))
            .with(RIME_COVEN.id(), () -> HexMechanic.adding(CovenHex.rime()))
            .with(ASH_COVEN.id(), () -> HexMechanic.adding(CovenHex.ash()))
            .with(INVERSION.id(), () -> HexMechanic.adding(new InversionHex()))
            .with(SYMPATHY.id(), () -> HexMechanic.adding(new SympathyHex()))
            .with(RECKONING.id(), () -> HexMechanic.adding(new ReckoningHex()));

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK, this.restHeading());
    private final OwnedMechanics<SplashMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private final TargetSelector randomAim;
    private final Hexing hexing = new Hexing();
    private HexTurn hexTurn = new HexTurn();
    private HexKeeper hexKeeper = new HexKeeper(this.hexing);
    private FireClock clock = new FireClock(COOLDOWN_MAX);
    private List<Blast> blasts = List.of();
    private List<Trace> arcs = List.of();
    private List<Trace> casts = List.of();
    private EnemyMob aimedAt;
    private int lastShotTick;
    private int currentTick;
    private int shotsFired;
    private final List<Zone> mines = new ArrayList<>();

    public SplashTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SPLASH, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .with(TowerStat.AREA_RADIUS, BLAST_RADIUS_CELLS).with(TowerStat.BLAST_EDGE, 0f)
                .with(TowerStat.SATURATION_CAP, 3f).with(TowerStat.SATURATION_INNER_STACKS, 1f), context, x, y);
        this.randomAim = PreferringSelector.priority(new RandomSelector(context.random()));
    }

    @Override
    protected OwnedMechanics<SplashMechanic> ownedMechanics() {
        return this.mechanics;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /** What its blast is like and what it carries, as the mechanics in {@code owned} make it. */
    private SplashSpec spec(List<SplashMechanic> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        SplashSpec spec = SplashSpec.from(view);
        for (SplashMechanic mechanic : owned) {
            spec = mechanic.refineSpec(spec);
        }
        return spec;
    }

    private float blastRadius() {
        return this.stats().value(TowerStat.AREA_RADIUS) * this.context.getBoard().scale();
    }

    /** How far every blast distance reaches compared with the base blast. */
    private float distanceScale() {
        return this.stats().value(TowerStat.AREA_RADIUS) / BLAST_RADIUS_CELLS;
    }

    private SaturationRule saturation(SplashSpec spec) {
        return spec.blast().saturates()
                ? new SaturationRule(1, Math.round(this.stats().value(TowerStat.SATURATION_INNER_STACKS)),
                        Math.round(this.stats().value(TowerStat.SATURATION_CAP)))
                : SaturationRule.none();
    }

    /** The share of damage a blast deals at {@code share} of its radius out, floored by its edge. */
    private float falloff(float share) {
        return Math.max(this.stats().value(TowerStat.BLAST_EDGE), 1f - share * share);
    }

    public void doTick(int gameTime) {
        this.currentTick = gameTime;
        List<SplashMechanic> owned = this.mechanics.all();
        if (!this.hexKeeper.isEmpty()) {
            this.hexKeeper.doTick(this.context.waves().tickOfWave(gameTime), this.spec(owned).hexes());
        }
        if (this.clock.isReady()) {
            SplashSpec spec = this.spec(owned);
            List<EnemyMob> inReach = spec.reach().matching(this.context.enemies());
            if (inReach.isEmpty()) {
                this.hexTurn.restart();
                this.blasts = List.of();
            } else if (!this.cast(inReach, spec)) {
                this.aimFor(spec).selectFrom(inReach).ifPresent(primary -> this.fire(primary, spec, owned));
            }
        }
        this.clock.advance(this.attackRate());
        if (this.aimedAt != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.aimedAt.getX(), this.aimedAt.getY()));
        } else if (this.betweenWaves()) {
            this.turretAim.settle();
        }
    }

    /** Its mines go with the wave, like every zone. */
    @Override
    protected void endFight() {
        this.hexTurn = new HexTurn();
        this.hexKeeper = new HexKeeper(this.hexing);
        this.clock = new FireClock(COOLDOWN_MAX);
        this.aimedAt = null;
        this.shotsFired = 0;
        this.mines.clear();
    }

    @Override
    protected void takeRestPose() {
        this.turretAim.rest();
    }

    /**
     * A Hexer's every fourth shot is a cast: the next hex in turn that has a target curses it, and
     * from Witch's Brew on the most Saturated enemies within the blast around it too. Whether it
     * cast; when no hex has a target, the shot is a blast instead.
     */
    private boolean cast(List<EnemyMob> inReach, SplashSpec spec) {
        HexSpec hexes = spec.hexes();
        if (!hexes.isActive() || (this.shotsFired + 1) % HexSpec.CAST_EVERY != 0) {
            return false;
        }
        float blastRadius = this.blastRadius();
        Optional<HexPick> pick = this.hexTurn.take(hexes.pool(), new HexScene(inReach, blastRadius,
                this.distance(ContagionHex.SPREAD_CELLS), this.distance(HexSpec.SHARE_CELLS)));
        if (pick.isEmpty()) {
            return false;
        }
        this.beginShot();
        this.shotsFired++;
        this.lastShotTick = this.currentTick;
        this.clock.spend();
        Hex hex = pick.get().hex();
        EnemyMob target = pick.get().target();
        this.aimedAt = target;
        this.blasts = List.of();
        this.arcs = List.of();
        List<Trace> traces = new ArrayList<>();
        for (EnemyMob cursed : this.cursedBy(hex, target, hexes.cursesPerCast(), blastRadius)) {
            traces.add(new Trace(this.centerX, this.centerY, (float) cursed.getX(), (float) cursed.getY()));
            hex.cast(cursed, hexes, this.hexing);
        }
        this.casts = List.copyOf(traces);
        return true;
    }

    /** {@code target}, then the most Saturated enemies within the blast around it that lack the hex. */
    private List<EnemyMob> cursedBy(Hex hex, EnemyMob target, int curses, float blastRadius) {
        List<EnemyMob> cursed = new ArrayList<>(List.of(target));
        InRangeTargetQuery.everyone((int) target.getX(), (int) target.getY(), blastRadius)
                .matching(this.context.enemies()).stream()
                .filter(enemy -> enemy != target && !enemy.hasEffect(hex.kind()))
                .sorted(Comparator.comparingInt((EnemyMob enemy) -> enemy.effectStacks(EffectKind.SATURATED)).reversed())
                .limit(curses - 1L)
                .forEach(cursed::add);
        return cursed;
    }

    /** {@code cells} grown by the blast radius bonus, in pixels: how far a hex's payload reaches. */
    private float distance(float cells) {
        return cells * this.context.getBoard().scale() * this.distanceScale();
    }

    /** Mastery: a hexed enemy that died leaves a cloud that gives what stands in it the debuffs it carried. */
    private void leaveCloud(EnemyMob dead) {
        List<Effect> carried = dead.activeEffects().stream().filter(effect -> effect.kind().spreadsWithCurse()).toList();
        if (carried.isEmpty()) {
            return;
        }
        this.context.zones().add(Zone.cloud(dead.getX(), dead.getY(),
                CLOUD_RADIUS_CELLS * this.context.getBoard().scale(), this.lengthened(CLOUD_TICKS), this::applyEffect,
                carried));
    }

    private TargetSelector aimFor(SplashSpec spec) {
        if (!spec.blast().aimsAtCrowds()) {
            return this.randomAim;
        }
        return PreferringSelector.priority(new MostNeighboursSelector(this.blastRadius()));
    }

    /**
     * One shot, as the mechanics shape it: a Thunderstrike first if one is due, then the blast, the arcs
     * it carries, and a Thunderclap's discharge if one is armed. The mechanics then react to how it
     * landed.
     */
    private void fire(EnemyMob primary, SplashSpec spec, List<SplashMechanic> owned) {
        TowerStats shotStats = this.beginShot();
        this.lastShotTick = this.currentTick;
        this.shotsFired++;
        this.clock.spend();
        this.aimedAt = primary;
        this.casts = List.of();
        SplashShot shot = SplashShot.plain();
        ShotContext context = new ShotContext(this.shotsFired);
        for (SplashMechanic mechanic : owned) {
            shot = mechanic.shape(shot, context);
        }
        if (this.saturation(spec).isActive() && primary.hasEffect(EffectKind.SATURATED)) {
        }
        List<Trace> traces = new ArrayList<>();
        Optional<EnemyMob> struck = shot.thunderstrike()
                ? this.thunderstrike(spec, shot.thunderstrikeFactor(), shotStats, traces) : Optional.empty();
        Blast blast = this.blast(primary, (int) primary.getX(), (int) primary.getY(), spec, shotStats);
        this.blasts = List.of(blast);
        if (spec.mines().isActive() && this.shotsFired % spec.mines().every() == 0) {
            this.leaveMine(primary.getX(), primary.getY(), spec);
        }
        boolean critical = struck.isPresent();
        if (spec.arcs().active()) {
            EnemyMob start = struck.orElseGet(() -> this.outermost(blast));
            ArcSpec arcs = struck.isPresent() ? spec.arcs().withShareAtLeast(1f) : spec.arcs();
            critical |= this.arc(blast, start, spec.withArcs(arcs), shotStats, traces);
        }
        if (shot.thunderclap()) {
            this.discharge(spec, shot.thunderclapShare(), shotStats, traces);
        }
        this.arcs = List.copyOf(traces);
        ShotResult result = new ShotResult(critical);
        for (SplashMechanic mechanic : owned) {
            mechanic.react(result);
        }
    }

    /**
     * Hits the primary first, then everything else within the blast, each for its share of the
     * damage at its distance; then Saturates them all. {@code shot} is the stats the blast hits with.
     */
    private Blast blast(EnemyMob primary, int x, int y, SplashSpec spec, TowerStats shot) {
        float radius = this.blastRadius();
        List<EnemyMob> caught = new ArrayList<>(InRangeTargetQuery.everyone(x, y, radius).matching(this.context.enemies()));
        caught.remove(primary);
        caught.addFirst(primary);
        SaturationRule saturation = this.saturation(spec);
        boolean critical = false;
        for (EnemyMob enemy : caught) {
            float distanceShare = (float) (Math.hypot(enemy.getX() - x, enemy.getY() - y) / radius);
            HitAdjustment adjustment = this.hitAdjustment(enemy, HitKind.BLAST, 0f, shot);
            int amount = Math.round(shot.damage() * this.falloff(Math.min(1f, distanceShare))
                    * adjustment.damageFactor());
            boolean crit = this.dealDamage(enemy, Damage.physical(amount), adjustment.applyTo(shot.attack()));
            this.report(enemy, HitKind.BLAST, crit, enemy.isDead());
            critical |= enemy == primary && crit;
            if (saturation.isActive() && !enemy.isDead()) {
                int stacks = saturation.stacksAt(distanceShare);
                int ticks = Math.round(SATURATION_SECONDS * TICKS_PER_SECOND);
                this.applyEffect(enemy, sink -> Effect.saturated(stacks, ticks, sink).withStackCap(saturation.cap()));
            }
        }
        return new Blast(primary, List.copyOf(caught), new Blast.Area(x, y, radius), critical);
    }

    /** A sold Splash takes its mines with it. */
    @Override
    public void doCleanup() {
        super.doCleanup();
        this.mines.forEach(this.context.zones()::remove);
        this.mines.clear();
    }

    /** Leaves a mine where the blast landed, replacing the oldest when it already keeps its most. */
    private void leaveMine(double x, double y, SplashSpec spec) {
        MineSpec mine = spec.mines();
        while (this.mines.size() >= mine.max()) {
            this.context.zones().remove(this.mines.removeFirst());
        }
        Zone laid = Zone.mine(x, y, mine.radiusCells() * this.context.getBoard().scale(), this.lengthened(mine.lifetimeTicks()),
                this::applyEffect, this::explode);
        this.mines.add(laid);
        this.context.zones().add(laid);
    }

    /** A mine went off under {@code by}: one blast where it lay. */
    private void explode(Zone mine, EnemyMob by) {
        this.mines.remove(mine);
        this.blast(by, (int) mine.x(), (int) mine.y(), this.spec(this.mechanics.all()), this.stats());
    }

    /** The enemy the blast caught furthest from its centre, where its arcs start. */
    private EnemyMob outermost(Blast blast) {
        return blast.caught().stream()
                .max(Comparator.comparingDouble(enemy -> Math.hypot(enemy.getX() - blast.area().centerX(),
                        enemy.getY() - blast.area().centerY())))
                .orElse(blast.primary());
    }

    /**
     * The arcs the blast carries, from {@code start}; each lands as a magic hit. Records where they
     * ran, and tells whether one crit.
     */
    private boolean arc(Blast blast, EnemyMob start, SplashSpec spec, TowerStats shot, List<Trace> traces) {
        ArcSpec arcs = spec.arcs();
        float cellReach = this.context.getBoard().scale() * this.distanceScale();
        float bound = blast.area().radius()
                + arcs.jumps() * ArcSpec.reachCells(this.saturation(spec).cap()) * cellReach;
        List<EnemyMob> candidates = InRangeTargetQuery.everyone(blast.area().centerX(), blast.area().centerY(), bound)
                .matching(this.context.enemies());
        Set<EnemyMob> struck = Collections.newSetFromMap(new IdentityHashMap<>());
        struck.addAll(blast.caught());
        List<ArcStrike> strikes = ArcPlanner.plan(blast.primary(), start, candidates, struck, arcs,
                from -> ArcSpec.reachCells(from.effectStacks(EffectKind.SATURATED)) * cellReach);
        boolean critical = false;
        for (ArcStrike strike : strikes) {
            traces.add(new Trace((float) strike.from().getX(), (float) strike.from().getY(),
                    (float) strike.to().getX(), (float) strike.to().getY()));
            critical |= this.strikeWithArc(strike.to(), strike.share(), spec, arcs.daze(), shot);
        }
        return critical;
    }

    /** Thunderclap: an arc from the tower into every enemy in range; each one it crits is Dazed. */
    private void discharge(SplashSpec spec, float share, TowerStats shot, List<Trace> traces) {
        for (EnemyMob enemy : spec.reach().matching(this.context.enemies())) {
            traces.add(new Trace(this.centerX, this.centerY, (float) enemy.getX(), (float) enemy.getY()));
            this.strikeWithArc(enemy, share, spec, SplashShot.DAZE_SECONDS, shot);
        }
    }

    /**
     * Thunderstrike: lightning from above onto the healthiest enemy in range, the Priority first,
     * for several times the blast as magic, and a Daze. The enemy it struck, if any.
     */
    private Optional<EnemyMob> thunderstrike(SplashSpec spec, float factor, TowerStats shot, List<Trace> traces) {
        Optional<EnemyMob> target = THUNDERSTRIKE_AIM.selectFrom(spec.reach().matching(this.context.enemies()));
        target.ifPresent(enemy -> {
            float x = (float) enemy.getX();
            float y = (float) enemy.getY();
            traces.add(new Trace(x, y - THUNDERSTRIKE_HEIGHT_CELLS * this.context.getBoard().scale(), x, y));
            this.dealDamage(enemy, Damage.magic(Math.round(shot.damage() * factor)), shot.attack());
            if (!enemy.isDead()) {
                int ticks = Math.round(SplashShot.DAZE_SECONDS * TICKS_PER_SECOND);
                this.applyEffect(enemy, sink -> Effect.dazed(ticks, sink));
            }
        });
        return target;
    }

    /** One arc's hit on {@code target}: magic, with the arc spec's crit rules; a crit Dazes for {@code dazeSeconds}. */
    private boolean strikeWithArc(EnemyMob target, float share, SplashSpec spec, float dazeSeconds, TowerStats shot) {
        ArcSpec arcs = spec.arcs();
        AttackProfile attack = shot.attack();
        if (!arcs.canCrit()) {
            attack = attack.withCritChance(0f);
        } else {
            SaturationRule saturation = this.saturation(spec);
            boolean fullySaturated = saturation.isActive()
                    && target.effectStacks(EffectKind.SATURATED) >= saturation.cap();
            float bonus = arcs.critBonus() + (fullySaturated ? arcs.saturatedCritBonus() : 0f);
            attack = attack.withCritChance(Math.min(1f, attack.critChance() + bonus));
        }
        attack = attack.withCritDamageBonus(arcs.critDamage());
        HitAdjustment adjustment = this.hitAdjustment(target, HitKind.ARC, 0f, shot);
        boolean critical = this.dealDamage(target,
                Damage.magic(Math.round(shot.damage() * share * adjustment.damageFactor())), adjustment.applyTo(attack));
        this.report(target, HitKind.ARC, critical, target.isDead());
        if (critical && dazeSeconds > 0f && !target.isDead()) {
            int ticks = Math.round(dazeSeconds * TICKS_PER_SECOND);
            this.applyEffect(target, sink -> Effect.dazed(ticks, sink));
        }
        return critical;
    }

    /** What the fork has made it: the Stormcaller on Arc, the Hexer on Hex. */
    public Fork fork() {
        SplashSpec spec = this.spec(this.mechanics.all());
        if (spec.arcs().active()) {
            return Fork.STORMCALLER;
        }
        return spec.hexes().isActive() ? Fork.HEXER : Fork.NONE;
    }

    @Override
    public String displayName() {
        return switch (this.fork()) {
            case NONE -> super.displayName();
            case STORMCALLER -> "Stormcaller";
            case HEXER -> "Hexer";
        };
    }

    /** The blast's target, which the turret follows; {@code null} when the last look found none. */
    public EnemyMob getPrimaryTarget() {
        return this.blasts.isEmpty() ? null : this.blasts.getFirst().primary();
    }

    /** Where the latest shot's curses went, when it was a cast. */
    public List<Trace> getCasts() {
        return this.casts;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    /** The blasts of the latest shot. */
    public List<Blast> getBlasts() {
        return this.blasts;
    }

    /** The arcs of the latest shot, a Thunderstrike's bolt among them, in the order they struck. */
    public List<Trace> getArcs() {
        return this.arcs;
    }

    /** How many ticks ago it last fired. */
    public int ticksSinceShot(int gameTime) {
        return gameTime - this.lastShotTick;
    }

    public float getCoolDownFraction() {
        return this.clock.remaining();
    }

    @Override
    protected List<NodeProgress> ownProgress() {
        List<NodeProgress> all = new ArrayList<>();
        SplashSpec spec = this.spec(this.mechanics.all());
        this.countingNode(HEX.id(), this.shotsFired % HexSpec.CAST_EVERY, HexSpec.CAST_EVERY).ifPresent(all::add);
        if (spec.mines().isActive()) {
            all.add(new NodeProgress(RangeNodes.RANGE_3_ID, this.shotsFired % spec.mines().every(),
                    spec.mines().every()));
        }
        this.countingNode(THUNDERSTRIKE.id(), this.shotsFired % ThunderstrikeMechanic.INTERVAL,
                ThunderstrikeMechanic.INTERVAL).ifPresent(all::add);
        return all;
    }

    @Override
    protected String targetingLabel() {
        return this.spec(this.mechanics.all()).blast().aimsAtCrowds() ? "most crowded" : "random";
    }

    @Override
    protected String description() {
        return "Damage falls off away from the blast's centre.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSplashTower(this);
    }

    /** What the hexes may make this tower do, as they are cast and as they pay out. */
    private final class Hexing implements HexActions, HexCaster {

        @Override
        public void curse(EnemyMob target, EffectKind kind, int ticks) {
            SplashTower.this.applyEffect(target, sink -> Effect.hex(kind, ticks, sink));
            SplashTower.this.hexKeeper.record(target, kind);
        }

        @Override
        public void applyEffect(EnemyMob target, Function<DamageSink, Effect> effect) {
            SplashTower.this.applyEffect(target, effect);
        }

        @Override
        public void dealPeriodicDamage(EnemyMob target, Damage damage) {
            SplashTower.this.dealPeriodicDamage(target, damage);
        }

        @Override
        public float distance(float cells) {
            return SplashTower.this.distance(cells);
        }

        @Override
        public EnemyRegistry enemies() {
            return SplashTower.this.context.enemies();
        }

        @Override
        public void leaveCloud(EnemyMob dead) {
            SplashTower.this.leaveCloud(dead);
        }

        @Override
        public void poison(EnemyMob target, float weaponShare, int ticks) {
            Damage perTick = Damage.magic(Math.round(SplashTower.this.damageCurrent() * weaponShare));
            SplashTower.this.applyEffect(target, sink -> Effect.poison(perTick, ticks, sink));
        }

        @Override
        public void burn(EnemyMob target, float weaponShare, int ticks) {
            Damage perTick = Damage.magic(Math.round(SplashTower.this.damageCurrent() * weaponShare));
            SplashTower.this.applyEffect(target, sink -> Effect.burn(perTick, ticks, sink));
        }

        @Override
        public void chill(EnemyMob target, float amount, int ticks) {
            SplashTower.this.applyEffect(target, sink -> Effect.chill(amount, ticks, sink));
        }
    }

    /** What the fork has made the Splash. */
    public enum Fork {
        NONE,
        STORMCALLER,
        HEXER
    }

    /**
     * One blast: the enemy it centred on, everything it caught (the primary first), and where and
     * how wide it was.
     *
     * @param critical whether the primary hit was a critical hit
     */
    public record Blast(EnemyMob primary, List<EnemyMob> caught, Area area, boolean critical) {

        /** The disc the blast covered, in board pixels. */
        public record Area(int centerX, int centerY, float radius) {
        }
    }

    /** Where one arc, bolt or cast ran, in board pixels. */
    public record Trace(float fromX, float fromY, float toX, float toY) {
    }
}
