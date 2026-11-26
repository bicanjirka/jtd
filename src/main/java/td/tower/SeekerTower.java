package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.IceKind;
import td.effect.StackingDebuff;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.projectile.MissileGuide;
import td.projectile.MissileProjectile;
import td.projectile.ProjectileStats;
import td.stat.StatBreakdown;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitKind;
import td.tower.proc.Proc;
import td.tower.proc.ProcCondition;
import td.tower.proc.ProcPayload;
import td.tower.seeker.FeedingMechanic;
import td.tower.seeker.FrozenLedger;
import td.tower.seeker.IceRider;
import td.tower.seeker.IceWakeMechanic;
import td.tower.seeker.MissileKind;
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
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.RangeNodes;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.util.TickRate;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

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

    public static final int PRICE = 20;
    public static final float DAMAGE_POINTS = 7f;
    public static final float RANGE = 4f;
    /** Cells a second at its top speed: slow enough to see a missile hunt; a faster one streaks. */
    public static final float MISSILE_SPEED = 3.75f;
    /** The ring the nest's missiles sit on turns this far a tick while the tower fights. */
    public static final double RING_RADIANS_PER_TICK = 0.08;

    /** A load every 2 s. */
    private static final int COOLDOWN_MAX = 39;
    private static final float FREEZE_TICKS = 20f;
    private static final float NEST_SIZE = 3f;
    private static final int LAUNCH_GAP_TICKS = 4;
    /** How far around itself a missile that lost its enemy looks for another. */
    private static final float HUNT_CELLS = 1f;
    private static final TargetSelector SOONEST_OUT = PreferringSelector.priority(new SoonestOutSelector());

    private static final UpgradeNode DEEP_FREEZE = UpgradeNode.of("seeker.deep_freeze", "Deep Freeze", 5)
            .withModifiers(TowerModifiers.freezeDuration(20));
    private static final UpgradeNode WIDE_COMB = UpgradeNode.of("seeker.wide_comb", "Wide Comb", 5)
            .withModifiers(TowerModifiers.nestCapacity(2));

    private static final UpgradeNode VOLLEY = UpgradeNode.of("seeker.volley", "Volley", 10)
            .inTier(1);
    private static final UpgradeNode HATCHING = UpgradeNode.of("seeker.hatching", "Hatching", 10)
            .inTier(1);
    private static final UpgradeNode SHARP_STING = UpgradeNode.of("seeker.sharp_sting", "Sharp Sting", 10)
            .withModifiers(TowerModifiers.damage(5f))
            .inTier(1);
    private static final UpgradeNode WIDE_COMB_2 = UpgradeNode.of("seeker.wide_comb_2", "Wide Comb II", 5)
            .withModifiers(TowerModifiers.nestCapacity(2))
            .inTier(1);

    private static final UpgradeNode RUBY = UpgradeNode.of("seeker.ruby", "Ruby", 15)
            .withModifiers(TowerModifiers.secondMissileDamage(20f))
            .inTier(2);
    private static final UpgradeNode SHADE = UpgradeNode.of("seeker.shade", "Shade", 15)
            .withModifiers(TowerModifiers.secondMissileDamage(20f))
            .inTier(2);
    private static final UpgradeNode FEEDING = UpgradeNode.of("seeker.feeding", "Feeding", 10)
            .inTier(2);
    private static final Proc RIME = Proc.onHit(new ProcPayload.ApplyStacks(StackingDebuff.UNRAVELED, 1))
            .when(new ProcCondition.TargetHasAnyOf(Set.of(EffectKind.FREEZE)));
    private static final UpgradeNode HIGH_PERCH = UpgradeNode.of(RangeNodes.RANGE_2_ID, "High Perch", 5)
            .withModifiers(TowerModifiers.range(1f).plus(TowerModifiers.huntRadius(1f)))
            .inTier(2);

    private static final UpgradeNode ICE_WAKE = UpgradeNode.of("seeker.ice_wake", "Ice Wake", 15)
            .inTier(3);
    private static final UpgradeNode SHATTER = UpgradeNode.of("seeker.shatter", "Shatter", 15)
            .withModifiers(TowerModifiers.shatterDamage(8f))
            .inTier(3);
    private static final UpgradeNode RICH_BROOD = UpgradeNode.of("seeker.rich_brood", "Rich Brood", 10)
            .withRequires(UpgradeCondition.owns(RUBY.id()).or(UpgradeCondition.owns(SHADE.id())))
            .inTier(3);
    private static final UpgradeNode DEEP_FREEZE_2 = UpgradeNode.of("seeker.deep_freeze_2", "Deep Freeze II", 5)
            .withModifiers(TowerModifiers.freezeDuration(40))
            .inTier(2);
    private static final UpgradeNode QUICK_BROOD = UpgradeNode.of("seeker.quick_brood", "Quick Brood", 55)
            .withModifiers(TowerModifiers.attackSpeed(0.55f))
            .inTier(3);
    private static final UpgradeNode SHARP_STING_2 = UpgradeNode.of("seeker.sharp_sting_2", "Sharp Sting II", 55)
            .withModifiers(TowerModifiers.damage(30f))
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(DEEP_FREEZE, WIDE_COMB))
            .with(VOLLEY, HATCHING, SHARP_STING, WIDE_COMB_2,
                    RUBY, SHADE, HIGH_PERCH, FEEDING,
                    ICE_WAKE, SHATTER, RICH_BROOD, DEEP_FREEZE_2, QUICK_BROOD, SHARP_STING_2)
            .withCluster(UpgradeCluster.of("Release", VOLLEY, HATCHING))
            .withCluster(UpgradeCluster.of("Second missile", RUBY, SHADE))
            .withCluster(UpgradeCluster.of("Around the ice", ICE_WAKE, SHATTER))
            .withCluster(UpgradeCluster.of("Apex", QUICK_BROOD, SHARP_STING_2))
            .withTiers(List.of(UpgradeTier.open(), UpgradeTier.gated(60, 0).withBonus(TowerModifiers.huntRadius(2f).plus(TowerModifiers.projectileSpeed(3.75f))),
                    UpgradeTier.gated(120, 0).withPerk("Rime", RIME),
                    UpgradeTier.gated(200, 0).withBonus(TowerModifiers.accuracy(100f))));

    private static final MechanicCatalogue<SeekerMechanic> MECHANICS = MechanicCatalogue.of(
            entry(VOLLEY.id(), () -> new ReleaseMechanic(Release.VOLLEY)),
            entry(HATCHING.id(), () -> new ReleaseMechanic(Release.HATCHING)),
            entry(FEEDING.id(), FeedingMechanic::new),
            entry(RUBY.id(), () -> new SecondMissileMechanic(MissileKind.RUBY)),
            entry(SHADE.id(), () -> new SecondMissileMechanic(MissileKind.SHADE)),
            entry(RICH_BROOD.id(), RichBroodMechanic::new),
            entry(ICE_WAKE.id(), IceWakeMechanic::new),
            entry(SHATTER.id(), ShatterMechanic::new));

    private final OwnedMechanics<SeekerMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private SeekerNest nest = new SeekerNest();
    private final List<ShatterMark> shatterMarks = new ArrayList<>();
    private FrozenLedger frozen = new FrozenLedger();
    private FireClock clock = new FireClock(COOLDOWN_MAX);
    /** The missiles it made this wave, which sets the kind of the next. */
    private int made;
    /** Freezes toward the next Release. */
    private int freezes;
    private boolean releaseDue;
    private double ringRadians;
    /** The tick its present quiet, or the quiet before its present fight, began; empty before its first tick. */
    private OptionalInt quietSince = OptionalInt.empty();
    /** The tick its present fight began; empty while it is quiet. */
    private OptionalInt fightingSince = OptionalInt.empty();

    public SeekerTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SEEKER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .with(TowerStat.FREEZE_DURATION, FREEZE_TICKS).with(TowerStat.NEST_CAPACITY, NEST_SIZE)
                .with(TowerStat.PROJECTILE_SPEED, MISSILE_SPEED).with(TowerStat.HUNT_RADIUS, HUNT_CELLS)
                .with(TowerStat.SECOND_MISSILE_DAMAGE, 0f).with(TowerStat.SHATTER_DAMAGE, 0f), context, x, y);
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
        SeekerSpec spec = SeekerSpec.from(this.viewpoint());
        for (SeekerMechanic mechanic : this.mechanics.all()) {
            spec = mechanic.refineSpec(spec);
        }
        return spec;
    }

    private int capacity() {
        return Math.round(this.stats().value(TowerStat.NEST_CAPACITY));
    }

    @Override
    protected void act(int gameTime) {
        this.shatterMarks.removeIf(mark -> MomentMemory.forgets(mark.startedAtTick(), gameTime));
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
            this.nest.loadHatched(this.nextMissile(spec));
        }
        if (this.clock.isReady() && this.nest.hasRoom(this.capacity())) {
            this.nest.load(this.nextMissile(spec));
            this.clock.spend();
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
        this.freezes = 0;
        this.releaseDue = false;
        this.ringRadians = 0.0;
        this.made = 0;
        this.nest = new SeekerNest();
        SeekerSpec spec = this.spec();
        while (this.nest.hasRoom(this.capacity())) {
            this.nest.load(this.nextMissile(spec));
        }
    }

    /** The kind of the next missile it makes, in the order its brood makes them. */
    private MissileKind nextMissile(SeekerSpec spec) {
        return spec.brood().kindOf(++this.made);
    }

    /**
     * As many missiles as the nest holds: a Volley's at once, one at each enemy in reach, soonest out first, and round
     * again while missiles are left; a Hatching's on top of the nest.
     */
    private void release(SeekerSpec spec, List<EnemyMob> inReach) {
        int missiles = this.capacity();
        switch (spec.release()) {
            case NONE -> {
            }
            case VOLLEY -> {
                List<EnemyMob> left = new ArrayList<>();
                for (int i = 0; i < missiles && !inReach.isEmpty(); i++) {
                    if (left.isEmpty()) {
                        left.addAll(inReach);
                    }
                    EnemyMob target = SOONEST_OUT.selectFrom(left).orElseThrow();
                    left.remove(target);
                    this.launch(this.nextMissile(spec), target, this.ringRadians + 2 * Math.PI * i / missiles);
                }
            }
            case HATCHING -> this.nest.hatch(missiles);
        }
    }

    /** What was launched: the kind of missile, and the stats it lands with however the tower changes meanwhile. */
    private record Shot(MissileKind kind, TowerStats stats) {
    }

    /** A missile of {@code kind} at {@code target}, leaving the ring at {@code slotRadians} along its orbit. */
    private void launch(MissileKind kind, EnemyMob target, double slotRadians) {
        Shot shot = new Shot(kind, this.beginShot());
        this.context.projectiles().add(new MissileProjectile(this.centerX, this.centerY, slotRadians + Math.PI / 2,
                target, this.missileStats(shot.stats(), kind), new Guide(shot, target)));
    }

    private ProjectileStats missileStats(TowerStats stats, MissileKind kind) {
        float speed = stats.value(TowerStat.PROJECTILE_SPEED);
        return ProjectileStats.of(this.pixelsPerTick(speed)).withLook(kind.look()).withStreak(speed > MISSILE_SPEED);
    }

    /**
     * A missile lands with the stats it was launched with: its kind's hit, and a freeze unless a Shield took the whole
     * of it. A dodged missile does nothing.
     */
    private void onImpact(EnemyMob target, Shot shot) {
        TowerStats stats = shot.stats();
        MissileKind kind = shot.kind();
        Optional<IceKind> iceBefore = iceOn(target);
        HitAdjustment adjustment = this.hitAdjustment(target, HitKind.MISSILE, 0f, stats);
        int amount = Math.round(kind.damageWith(stats) * adjustment.damageFactor());
        HitResult hit = this.landHit(target, Damage.magic(amount), this.profileOf(shot, adjustment).asAimed());
        if (hit.dodged()) {
            return;
        }
        boolean shieldTookAll = hit.absorbed() > 0 && hit.landed().amount() == 0;
        if (!shieldTookAll && !target.isDead()) {
            int ticks = Math.round(stats.value(TowerStat.FREEZE_DURATION));
            this.applyEffect(target, sink -> Effect.freeze(ticks, kind.ice(), sink));
            if (kind.withers()) {
                this.applyEffect(target, sink -> Effect.withered(ticks, sink));
            }
            Optional<IceKind> iceAfter = iceOn(target);
            if (iceBefore.isEmpty() && iceAfter.isPresent()) {
                this.frozen.record(target, kind, stats);
                this.countFreeze();
            } else if (iceBefore.filter(before -> before != kind.ice()).isPresent()
                    && iceAfter.filter(kind.ice()::equals).isPresent()) {
                this.frozen.retake(target, kind, stats);
            }
        }
        this.report(target, HitKind.MISSILE, hit.critical(), target.isDead());
    }

    /** The ice {@code enemy} is frozen in, if it is frozen. */
    private static Optional<IceKind> iceOn(EnemyMob enemy) {
        return enemy.activeEffect(EffectKind.FREEZE).map(Effect::ice);
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
            this.clock.hasten(FeedingMechanic.feedFor(this.attackRate()));
        }
    }

    /** The freezes that ended: a Shatter bursts; an Ice Wake chills round the rest. */
    private void settleIce(SeekerSpec spec) {
        for (FrozenLedger.Freeze freeze : this.frozen.settle()) {
            if (spec.ice() == IceRider.SHATTER) {
                this.shatter(freeze);
            }
        }
        if (spec.ice() == IceRider.ICE_WAKE) {
            float radius = IceWakeMechanic.RADIUS_CELLS * this.cellPixels();
            for (EnemyMob held : this.frozen.holding()) {
                for (EnemyMob passing : InRangeTargetQuery.around(held, radius).matching(this.context.enemies())) {
                    this.applyEffect(passing, sink -> Effect.chill(IceWakeMechanic.CHILL, IceWakeMechanic.CHILL_TICKS,
                            sink));
                }
            }
        }
    }

    /** The ice bursts: every other enemy close by takes the Shatter's own damage, as the missile that froze it. */
    private void shatter(FrozenLedger.Freeze freeze) {
        Shot shot = new Shot(freeze.kind(), freeze.stats());
        Damage damage = Damage.magic(Math.round(freeze.stats().value(TowerStat.SHATTER_DAMAGE)));
        AttackProfile profile = this.profileOf(shot, HitAdjustment.none());
        float radius = ShatterMechanic.RADIUS_CELLS * this.cellPixels();
        InRangeTargetQuery.around(freeze.enemy(), radius).matching(this.context.enemies())
                .forEach(enemy -> this.dealDamage(enemy, damage, profile));
        this.shatterMarks.add(new ShatterMark(freeze.enemy().position(), radius,
                this.tickNow(), freeze.kind()));
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
        return this.frozen.holding().stream().map(EnemyMob::position).toList();
    }

    /** How far an Ice Wake reaches round the ice, in pixels. */
    public float iceWakeRadius() {
        return IceWakeMechanic.RADIUS_CELLS * this.cellPixels();
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
        return this.quietSince.stream().map(since -> this.fightingSince.orElse(gameTime) - since).findFirst();
    }

    /** How long its present fight has run, an enemy in its reach all along; empty while it is quiet. */
    public OptionalInt ticksFighting(int gameTime) {
        return this.fightingSince.stream().map(since -> gameTime - since).findFirst();
    }

    /** Whether the next freeze it lands sets off a Release. */
    public boolean releaseReady() {
        return this.spec().release() != Release.NONE && this.freezes == ReleaseMechanic.EVERY - 1;
    }

    /** The second kind of missile its nest draws, its identity, once it took one. */
    public Optional<MissileKind> secondMissile() {
        return this.spec().brood().second();
    }

    @Override
    protected DamageType damageType() {
        return DamageType.MAGIC;
    }

    /** How a missile flies is on its own popup in the nest, not on the tower's sheet. */
    @Override
    protected boolean listsRow(TowerStat stat) {
        return switch (stat) {
            case PROJECTILE_SPEED, PROJECTILE_SIZE, HUNT_RADIUS -> false;
            default -> true;
        };
    }

    /** How its missiles fly now: their speed, size and how far round they look for a new enemy. */
    private List<StatBreakdown> missileRows() {
        TowerStats stats = this.stats();
        return List.of(StatBreakdown.unchanged(TowerStatRow.PROJECTILE_SPEED, stats.value(TowerStat.PROJECTILE_SPEED)),
                StatBreakdown.unchanged(TowerStatRow.PROJECTILE_SIZE, this.missileStats(stats, MissileKind.FROST).size()),
                this.liveBreakdown(TowerStat.HUNT_RADIUS));
    }

    /** Each kind of missile the nest holds, with how many, and the Release the next freeze sets off. */
    @Override
    protected List<TowerBuffState> ownConditions() {
        Map<MissileKind, Integer> held = new EnumMap<>(MissileKind.class);
        this.nest.missiles().forEach(kind -> held.merge(kind, 1, Integer::sum));
        List<TowerBuffState> states = new ArrayList<>();
        held.forEach((kind, count) -> states.add(TowerBuffState.of(kind.term(), TermText.of(count + " ready in the ")
                .plus(TowerTerm.NEST)).withStacks(count).withStats(this.missileRows())));
        if (this.releaseReady()) {
            TowerTerm release = this.spec().release() == Release.VOLLEY ? TowerTerm.VOLLEY : TowerTerm.HATCHING;
            states.add(TowerBuffState.of(release, TermText.of("the next ").plus(EffectKind.FREEZE)
                    .plus(" sets it off")));
        }
        return states;
    }

    /** How far the freezes are toward the next Release. */
    @Override
    protected List<NodeProgress> ownProgress() {
        return List.of(VOLLEY, HATCHING).stream()
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
        return "Loads homing missiles into a nest that is full at every wave start, and launches them at the enemy that would leave soonest.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSeekerTower(this);
    }

    /**
     * One missile's say in its own flight: it gives up an enemy frozen since it picked it for the soonest-out enemy
     * close by that isn't frozen, and stays on it when there is none. With no enemy left it flies home into the nest,
     * or fades when the nest is full.
     */
    private final class Guide implements MissileGuide {

        private final Shot shot;
        private EnemyMob pick;
        private boolean frozenAtPick;

        Guide(Shot shot, EnemyMob target) {
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
            float radius = this.shot.stats().value(TowerStat.HUNT_RADIUS) * SeekerTower.this.cellPixels();
            List<EnemyMob> near = InRangeTargetQuery.visible((int) Math.round(x), (int) Math.round(y), radius)
                    .matching(SeekerTower.this.context.enemies()).stream()
                    .filter(enemy -> !enemy.hasEffect(EffectKind.FREEZE))
                    .toList();
            Optional<EnemyMob> next = SOONEST_OUT.selectFrom(near)
                    .or(() -> Optional.of(this.pick).filter(EnemyMob::validTarget));
            next.ifPresent(enemy -> {
                this.pick = enemy;
                this.frozenAtPick = enemy.hasEffect(EffectKind.FREEZE);
            });
            return next;
        }

        @Override
        public void land(EnemyMob target, int gameTime) {
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
