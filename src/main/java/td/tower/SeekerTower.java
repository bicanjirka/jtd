package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.projectile.MissileGuide;
import td.projectile.MissileProjectile;
import td.projectile.ProjectileStats;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitKind;
import td.tower.seeker.BroodSpec;
import td.tower.seeker.FeedingMechanic;
import td.tower.seeker.FrozenLedger;
import td.tower.seeker.IceRider;
import td.tower.seeker.IceWakeMechanic;
import td.tower.seeker.MissileKind;
import td.tower.seeker.NestView;
import td.tower.seeker.Release;
import td.tower.seeker.ReleaseMechanic;
import td.tower.seeker.RichBroodMechanic;
import td.tower.seeker.SecondMissileMechanic;
import td.tower.seeker.SeekerMechanic;
import td.tower.seeker.SeekerNest;
import td.tower.seeker.SeekerSpec;
import td.tower.seeker.ShatterMark;
import td.tower.seeker.ShatterMechanic;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.SoonestOutSelector;
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
import td.util.ThreadConfined;
import td.util.TickRate;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static td.tower.upgrade.MechanicCatalogue.entry;

/**
 * The control tower: its nest loads a homing missile whenever it has room, with or without a target, so it is full at
 * every wave start; with an enemy in range the stored missiles launch one after another, each at the visible enemy
 * that would leave soonest. Every enemy a missile hits is frozen, unless a Shield takes the whole hit. A missile whose
 * enemy is frozen before it lands turns to another close by, or stays on it when there is none.
 * <p>
 * What each owned node does lives in its data or in a {@link SeekerMechanic}: the tower reads one spec from them
 * whenever it loads, launches or lands a missile.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SeekerTower extends AbstractTower {

    public static final int PRICE = 15;
    public static final float DAMAGE_POINTS = 15f;
    public static final float RANGE = 4f;
    /** Pixels a tick at its top speed: slow enough to see a missile hunt. */
    public static final float MISSILE_SPEED = 8f;
    /** The ring the nest's missiles sit on turns this far a tick while the tower fights. */
    public static final double RING_RADIANS_PER_TICK = 0.08;
    /** How long the shards of a burst ice fly. */
    public static final int SHATTER_MARK_TICKS = 10;

    /** A load every 2 s. */
    private static final int COOLDOWN_MAX = 39;
    private static final float FREEZE_TICKS = 20f;
    private static final float NEST_SIZE = 3f;
    private static final int LAUNCH_GAP_TICKS = 4;
    /** How far around itself a missile that lost its enemy looks for another. */
    private static final float RETARGET_CELLS = 2f;
    private static final TargetSelector SOONEST_OUT = PreferringSelector.priority(new SoonestOutSelector());

    private static final UpgradeNode QUICK_BROOD = UpgradeNode.of("seeker.quick_brood", "Quick Brood", 9)
            .withModifiers(TowerModifiers.attackSpeed(2f / 1.6f - 1f));
    private static final UpgradeNode DEEP_FREEZE = UpgradeNode.of("seeker.deep_freeze", "Deep Freeze", 3)
            .withModifiers(TowerModifiers.freezeDuration(6));
    private static final UpgradeNode WIDE_COMB = UpgradeNode.of("seeker.wide_comb", "Wide Comb", 3)
            .withModifiers(TowerModifiers.nestCapacity(2));
    private static final UpgradeNode LONG_FLIGHT = UpgradeNode.of(RangeNodes.RANGE_ID, "Long Flight", 3)
            .withModifiers(TowerModifiers.range(1f));

    private static final UpgradeNode SWARM = UpgradeNode.of("seeker.swarm", "Swarm", 16)
            .inTier(1);
    private static final UpgradeNode HATCHING = UpgradeNode.of("seeker.hatching", "Hatching", 16)
            .inTier(1);
    private static final UpgradeNode FEEDING = UpgradeNode.of("seeker.feeding", "Feeding", 8)
            .inTier(1);
    private static final UpgradeNode SWIFT_WINGS = UpgradeNode.of("seeker.swift_wings", "Swift Wings", 6)
            .withModifiers(TowerModifiers.projectileSpeed(0.5f))
            .inTier(1);
    private static final UpgradeNode WIDE_COMB_2 = UpgradeNode.of("seeker.wide_comb_2", "Wide Comb II", 4)
            .withModifiers(TowerModifiers.nestCapacity(2))
            .inTier(1);

    private static final UpgradeNode RUBY = UpgradeNode.of("seeker.ruby", "Ruby", 19)
            .inTier(2);
    private static final UpgradeNode SHADE = UpgradeNode.of("seeker.shade", "Shade", 19)
            .inTier(2);
    private static final UpgradeNode SHARP_STING = UpgradeNode.of("seeker.sharp_sting", "Sharp Sting", 39)
            .withModifiers(TowerModifiers.damage(10f))
            .inTier(2);
    private static final UpgradeNode LONG_FLIGHT_2 = UpgradeNode.of(RangeNodes.RANGE_2_ID, "Long Flight II", 10)
            .withModifiers(TowerModifiers.range(1f))
            .inTier(2);

    private static final UpgradeNode ICE_WAKE = UpgradeNode.of("seeker.ice_wake", "Ice Wake", 22)
            .inTier(3);
    private static final UpgradeNode SHATTER = UpgradeNode.of("seeker.shatter", "Shatter", 22)
            .inTier(3);
    private static final UpgradeNode RICH_BROOD = UpgradeNode.of("seeker.rich_brood", "Rich Brood", 14)
            .withRequires(UpgradeCondition.owns(RUBY.id()).or(UpgradeCondition.owns(SHADE.id())))
            .inTier(3);
    private static final UpgradeNode DEEP_FREEZE_2 = UpgradeNode.of("seeker.deep_freeze_2", "Deep Freeze II", 20)
            .withModifiers(TowerModifiers.freezeDuration(20))
            .inTier(3);
    private static final UpgradeNode QUICK_BROOD_2 = UpgradeNode.of("seeker.quick_brood_2", "Quick Brood II", 31)
            .withModifiers(TowerModifiers.attackSpeed(2f / 1.3f - 2f / 1.6f))
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(QUICK_BROOD, DEEP_FREEZE, WIDE_COMB, LONG_FLIGHT))
            .with(SWARM, HATCHING, FEEDING, SWIFT_WINGS, WIDE_COMB_2,
                    RUBY, SHADE, SHARP_STING, LONG_FLIGHT_2,
                    ICE_WAKE, SHATTER, RICH_BROOD, DEEP_FREEZE_2, QUICK_BROOD_2)
            .withCluster(UpgradeCluster.of("Release", SWARM, HATCHING))
            .withCluster(UpgradeCluster.of("Second missile", RUBY, SHADE))
            .withCluster(UpgradeCluster.of("Around the ice", ICE_WAKE, SHATTER));

    private static final MechanicCatalogue<SeekerMechanic> MECHANICS = MechanicCatalogue.of(
            entry(SWARM.id(), () -> new ReleaseMechanic(Release.SWARM)),
            entry(HATCHING.id(), () -> new ReleaseMechanic(Release.HATCHING)),
            entry(FEEDING.id(), FeedingMechanic::new),
            entry(RUBY.id(), () -> new SecondMissileMechanic(MissileKind.RUBY)),
            entry(SHADE.id(), () -> new SecondMissileMechanic(MissileKind.SHADE)),
            entry(RICH_BROOD.id(), RichBroodMechanic::new),
            entry(ICE_WAKE.id(), IceWakeMechanic::new),
            entry(SHATTER.id(), ShatterMechanic::new));

    private static final TowerVisitor<Optional<SeekerTower>> AS_SEEKER = new TowerVisitor<>() {
        @Override
        public Optional<SeekerTower> visitSniperTower(SniperTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<SeekerTower> visitSplashTower(SplashTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<SeekerTower> visitPulseTower(PulseTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<SeekerTower> visitAuraTower(AuraTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<SeekerTower> visitMortarTower(MortarTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<SeekerTower> visitSeekerTower(SeekerTower tower) {
            return Optional.of(tower);
        }

        @Override
        public Optional<SeekerTower> visitCinderTower(CinderTower tower) {
            return Optional.empty();
        }
    };

    private final OwnedMechanics<SeekerMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private SeekerNest nest = new SeekerNest();
    private final List<ShatterMark> shatterMarks = new ArrayList<>();
    private FrozenLedger frozen = new FrozenLedger();
    private FireClock clock = new FireClock(COOLDOWN_MAX);
    /** The charge freezes have fed the load in progress, held under its floor. */
    private double fed;
    /** Freezes toward the next Release. */
    private int freezes;
    private boolean releaseDue;
    private double ringRadians;
    private int tickNow;
    /** The tick its present quiet, or the quiet before its present fight, began; empty before its first tick. */
    private OptionalInt quietSince = OptionalInt.empty();
    /** The tick its present fight began; empty while it is quiet. */
    private OptionalInt fightingSince = OptionalInt.empty();

    public SeekerTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SEEKER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .with(TowerStat.FREEZE_DURATION, FREEZE_TICKS).with(TowerStat.NEST_CAPACITY, NEST_SIZE)
                .with(TowerStat.PROJECTILE_SPEED, 1f), context, x, y);
    }

    @Override
    protected OwnedMechanics<SeekerMechanic> ownedMechanics() {
        return this.mechanics;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /** Whom it may hunt and what its freezes, nest and ice do, as its owned mechanics make them. */
    private SeekerSpec spec() {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.cell());
        SeekerSpec spec = SeekerSpec.from(view);
        for (SeekerMechanic mechanic : this.mechanics.all()) {
            spec = mechanic.refineSpec(spec);
        }
        return spec;
    }

    private int cell() {
        return this.context.getBoard().scale();
    }

    private int capacity() {
        return Math.round(this.stats().value(TowerStat.NEST_CAPACITY));
    }

    public void doTick(int gameTime) {
        this.tickNow = gameTime;
        this.shatterMarks.removeIf(mark -> gameTime - mark.startedAtTick() >= SHATTER_MARK_TICKS);
        SeekerSpec spec = this.spec();
        this.settleIce(spec);
        this.nest.keepOnly(spec.brood()::owns);
        List<EnemyMob> inReach = spec.reach().matching(this.context.enemies());
        this.noteFight(!inReach.isEmpty(), gameTime);
        if (this.releaseDue) {
            this.releaseDue = false;
            this.release(spec, inReach);
        }
        if (this.nest.hatchesNow(ReleaseMechanic.HATCH_GAP_TICKS)) {
            this.nest.load(spec.brood().draw(this.context.random()));
        }
        if (this.clock.isReady() && this.nest.hasRoom(this.capacity())) {
            this.nest.load(spec.brood().draw(this.context.random()));
            this.clock.spend();
            this.fed = 0.0;
        }
        this.nest.tick();
        if (!inReach.isEmpty()) {
            this.ringRadians += RING_RADIANS_PER_TICK;
            if (this.nest.readyToLaunch()) {
                EnemyMob target = SOONEST_OUT.selectFrom(inReach).orElseThrow();
                this.launch(this.nest.launch(LAUNCH_GAP_TICKS), target, this.ringRadians);
            }
        }
        this.clock.advance(this.attackRate());
    }

    /** A fight begins when an enemy comes into its reach and ends when none is left there. */
    private void noteFight(boolean fighting, int gameTime) {
        if (this.quietSince.isEmpty()) {
            this.quietSince = OptionalInt.of(gameTime);
        }
        if (fighting && this.fightingSince.isEmpty()) {
            this.fightingSince = OptionalInt.of(gameTime);
        } else if (!fighting && this.fightingSince.isPresent()) {
            this.quietSince = OptionalInt.of(gameTime);
            this.fightingSince = OptionalInt.empty();
        }
    }

    /** A nest that waited with nothing in reach is full, so every wave starts with it full and the count at none. */
    @Override
    protected void endFight() {
        this.frozen = new FrozenLedger();
        this.shatterMarks.clear();
        this.clock = new FireClock(COOLDOWN_MAX);
        this.fed = 0.0;
        this.freezes = 0;
        this.releaseDue = false;
        this.ringRadians = 0.0;
        this.nest = new SeekerNest();
        BroodSpec brood = this.spec().brood();
        while (this.nest.hasRoom(this.capacity())) {
            this.nest.load(brood.draw(this.context.random()));
        }
    }

    /** A Swarm's missiles at once, at as many different enemies, soonest out first; a Hatching's into the nest. */
    private void release(SeekerSpec spec, List<EnemyMob> inReach) {
        switch (spec.release()) {
            case NONE -> {
            }
            case SWARM -> {
                List<EnemyMob> left = new ArrayList<>(inReach);
                for (int i = 0; i < ReleaseMechanic.MISSILES && !left.isEmpty(); i++) {
                    EnemyMob target = SOONEST_OUT.selectFrom(left).orElseThrow();
                    left.remove(target);
                    this.launch(spec.brood().draw(this.context.random()), target,
                            this.ringRadians + 2 * Math.PI * i / ReleaseMechanic.MISSILES);
                }
            }
            case HATCHING -> this.nest.hatch(ReleaseMechanic.MISSILES);
        }
    }

    /** What was launched: the kind of missile, and the stats it lands with however the tower changes meanwhile. */
    private record Shot(MissileKind kind, TowerStats stats) {
    }

    /** A missile of {@code kind} at {@code target}, leaving the ring at {@code slotRadians} along its orbit. */
    private void launch(MissileKind kind, EnemyMob target, double slotRadians) {
        Shot shot = new Shot(kind, this.beginShot());
        this.context.projectiles().add(new MissileProjectile(this.centerX, this.centerY, slotRadians + Math.PI / 2,
                target, this.missileStats(shot.stats(), kind), new Flight(shot, target)));
    }

    private ProjectileStats missileStats(TowerStats stats, MissileKind kind) {
        float speedScale = stats.value(TowerStat.PROJECTILE_SPEED);
        return ProjectileStats.of(MISSILE_SPEED * speedScale).withLook(kind.look()).withStreak(speedScale > 1f);
    }

    /**
     * A missile lands with the stats it was launched with: its kind's hit, and a freeze unless a Shield took the whole
     * of it. A dodged missile does nothing.
     */
    private void onImpact(EnemyMob target, Shot shot) {
        TowerStats stats = shot.stats();
        MissileKind kind = shot.kind();
        boolean wasFrozen = target.hasEffect(EffectKind.FREEZE);
        HitAdjustment adjustment = this.hitAdjustment(target, HitKind.MISSILE, 0f, stats);
        int amount = Math.round(stats.damage() * kind.damageFactor() * adjustment.damageFactor());
        HitResult hit = this.landHit(target, Damage.magic(amount), this.profileOf(shot, adjustment).asAimed());
        if (hit.dodged()) {
            return;
        }
        boolean shieldTookAll = hit.absorbed() > 0 && hit.landed().amount() == 0;
        if (!shieldTookAll && !target.isDead()) {
            int ticks = Math.round(stats.value(TowerStat.FREEZE_DURATION));
            this.applyEffect(target, sink -> Effect.freeze(ticks, kind.ice(), sink));
            if (!wasFrozen && target.hasEffect(EffectKind.FREEZE)) {
                this.frozen.record(target, kind, stats, this.tickNow);
                this.countFreeze();
            }
        }
        this.report(target, HitKind.MISSILE, hit.critical(), target.isDead());
    }

    private AttackProfile profileOf(Shot shot, HitAdjustment adjustment) {
        AttackProfile profile = adjustment.applyTo(shot.stats().attack());
        return shot.kind().shadow() ? profile.asShadow() : profile;
    }

    /** A freeze it landed: one more toward the next Release, and with Feeding a load that comes sooner. */
    private void countFreeze() {
        SeekerSpec spec = this.spec();
        if (spec.release() != Release.NONE && ++this.freezes >= ReleaseMechanic.EVERY) {
            this.freezes = 0;
            this.releaseDue = true;
        }
        if (spec.feeds()) {
            double fed = FeedingMechanic.feedFor(this.fed, this.clock.period(), this.attackRate());
            this.clock.hasten(fed);
            this.fed += fed;
        }
    }

    /** The freezes that ended: a Shatter bursts and a Shade's enemy stays Silenced; an Ice Wake chills round the rest. */
    private void settleIce(SeekerSpec spec) {
        for (FrozenLedger.Thaw thaw : this.frozen.settle(this.tickNow)) {
            FrozenLedger.Freeze freeze = thaw.freeze();
            if (spec.ice() == IceRider.SHATTER) {
                this.shatter(freeze);
            }
            if (freeze.kind().silencesAfterThaw() && !freeze.enemy().isDead()) {
                this.applyEffect(freeze.enemy(), sink -> Effect.silenced(thaw.heldTicks(), sink));
            }
        }
        if (spec.ice() == IceRider.ICE_WAKE) {
            float radius = IceWakeMechanic.RADIUS_CELLS * this.cell();
            for (EnemyMob held : this.frozen.holding()) {
                for (EnemyMob passing : this.around(held, radius)) {
                    this.applyEffect(passing, sink -> Effect.chill(IceWakeMechanic.CHILL, IceWakeMechanic.CHILL_TICKS,
                            sink));
                }
            }
        }
    }

    /** The ice bursts: every other enemy close by takes a share of the damage of the missile that froze it. */
    private void shatter(FrozenLedger.Freeze freeze) {
        Shot shot = new Shot(freeze.kind(), freeze.stats());
        Damage damage = Damage.magic(Math.round(freeze.stats().damage() * freeze.kind().damageFactor()
                * ShatterMechanic.SHARE));
        AttackProfile profile = this.profileOf(shot, HitAdjustment.none());
        float radius = ShatterMechanic.RADIUS_CELLS * this.cell();
        this.around(freeze.enemy(), radius).forEach(enemy -> this.dealDamage(enemy, damage, profile));
        this.shatterMarks.add(new ShatterMark(new Vec2(freeze.enemy().getX(), freeze.enemy().getY()), radius,
                this.tickNow, freeze.kind()));
    }

    /** Every enemy within {@code radius} pixels of {@code center}, hidden ones too, but itself. */
    private List<EnemyMob> around(EnemyMob center, float radius) {
        return InRangeTargetQuery.everyone((int) center.getX(), (int) center.getY(), radius)
                .matching(this.context.enemies()).stream()
                .filter(enemy -> enemy != center)
                .toList();
    }

    /** The missiles it holds ready, the next to leave first, each of its kind. */
    public List<MissileKind> nestMissiles() {
        return this.nest.missiles();
    }

    /** Where every enemy it holds frozen stands while it has an Ice Wake; none without one. */
    public List<Vec2> iceWakes() {
        if (this.spec().ice() != IceRider.ICE_WAKE) {
            return List.of();
        }
        return this.frozen.holding().stream().map(enemy -> new Vec2(enemy.getX(), enemy.getY())).toList();
    }

    /** How far an Ice Wake reaches round the ice, in pixels. */
    public float iceWakeRadius() {
        return IceWakeMechanic.RADIUS_CELLS * this.cell();
    }

    /** The ice that burst lately, for the shards the board draws. */
    public List<ShatterMark> shatterMarks() {
        return List.copyOf(this.shatterMarks);
    }

    /** Where the ring its nest sits on has turned to, in radians. */
    public double ringRadians() {
        return this.ringRadians;
    }

    /**
     * How long it has been quiet, or, while it fights, how long the quiet before this fight lasted; empty before its
     * first tick.
     */
    public OptionalInt ticksQuiet(int gameTime) {
        if (this.quietSince.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(this.fightingSince.orElse(gameTime) - this.quietSince.getAsInt());
    }

    /** How long its present fight has run, an enemy in its reach all along; empty while it is quiet. */
    public OptionalInt ticksFighting(int gameTime) {
        return this.fightingSince.isPresent() ? OptionalInt.of(gameTime - this.fightingSince.getAsInt())
                : OptionalInt.empty();
    }

    /** Whether the next freeze it lands sets off a Release. */
    public boolean releaseReady() {
        return this.spec().release() != Release.NONE && this.freezes == ReleaseMechanic.EVERY - 1;
    }

    /** The second kind of missile its nest draws, its identity, once it took one. */
    public Optional<MissileKind> secondMissile() {
        return this.spec().brood().second();
    }

    /** Its nest as the pane shows it. */
    public NestView nestView() {
        return new NestView(this.nest.missiles(), this.missileChances());
    }

    /** {@code tower}'s nest as the pane shows it, when it is an Aerie. */
    public static Optional<NestView> nestViewOf(Tower tower) {
        return tower.accept(AS_SEEKER).map(SeekerTower::nestView);
    }

    /** What share of the new missiles each kind comes to. */
    public Map<MissileKind, Float> missileChances() {
        return this.spec().brood().chances();
    }

    @Override
    protected DamageType damageType() {
        return DamageType.MAGIC;
    }

    @Override
    protected List<TowerStatLine> ownStats(TowerStats stats) {
        ProjectileStats missile = this.missileStats(stats, MissileKind.FROST);
        float cellsPerSecond = missile.speed() * TICKS_PER_SECOND / this.cell();
        return List.of(TowerStatLine.fixed(TowerStatRow.PROJECTILE_SPEED, TowerStat.PROJECTILE_SPEED, cellsPerSecond),
                TowerStatLine.fixed(TowerStatRow.PROJECTILE_SIZE, TowerStat.PROJECTILE_SIZE, missile.size()));
    }

    /** The nest's missiles, and the Release the next freeze sets off. */
    @Override
    protected List<TowerBuffState> ownConditions() {
        List<TowerBuffState> states = new ArrayList<>();
        if (this.nest.stored() > 0) {
            states.add(TowerBuffState.of(TowerTerm.NEST, TermText.of(this.nest.stored() + " of " + this.capacity()
                    + " missiles ready")).withStacks(this.nest.stored()));
        }
        if (this.releaseReady()) {
            TowerTerm release = this.spec().release() == Release.SWARM ? TowerTerm.SWARM : TowerTerm.HATCHING;
            states.add(TowerBuffState.of(release, TermText.of("the next ").plus(EffectKind.FREEZE)
                    .plus(" sets it off")));
        }
        return states;
    }

    /** How far the freezes are toward the next Release. */
    @Override
    protected List<NodeProgress> ownProgress() {
        return List.of(SWARM, HATCHING).stream()
                .flatMap(node -> this.countingNode(node.id(), this.freezes, ReleaseMechanic.EVERY).stream())
                .toList();
    }

    @Override
    protected List<AppliedEffect> baseApplies() {
        float seconds = this.stats().value(TowerStat.FREEZE_DURATION) / TickRate.TICKS_PER_SECOND;
        return List.of(new AppliedEffect(EffectKind.FREEZE, List.of(new AppliedEffect.Source(this.displayName(),
                TermText.of("every missile that hits applies ").plus(EffectKind.FREEZE)
                        .plus(" for " + TermNumbers.seconds(seconds) + ", unless a ").plus(EffectKind.SHIELD)
                        .plus(" takes the whole hit")))));
    }

    @Override
    protected String targetingLabel() {
        return "soonest out";
    }

    @Override
    protected String description() {
        return "Loads homing missiles into a nest that is full at every wave start.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSeekerTower(this);
    }

    /**
     * One missile's say in its own flight: it gives up an enemy frozen since it picked it for the soonest-out enemy
     * close by that isn't frozen, and stays on it when there is none. With no enemy left it flies home into the nest,
     * or fades when the nest is full.
     */
    private final class Flight implements MissileGuide {

        private final Shot shot;
        private EnemyMob pick;
        private boolean frozenAtPick;

        Flight(Shot shot, EnemyMob target) {
            this.shot = shot;
            this.pick = target;
            this.frozenAtPick = target.hasEffect(EffectKind.FREEZE);
        }

        @Override
        public boolean keeps(EnemyMob target) {
            return this.frozenAtPick || !target.hasEffect(EffectKind.FREEZE);
        }

        @Override
        public Optional<EnemyMob> retarget(double x, double y) {
            float radius = RETARGET_CELLS * SeekerTower.this.cell();
            List<EnemyMob> near = InRangeTargetQuery.visible((int) Math.round(x), (int) Math.round(y), radius)
                    .matching(SeekerTower.this.context.enemies()).stream()
                    .filter(enemy -> !enemy.hasEffect(EffectKind.FREEZE))
                    .toList();
            Optional<EnemyMob> next = SOONEST_OUT.selectFrom(near);
            if (next.isEmpty() && this.pick.validTarget()) {
                next = Optional.of(this.pick);
            }
            next.ifPresent(enemy -> {
                this.pick = enemy;
                this.frozenAtPick = enemy.hasEffect(EffectKind.FREEZE);
            });
            return next;
        }

        @Override
        public void land(EnemyMob target) {
            SeekerTower.this.onImpact(target, this.shot);
        }

        @Override
        public void arriveHome() {
            if (SeekerTower.this.nest.hasRoom(SeekerTower.this.capacity())) {
                SeekerTower.this.nest.load(this.shot.kind());
            }
        }
    }
}
