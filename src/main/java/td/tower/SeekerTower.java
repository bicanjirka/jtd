package td.tower;

import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.MissileProjectile;
import td.projectile.ProjectileStats;
import td.tower.buff.TowerBuff;
import td.tower.seeker.BroodPerk;
import td.tower.seeker.Impact;
import td.tower.seeker.NestGrowthPerk;
import td.tower.seeker.NestPerk;
import td.tower.seeker.OverTheHorizonPerk;
import td.tower.seeker.RearmPerk;
import td.tower.seeker.SalvoPlanner;
import td.tower.seeker.SecondMissilePerk;
import td.tower.seeker.SeekerActions;
import td.tower.seeker.SeekerNest;
import td.tower.seeker.SeekerPerk;
import td.tower.seeker.SeekerSpec;
import td.tower.seeker.ShatterburstPerk;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.OwnedPerks;
import td.tower.upgrade.PerkCatalogue;
import td.tower.upgrade.PurposeCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The hunter: a slow homing missile at the fastest visible enemy in range, which it never loses
 * once fired. On impact it deals magic damage and freezes whichever enemy it reached. A frozen
 * enemy has stopped, so the next missile hunts the next runner.
 * <p>
 * Attuned, the cooldown loads a missile into a nest instead of firing, and stored missiles launch
 * as a salvo whenever a target is in range; the nest fills between waves too.
 * <p>
 * What each owned node does lives in a {@link SeekerPerk}: the tower reads one spec from them
 * whenever it looks for a target.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SeekerTower extends AbstractTower {

    public static final int PRICE = 30;
    public static final float DAMAGE_POINTS = 40f;
    public static final float RANGE = 4.5f;
    /** Pixels a tick: slow enough to see a missile hunt. */
    public static final float MISSILE_SPEED = 8f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final int FREEZE_DURATION_TICKS_BASE = 30;
    private static final float DEEP_FREEZE_DURATION_MULTIPLIER = 1.75f;
    /** Share of weapon damage a shattered enemy's neighbours take. */
    private static final float SHATTER_DAMAGE_SHARE = 0.5f;
    private static final float SHATTER_RADIUS_CELLS = 1.5f;

    private static final String FREEZE_DEED = "Freezes";
    private static final int FREEZES_NEEDED = 25;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withAttune("Nest: the cooldown loads a missile into the nest (up to 3) instead of firing; with a "
                    + "target in range, stored missiles launch 4 ticks apart")
            .withRangeThree(0.1f, "Over the Horizon: may fire at Revealed or Marked enemies within 1.5x its range");

    private static final UpgradeNode TWIN_WARHEAD_1 = UpgradeTier.HEAD_1.node("seeker.head.twin_warhead.1",
            "Twin Warhead", PRICE)
            .withBuff(TowerBuff.fireRate(0.3f))
            .withExtraEffect("the nest holds one more missile");
    private static final UpgradeNode TWIN_WARHEAD_2 = UpgradeTier.HEAD_2.node("seeker.head.twin_warhead.2",
            "Twin Warhead II", PRICE)
            .withExtraEffect("two missiles a shot, the second at the next target")
            .after(TWIN_WARHEAD_1);
    private static final UpgradeNode BROOD = UpgradeTier.HEAD_3.node("seeker.head.twin_warhead.3", "Brood", PRICE)
            .withGate(new PurposeCondition(FREEZE_DEED, FREEZES_NEEDED))
            .withExtraEffect("the nest holds 6; a salvo spreads across different targets")
            .after(TWIN_WARHEAD_2);
    private static final UpgradeNode SHATTERBURST = UpgradeTier.HEAD_4.node("seeker.head.twin_warhead.4a",
            "Shatterburst", PRICE)
            .withExtraEffect("a missile that hits a frozen enemy bursts for 35% magic damage around it; every enemy "
                    + "the burst hits is Silenced 2s and gains Unraveled")
            .after(BROOD);
    private static final UpgradeNode REARM = UpgradeTier.HEAD_4.node("seeker.head.twin_warhead.4b", "Rearm", PRICE)
            .withExtraEffect("a missile that freezes its target launches a new missile from the tower at once; a "
                    + "rearmed missile's freeze rearms nothing")
            .after(BROOD);
    private static final UpgradeNode DEEP_FREEZE_1 = UpgradeTier.HEAD_1.node("seeker.head.deep_freeze.1",
            "Deep Freeze", PRICE)
            .withBuff(TowerBuff.damage(0.3f));
    private static final UpgradeNode DEEP_FREEZE_2 = UpgradeTier.HEAD_2.node("seeker.head.deep_freeze.2",
            "Deep Freeze II", PRICE)
            .withBuff(TowerBuff.damage(0.25f))
            .after(DEEP_FREEZE_1)
            .withExtraEffect("+75% freeze duration, killing a frozen enemy shatters it for 50% weapon damage splash");
    private static final UpgradeNode HOMING_CURSE = UpgradeTier.SPECIAL.node("seeker.special.homing_curse",
            "Homing Curse", PRICE)
            .withExtraEffect("impact applies 1 Vulnerable stack, or 2 if the target was already frozen or chilled");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS, BROOD))
            .with(TWIN_WARHEAD_1, TWIN_WARHEAD_2, BROOD, SHATTERBURST, REARM, DEEP_FREEZE_1, DEEP_FREEZE_2,
                    HOMING_CURSE)
            .withChoice(ExclusiveChoice.oneOf(TWIN_WARHEAD_1, DEEP_FREEZE_1))
            .withChoice(ExclusiveChoice.oneOf(SHATTERBURST, REARM));

    private static final PerkCatalogue<SeekerPerk> PERKS = PerkCatalogue.<SeekerPerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, NestPerk::new)
            .with(StandardBaseSlot.RANGE_3_ID, OverTheHorizonPerk::new)
            .with(TWIN_WARHEAD_1.id(), () -> new NestGrowthPerk(1))
            .with(TWIN_WARHEAD_2.id(), SecondMissilePerk::new)
            .with(BROOD.id(), BroodPerk::new)
            .with(SHATTERBURST.id(), ShatterburstPerk::new)
            .with(REARM.id(), RearmPerk::new);

    private static final int COOLDOWN_MAX = 45;

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final OwnedPerks<SeekerPerk> perks = new OwnedPerks<>(PERKS);
    private final SeekerNest nest = new SeekerNest();
    private final ProjectileStats missile = ProjectileStats.of(MISSILE_SPEED);
    private final SeekerActions actions = new Actions();
    private volatile int freezeDurationTicks = FREEZE_DURATION_TICKS_BASE;
    private int coolDown = 0;
    private EnemyMob currentTarget;

    public SeekerTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SEEKER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.perks.add(node);
        if (node.equals(DEEP_FREEZE_2)) {
            this.freezeDurationTicks = Math.round(this.freezeDurationTicks * DEEP_FREEZE_DURATION_MULTIPLIER);
        }
    }

    /** Whom it may fire at, how it picks and what its nest holds, as the perks in {@code owned} make it. */
    private SeekerSpec spec(List<SeekerPerk> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        SeekerSpec spec = SeekerSpec.from(view);
        for (SeekerPerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        return spec;
    }

    private EnemyMob findTarget(SeekerSpec spec) {
        return PreferringSelector.priority(spec.aim().selector())
                .selectFrom(spec.reach().matching(this.context.enemies())).orElse(null);
    }

    /**
     * One launch, as the spec shapes it: a missile at the pick among the enemies in reach, and a
     * second at the next one when it sends two. A spread salvo avoids whom it has already fired at.
     * Whether anything was launched; it is not when nothing is in reach.
     */
    private boolean launchShot(SeekerSpec spec) {
        List<EnemyMob> inReach = spec.reach().matching(this.context.enemies());
        Set<EnemyMob> avoided = new HashSet<>(spec.spreadsSalvo() ? this.nest.salvoTargets() : Set.of());
        for (int i = 0; i < spec.missilesPerShot(); i++) {
            Optional<EnemyMob> target = SalvoPlanner.pick(PreferringSelector.priority(spec.aim().selector()), inReach,
                    avoided);
            if (target.isEmpty()) {
                if (i == 0) {
                    this.currentTarget = null;
                }
                return i > 0;
            }
            if (i == 0) {
                this.currentTarget = target.get();
            }
            this.context.projectiles().add(this.newMissile(target.get(), false));
            if (spec.nest().isActive()) {
                this.nest.recordTarget(target.get());
            }
            avoided.add(target.get());
        }
        return true;
    }

    public void doTick(int gameTime) {
        SeekerSpec spec = this.spec(this.perks.all());
        if (spec.nest().isActive()) {
            this.tickNest(spec);
        } else {
            this.tickDirect(spec);
        }
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    /** No nest: a missile leaves as the cooldown ends, if there is anything to fire at. */
    private void tickDirect(SeekerSpec spec) {
        if (this.coolDown > 0) {
            this.coolDown--;
            return;
        }
        if (this.launchShot(spec)) {
            this.coolDown = this.coolDownCurrent();
        }
    }

    /**
     * The cooldown loads a missile whenever there is room, with or without a target; a stored
     * missile launches at the fastest enemy in reach once the gap since the last has passed.
     */
    private void tickNest(SeekerSpec spec) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else if (this.nest.hasRoom(spec.nest())) {
            this.nest.load();
            this.coolDown = this.coolDownCurrent();
        }
        this.nest.tick(spec.nest());
        if (this.nest.readyToLaunch() && this.launchShot(spec)) {
            this.nest.launch(spec.nest());
        }
    }

    /** A missile at {@code target}; a rearmed one's freeze rearms nothing. */
    private MissileProjectile newMissile(EnemyMob target, boolean rearmed) {
        return new MissileProjectile(this.centerX, this.centerY, target, this.context.enemies(), this.missile,
                hit -> this.onImpact(hit, rearmed));
    }

    private void onImpact(EnemyMob target, boolean rearmed) {
        Set<EffectKind> before = target.activeEffectKinds();
        boolean wasFrozen = before.contains(EffectKind.FREEZE);
        boolean controlled = wasFrozen || before.contains(EffectKind.CHILL);
        this.dealDamage(target, Damage.magic(this.damageCurrent()));
        this.applyEffect(target, sink -> Effect.freeze(this.freezeDurationTicks, sink));
        if (target.hasEffect(EffectKind.FREEZE)) {
            this.countDeedOfAttack();
        }
        if (this.upgrades().owns(HOMING_CURSE.id())) {
            this.applyStacks(target, EffectKind.VULNERABLE, controlled ? 2 : 1);
        }
        Impact impact = new Impact(target, wasFrozen, !wasFrozen && target.hasEffect(EffectKind.FREEZE), rearmed);
        for (SeekerPerk perk : this.perks.all()) {
            perk.react(impact, this.actions);
        }
    }

    /** Deep Freeze II: a frozen enemy this tower kills shatters, hurting whatever stands near it. */
    @Override
    protected void onKill(EnemyMob killed) {
        if (!this.upgrades().owns(DEEP_FREEZE_2.id()) || !killed.activeEffectKinds().contains(EffectKind.FREEZE)) {
            return;
        }
        float radius = SHATTER_RADIUS_CELLS * this.context.getBoard().scale();
        Damage shatter = Damage.magic(Math.round(this.damageCurrent() * SHATTER_DAMAGE_SHARE));
        for (EnemyMob nearby : InRangeTargetQuery.everyone((int) killed.getX(), (int) killed.getY(), radius)
                .matching(this.context.enemies())) {
            this.dealDamage(nearby, shatter);
        }
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    int getFreezeDurationTicks() {
        return this.freezeDurationTicks;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    /** How many missiles the nest holds now; none without Attune. */
    public int getNestStored() {
        return this.nest.stored();
    }

    @Override
    protected DamageType damageType() {
        return DamageType.MAGIC;
    }

    @Override
    protected List<TowerStatLine> ownStats() {
        float cellsPerSecond = this.missile.speed() * TICKS_PER_SECOND / this.context.getBoard().scale();
        return List.of(TowerStatLine.fixed(TowerStat.PROJECTILE_SPEED, cellsPerSecond),
                TowerStatLine.fixed(TowerStat.PROJECTILE_SIZE, this.missile.size()));
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        SeekerSpec spec = this.spec(this.perks.all());
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.FREEZE, "Freezes", BehaviourLine.seconds(this.freezeDurationTicks)));
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Targets", spec.aim().label()));
        if (spec.nest().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Nest", this.nest.stored() + "/" + spec.nest().capacity()));
        }
        if (this.upgrades().owns(HOMING_CURSE.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.VULNERABLE, "Impact applies", "1 vulnerable, 2 if chilled"));
        }
        return lines;
    }

    @Override
    protected String description() {
        return "Fires a homing missile.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSeekerTower(this);
    }

    /** What the perks may make this tower do. */
    private final class Actions implements SeekerActions {

        @Override
        public List<EnemyMob> burst(EnemyMob center, float share, float radiusCells) {
            float radius = radiusCells * SeekerTower.this.context.getBoard().scale();
            Damage damage = Damage.magic(Math.round(SeekerTower.this.damageCurrent() * share));
            List<EnemyMob> hit = InRangeTargetQuery.everyone((int) center.getX(), (int) center.getY(), radius)
                    .matching(SeekerTower.this.context.enemies()).stream()
                    .filter(enemy -> enemy != center)
                    .toList();
            hit.forEach(enemy -> SeekerTower.this.dealDamage(enemy, damage));
            return hit;
        }

        @Override
        public void silence(EnemyMob target, int ticks) {
            SeekerTower.this.applyEffect(target, sink -> Effect.silenced(ticks, sink));
        }

        @Override
        public void applyStacks(EnemyMob target, EffectKind kind, int stacks) {
            SeekerTower.this.applyStacks(target, kind, stacks);
        }

        @Override
        public void rearm() {
            EnemyMob target = SeekerTower.this.findTarget(SeekerTower.this.spec(SeekerTower.this.perks.all()));
            if (target != null) {
                SeekerTower.this.context.projectiles().add(SeekerTower.this.newMissile(target, true));
            }
        }
    }
}
