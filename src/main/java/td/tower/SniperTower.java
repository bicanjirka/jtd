package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.sniper.AimFocus;
import td.tower.sniper.AimLock;
import td.tower.sniper.ExecutionerPerk;
import td.tower.sniper.FifthShotPerk;
import td.tower.sniper.FrenzyPerk;
import td.tower.sniper.HollowPointPerk;
import td.tower.sniper.MomentumPerk;
import td.tower.sniper.QuickScopePerk;
import td.tower.sniper.RailgunPerk;
import td.tower.sniper.ShotActions;
import td.tower.sniper.ShotContext;
import td.tower.sniper.ShotResult;
import td.tower.sniper.SniperPerk;
import td.tower.sniper.SniperShot;
import td.tower.sniper.SniperTempo;
import td.tower.sniper.SteadyAimPerk;
import td.tower.sniper.SteadyTempoPerk;
import td.tower.sniper.WeakSpotPerk;
import td.tower.targeting.BeyondRadiusTargetQuery;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.OnSegmentTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.TargetQuery;
import td.tower.targeting.TargetSelector;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.PurposeCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The patient single-target tower: it gets better the longer it stays on one enemy. It fires at the
 * visible enemy in range furthest along the path; any {@code SPECIAL} upgrade switches it to the
 * enemy with the most health. The turret turns at a capped rate and holds its heading when idle.
 * <p>
 * What each owned node does lives in a {@link SniperPerk}: the tower shapes every shot through them
 * and lets them react to how it landed.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SniperTower extends AbstractTower {

    public static final int PRICE = 15;
    public static final float DAMAGE_POINTS = 40f;
    public static final float RANGE = 4.0f;
    public static final float CRIT_CHANCE = 0.05f;
    public static final float CRIT_MULTIPLIER = 2.0f;
    private static final double MAX_TURN_RADIANS_PER_TICK = 0.4;
    /** A shot every 2.5 s. */
    private static final int COOLDOWN_MAX = 49;

    /** Overwatch: it can't shoot at what is this close, however far its range grows. */
    private static final float DEAD_ZONE_CELLS = 2f;
    private static final float OVERWATCH_RANGE_BONUS = 0.35f;
    /** Railgun reaches this far past the Sniper's range, and every enemy it pierces after the first takes less. */
    private static final float RAILGUN_EXTRA_REACH_CELLS = 2f;
    private static final float RAILGUN_FALLOFF = 0.75f;
    private static final float RAILGUN_HALF_WIDTH_CELLS = 0.35f;
    /** Enough to kill anything, so that an execution takes exactly what health is left. */
    private static final int EXECUTION_DAMAGE = 1_000_000_000;
    private static final float MOMENTUM_BURST_SECONDS = 5f;
    private static final int STEADY_AIM_STACKS = 1;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withAttune("Steady Aim: +10% crit chance on every shot after the first at one target")
            .withRangeThree(OVERWATCH_RANGE_BONUS, "Overwatch: can't shoot within 2 cells");

    private static final UpgradeNode FOCUSED_OPTICS_1 = UpgradeTier.HEAD_1.node("sniper.head.focused_optics.1",
            "Focused Optics", PRICE)
            .withExtraEffect("Steady Aim also gives +25% fire rate; Quick Scope: the first shot at a new target has "
                    + "+50% crit chance");
    private static final UpgradeNode FOCUSED_OPTICS_2 = UpgradeTier.HEAD_2.node("sniper.head.focused_optics.2",
            "Focused Optics II", PRICE)
            .withBuff(TowerBuff.damage(0.4f))
            .withExtraEffect("a crit starts Frenzy: the next 3 shots come twice as fast")
            .after(FOCUSED_OPTICS_1);
    private static final UpgradeNode FOCUSED_OPTICS_3 = UpgradeTier.HEAD_3.node("sniper.head.focused_optics.3",
            "Focused Optics III", PRICE)
            .withBuff(TowerBuff.damage(0.3f))
            .withGate(new PurposeCondition("Steady Aim shots", 20))
            .withExtraEffect("Weak Spot: non-crit shots ignore plating and 50 armor")
            .after(FOCUSED_OPTICS_2);
    private static final UpgradeNode RAILGUN = UpgradeTier.HEAD_4.node("sniper.head.focused_optics.4a", "Railgun",
            PRICE)
            .withExtraEffect("the shot pierces every enemy on the line through its target, 2 cells past max "
                    + "range; each one after the first takes 25% less")
            .after(FOCUSED_OPTICS_3);
    private static final UpgradeNode EXECUTIONER = UpgradeTier.HEAD_4.node("sniper.head.focused_optics.4b",
            "Executioner", PRICE)
            .withExtraEffect("a non-boss enemy left under 15% health by a shot dies; a boss under 25% takes +50%; "
                    + "executions count as crits")
            .after(FOCUSED_OPTICS_3);
    private static final UpgradeNode MARKSMANS_EYE_1 = UpgradeTier.HEAD_1.node("sniper.head.marksmans_eye.1",
            "Marksman's Eye", PRICE)
            .withBuff(TowerBuff.critChance(0.15f));
    private static final UpgradeNode MARKSMANS_EYE_2 = UpgradeTier.HEAD_2.node("sniper.head.marksmans_eye.2",
            "Marksman's Eye II", PRICE)
            .withBuff(TowerBuff.critChance(0.2f).withArmorPenetration(0.5f))
            .after(MARKSMANS_EYE_1);
    private static final UpgradeNode HOLLOW_POINT = UpgradeTier.SPECIAL.node("sniper.special.hollow_point",
            "Hollow Point", PRICE)
            .withExtraEffect("crits apply Vulnerable, +15% damage taken, stacks x3");
    private static final UpgradeNode FIFTH_SHOT = UpgradeTier.SPECIAL.node("sniper.special.fifth_shot",
            "Fifth Shot", PRICE)
            .withExtraEffect("every 5th shot is a guaranteed crit, and its crits deal 250%");
    private static final UpgradeNode MOMENTUM = UpgradeTier.SPECIAL.node("sniper.special.momentum", "Momentum", PRICE)
            .withExtraEffect("post-crit shot deals 500% and ignores armor and plating; a kill grants +100% fire "
                    + "rate for 5s");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS, FOCUSED_OPTICS_3))
            .with(FOCUSED_OPTICS_1, FOCUSED_OPTICS_2, FOCUSED_OPTICS_3, RAILGUN, EXECUTIONER, MARKSMANS_EYE_1,
                    MARKSMANS_EYE_2, HOLLOW_POINT, FIFTH_SHOT, MOMENTUM)
            .withChoice(ExclusiveChoice.oneOf(FOCUSED_OPTICS_1, MARKSMANS_EYE_1))
            .withChoice(ExclusiveChoice.oneOf(RAILGUN, EXECUTIONER))
            .withChoice(ExclusiveChoice.specials(HOLLOW_POINT, FIFTH_SHOT, MOMENTUM));

    /** The perk each node brings, by node id: a new one for every tower, as some carry state. */
    private static final Map<String, Supplier<SniperPerk>> PERKS = Map.of(
            StandardBaseSlot.ATTUNE_ID, SteadyAimPerk::new,
            FOCUSED_OPTICS_1.id(), () -> new Both(new QuickScopePerk(), new SteadyTempoPerk()),
            FOCUSED_OPTICS_2.id(), FrenzyPerk::new,
            FOCUSED_OPTICS_3.id(), WeakSpotPerk::new,
            RAILGUN.id(), RailgunPerk::new,
            EXECUTIONER.id(), ExecutionerPerk::new,
            HOLLOW_POINT.id(), HollowPointPerk::new,
            FIFTH_SHOT.id(), FifthShotPerk::new,
            MOMENTUM.id(), MomentumPerk::new);

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final AimFocus focus = new AimFocus();
    private final SniperTempo tempo = new SniperTempo();
    private final ShotActions actions = new Actions();
    private volatile List<SniperPerk> perks = List.of();
    private volatile TargetSelector targetSelector = PreferringSelector.priority(new FurthestAlongPathSelector());
    private int coolDown = 0;
    private EnemyMob currentTarget;
    private boolean lastShotCritical;
    private int shotsFired;
    private int currentTick;

    public SniperTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SNIPER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .withCritChance(CRIT_CHANCE).withCritMultiplier(CRIT_MULTIPLIER), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /**
     * A {@code SPECIAL} tower's pricier shots go to the enemy with the most health left, not to
     * finishing off the nearly dead.
     */
    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        Supplier<SniperPerk> perk = PERKS.get(node.id());
        if (perk != null) {
            List<SniperPerk> next = new ArrayList<>(this.perks);
            next.add(perk.get());
            this.perks = List.copyOf(next);
        }
        if (node.slot() == UpgradeSlot.SPECIAL) {
            this.targetSelector = PreferringSelector.priority(new HighestHealthSelector());
        }
    }

    private EnemyMob findEnemy() {
        TargetQuery query = InRangeTargetQuery.visible(this.centerX, this.centerY, this.rangeReal());
        if (this.upgrades().owns(StandardBaseSlot.RANGE_3_ID)) {
            float deadZone = DEAD_ZONE_CELLS * this.context.getBoard().scale();
            query = query.and(new BeyondRadiusTargetQuery(this.centerX, this.centerY, deadZone));
        }
        return this.targetSelector.selectFrom(query.matching(this.context.enemies())).orElse(null);
    }

    public void doTick(int gameTime) {
        this.currentTick = gameTime;
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            this.currentTarget = this.findEnemy();
            if (this.currentTarget != null) {
                this.fire(this.currentTarget);
            }
        }
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    /** One shot: shaped by the owned perks, landed, settled and reacted to, and the wait that follows. */
    private void fire(EnemyMob target) {
        this.shotsFired++;
        AimLock lock = this.focus.lock(target, STEADY_AIM_STACKS, false);
        ShotContext context = new ShotContext(target, lock, this.shotsFired);
        List<SniperPerk> owned = this.perks;
        SniperShot shot = SniperShot.of(this.stats().attack());
        for (SniperPerk perk : owned) {
            shot = perk.shape(shot, context);
        }
        boolean critical = shot.piercing() ? this.pierce(shot, target) : this.land(shot, target, 1f);
        ShotResult result = new ShotResult(target, critical, target.isDead());
        for (SniperPerk perk : owned) {
            result = perk.settle(result, this.actions);
        }
        for (SniperPerk perk : owned) {
            perk.react(result, this.actions);
        }
        this.lastShotCritical = result.critical();
        if (lock.stacks() > 0) {
            this.countDeedOfAttack();
        }
        float fireRate = shot.fireRateBonus();
        float tempoBonus = this.tempo.takeFireRateBonus(this.currentTick);
        this.coolDown = TowerBuff.fireRate(fireRate).combine(TowerBuff.fireRate(tempoBonus))
                .fireRateFor(this.coolDownCurrent());
    }

    /** Lands {@code shot} on {@code target} at {@code share} of its damage; whether it crit. */
    private boolean land(SniperShot shot, EnemyMob target, float share) {
        int amount = Math.round(this.damageCurrent() * shot.damageFactor() * share);
        Damage damage = shot.type() == DamageType.MAGIC ? Damage.magic(amount) : Damage.physical(amount);
        return this.dealDamage(target, damage, shot.attack());
    }

    /**
     * A shot through everything on the line from the Sniper past {@code target}; each enemy after
     * the first it passes takes less. Whether the aimed enemy's hit crit.
     */
    private boolean pierce(SniperShot shot, EnemyMob target) {
        float scale = this.context.getBoard().scale();
        double heading = TurretAim.angleTo(this.centerX, this.centerY, target.getX(), target.getY());
        double reach = this.rangeReal() + RAILGUN_EXTRA_REACH_CELLS * scale;
        List<EnemyMob> line = new OnSegmentTargetQuery(this.centerX, this.centerY,
                this.centerX + Math.cos(heading) * reach, this.centerY + Math.sin(heading) * reach,
                RAILGUN_HALF_WIDTH_CELLS * scale).matching(this.context.enemies());
        boolean critical = false;
        float share = 1f;
        for (EnemyMob enemy : line) {
            boolean crit = this.land(shot, enemy, share);
            critical |= enemy == target && crit;
            share *= RAILGUN_FALLOFF;
        }
        return critical;
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    public boolean wasLastShotCritical() {
        return this.lastShotCritical;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public float getCoolDownFraction() {
        return (float) this.coolDown / this.coolDownCurrent();
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        boolean special = this.upgrades().countIn(UpgradeSlot.SPECIAL) > 0;
        BehaviourLine targets = new BehaviourLine(BehaviourMarker.TARGETING, "Targets", special ? "most health" : "first");
        if (!this.upgrades().owns(HOLLOW_POINT.id())) {
            return List.of(targets);
        }
        return List.of(targets, new BehaviourLine(BehaviourMarker.VULNERABLE, "Crits apply", "vulnerable"));
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSniperTower(this);
    }

    /** What the perks may make this tower do. */
    private final class Actions implements ShotActions {

        @Override
        public void startFrenzy() {
            SniperTower.this.tempo.startFrenzy(SniperTower.this.currentTick);
        }

        @Override
        public void startBurst() {
            SniperTower.this.tempo.startBurst(
                    SniperTower.this.currentTick + Math.round(MOMENTUM_BURST_SECONDS * TICKS_PER_SECOND));
        }

        @Override
        public boolean execute(EnemyMob target) {
            AttackProfile finisher = AttackProfile.none().withArmorPenetration(1f, 0f)
                    .withPlatingPenetration(1f).asPeriodic();
            SniperTower.this.dealDamage(target, Damage.physical(EXECUTION_DAMAGE), finisher);
            return target.isDead();
        }

        @Override
        public void applyVulnerable(EnemyMob target, int stacks) {
            SniperTower.this.applyVulnerable(target, stacks);
        }
    }

    /** Two perks that one node brings. */
    private record Both(SniperPerk first, SniperPerk second) implements SniperPerk {

        @Override
        public SniperShot shape(SniperShot shot, ShotContext context) {
            return this.second.shape(this.first.shape(shot, context), context);
        }
    }
}
