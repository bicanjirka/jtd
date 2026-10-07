package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.sniper.AimFocus;
import td.tower.sniper.AimLock;
import td.tower.sniper.AimRules;
import td.tower.sniper.ArmorPiercePerk;
import td.tower.sniper.CleanShotPerk;
import td.tower.sniper.CritStreakPerk;
import td.tower.sniper.ExecutionerPerk;
import td.tower.sniper.FifthShotPerk;
import td.tower.sniper.FrenzyPerk;
import td.tower.sniper.HeadhunterPerk;
import td.tower.sniper.HollowPointPerk;
import td.tower.sniper.LongShotPerk;
import td.tower.sniper.MomentumPerk;
import td.tower.sniper.OverwatchPerk;
import td.tower.sniper.QuickScopePerk;
import td.tower.sniper.RailgunPerk;
import td.tower.sniper.RicochetPerk;
import td.tower.sniper.ShatterShotPerk;
import td.tower.sniper.ShotActions;
import td.tower.sniper.ShotContext;
import td.tower.sniper.ShotResult;
import td.tower.sniper.SilverRoundsPerk;
import td.tower.sniper.SniperPerk;
import td.tower.sniper.SniperShot;
import td.tower.sniper.SniperSpec;
import td.tower.sniper.SniperTempo;
import td.tower.sniper.SpotterUplinkPerk;
import td.tower.sniper.SteadyAimPerk;
import td.tower.sniper.SteadyAimStacksPerk;
import td.tower.sniper.SteadyTempoPerk;
import td.tower.sniper.SunderRoundsPerk;
import td.tower.sniper.UnbrokenAimPerk;
import td.tower.sniper.WeakSpotPerk;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.tower.targeting.OnSegmentTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.TargetSelector;
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
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The patient single-target tower: it gets better the longer it stays on one enemy. It fires at the
 * visible enemy in range furthest along the path; the first special it owns decides whom it aims at
 * instead. The turret turns at a capped rate and holds its heading when idle.
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

    private static final float OVERWATCH_RANGE_BONUS = 0.35f;
    /** Railgun reaches this far past the Sniper's range, and every enemy it pierces after the first takes less. */
    private static final float RAILGUN_EXTRA_REACH_CELLS = 2f;
    private static final float RAILGUN_FALLOFF = 0.75f;
    private static final float RAILGUN_HALF_WIDTH_CELLS = 0.35f;
    /** Enough to kill anything, so that an execution takes exactly what health is left. */
    private static final int EXECUTION_DAMAGE = 1_000_000_000;
    private static final float MOMENTUM_BURST_SECONDS = 5f;

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
            .withExtraEffect("Steady Aim stacks to 3 (+30%: 35% crit from the 4th shot)");
    private static final UpgradeNode MARKSMANS_EYE_2 = UpgradeTier.HEAD_2.node("sniper.head.marksmans_eye.2",
            "Marksman's Eye II", PRICE)
            .withBuff(TowerBuff.fireRate(0.25f))
            .withExtraEffect("ignores 30 armor")
            .after(MARKSMANS_EYE_1);
    private static final UpgradeNode MARKSMANS_EYE_3 = UpgradeTier.HEAD_3.node("sniper.head.marksmans_eye.3",
            "Marksman's Eye III", PRICE)
            .withGate(new PurposeCondition("Steady Aim shots", 20))
            .withExtraEffect("Clean Shot: crits go through shields straight to health")
            .after(MARKSMANS_EYE_2);
    private static final UpgradeNode UNBROKEN_AIM = UpgradeTier.HEAD_4.node("sniper.head.marksmans_eye.4a",
            "Unbroken Aim", PRICE)
            .withExtraEffect("Steady Aim stacks to 5 (55% crit) and survives a kill; only retargeting a living "
                    + "enemy resets it")
            .after(MARKSMANS_EYE_3);
    private static final UpgradeNode SUNDER_ROUNDS = UpgradeTier.HEAD_4.node("sniper.head.marksmans_eye.4b",
            "Sunder Rounds", PRICE)
            .withExtraEffect("every crit adds 1 Sundered")
            .after(MARKSMANS_EYE_3);
    private static final UpgradeNode TRADECRAFT_1 = UpgradeTier.EXTRA_1.node("sniper.extra.tradecraft.1",
            "Tradecraft", PRICE)
            .withExtraEffect("a crit makes the next crit deal +25% crit damage, two in a row +50%, no further");
    private static final UpgradeNode TRADECRAFT_2 = UpgradeTier.EXTRA_2.node("sniper.extra.tradecraft.2",
            "Tradecraft II", PRICE)
            .withExtraEffect("+25% damage against targets past two thirds of its range")
            .after(TRADECRAFT_1);
    private static final UpgradeNode SILVER_ROUNDS = UpgradeTier.EXTRA_3.node("sniper.extra.tradecraft.3",
            "Silver Rounds", PRICE)
            .withExtraEffect("every 3rd shot is magic with +25% magic penetration; it keeps the crit streak")
            .after(TRADECRAFT_2);
    private static final UpgradeNode SPOTTER_UPLINK = UpgradeTier.EXTRA_4.node("sniper.extra.tradecraft.4",
            "Spotter Uplink", PRICE)
            .withExtraEffect("may shoot Marked or Revealed enemies within twice its range")
            .after(SILVER_ROUNDS);
    private static final UpgradeNode MOMENTUM = UpgradeTier.SPECIAL.node("sniper.special.momentum", "Momentum", PRICE)
            .withExtraEffect("a crit charges the next shot to x5 damage that ignores armor and plating; a kill grants "
                    + "+100% fire rate for 5s; aims at the highest rank")
            .after(FOCUSED_OPTICS_1);
    private static final UpgradeNode RICOCHET = UpgradeTier.SPECIAL.node("sniper.special.ricochet", "Ricochet", PRICE)
            .withExtraEffect("a crit bounces to the nearest enemy within 1.5 cells for 60%, up to 3 bounces, each "
                    + "can crit; aims at the enemy with most neighbours")
            .after(FOCUSED_OPTICS_1);
    private static final UpgradeNode HEADHUNTER = UpgradeTier.SPECIAL.node("sniper.special.headhunter", "Headhunter",
            PRICE)
            .withExtraEffect("+40% damage against Elite and Boss rank; aims at the highest rank")
            .after(FOCUSED_OPTICS_1);
    private static final UpgradeNode HOLLOW_POINT = UpgradeTier.SPECIAL.node("sniper.special.hollow_point",
            "Hollow Point", PRICE)
            .withExtraEffect("crits apply Vulnerable, +15% damage taken, stacks x3; aims at the most health")
            .after(MARKSMANS_EYE_1);
    private static final UpgradeNode FIFTH_SHOT = UpgradeTier.SPECIAL.node("sniper.special.fifth_shot",
            "Fifth Shot", PRICE)
            .withExtraEffect("every 5th shot is a guaranteed crit with +50% crit damage; aims at the most health")
            .after(MARKSMANS_EYE_1);
    private static final UpgradeNode SHATTER_SHOT = UpgradeTier.SPECIAL.node("sniper.special.shatter_shot",
            "Shatter Shot", PRICE)
            .withExtraEffect("crits on a frozen enemy deal +50% crit damage; aims at a frozen enemy first")
            .after(MARKSMANS_EYE_1);

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS, FOCUSED_OPTICS_3,
            MARKSMANS_EYE_3))
            .with(FOCUSED_OPTICS_1, FOCUSED_OPTICS_2, FOCUSED_OPTICS_3, RAILGUN, EXECUTIONER, MARKSMANS_EYE_1,
                    MARKSMANS_EYE_2, MARKSMANS_EYE_3, UNBROKEN_AIM, SUNDER_ROUNDS, TRADECRAFT_1, TRADECRAFT_2,
                    SILVER_ROUNDS, SPOTTER_UPLINK, MOMENTUM, RICOCHET, HEADHUNTER, HOLLOW_POINT, FIFTH_SHOT,
                    SHATTER_SHOT)
            .withChoice(ExclusiveChoice.oneOf(FOCUSED_OPTICS_1, MARKSMANS_EYE_1))
            .withChoice(ExclusiveChoice.oneOf(RAILGUN, EXECUTIONER))
            .withChoice(ExclusiveChoice.oneOf(UNBROKEN_AIM, SUNDER_ROUNDS))
            .withChoice(ExclusiveChoice.specials(MOMENTUM, RICOCHET, HEADHUNTER, HOLLOW_POINT, FIFTH_SHOT,
                    SHATTER_SHOT));

    private static final PerkCatalogue<SniperPerk> PERKS = PerkCatalogue.<SniperPerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, SteadyAimPerk::new)
            .with(StandardBaseSlot.RANGE_3_ID, OverwatchPerk::new)
            .with(FOCUSED_OPTICS_1.id(), QuickScopePerk::new)
            .with(FOCUSED_OPTICS_1.id(), SteadyTempoPerk::new)
            .with(FOCUSED_OPTICS_2.id(), FrenzyPerk::new)
            .with(FOCUSED_OPTICS_3.id(), WeakSpotPerk::new)
            .with(RAILGUN.id(), RailgunPerk::new)
            .with(EXECUTIONER.id(), ExecutionerPerk::new)
            .with(MARKSMANS_EYE_1.id(), () -> new SteadyAimStacksPerk(3))
            .with(MARKSMANS_EYE_2.id(), () -> new ArmorPiercePerk(30f))
            .with(MARKSMANS_EYE_3.id(), CleanShotPerk::new)
            .with(UNBROKEN_AIM.id(), UnbrokenAimPerk::new)
            .with(SUNDER_ROUNDS.id(), SunderRoundsPerk::new)
            .with(TRADECRAFT_1.id(), CritStreakPerk::new)
            .with(TRADECRAFT_2.id(), LongShotPerk::new)
            .with(SILVER_ROUNDS.id(), SilverRoundsPerk::new)
            .with(SPOTTER_UPLINK.id(), SpotterUplinkPerk::new)
            .with(MOMENTUM.id(), MomentumPerk::new)
            .with(RICOCHET.id(), RicochetPerk::new)
            .with(HEADHUNTER.id(), HeadhunterPerk::new)
            .with(HOLLOW_POINT.id(), HollowPointPerk::new)
            .with(FIFTH_SHOT.id(), FifthShotPerk::new)
            .with(SHATTER_SHOT.id(), ShatterShotPerk::new);

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final AimFocus focus = new AimFocus();
    private final SniperTempo tempo = new SniperTempo();
    private final ShotActions actions = new Actions();
    private final OwnedPerks<SniperPerk> perks = new OwnedPerks<>(PERKS);
    private final FireClock clock = new FireClock(COOLDOWN_MAX);
    private SniperShot shotInFlight;
    private EnemyMob currentTarget;
    private ShotTrace lastShot;
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

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.perks.add(node);
    }

    private Viewpoint viewpoint() {
        return new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
    }

    /** Whom it may shoot and how it picks, as the perks in {@code owned} make it. */
    private SniperSpec spec(List<SniperPerk> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        SniperSpec spec = SniperSpec.from(view);
        for (SniperPerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        return spec;
    }

    private EnemyMob findEnemy(SniperSpec spec) {
        TargetSelector selector = PreferringSelector.priority(spec.chosenAim().selector());
        return selector.selectFrom(spec.reach().matching(this.context.enemies())).orElse(null);
    }

    public void doTick(int gameTime) {
        this.currentTick = gameTime;
        if (this.clock.isReady()) {
            List<SniperPerk> owned = this.perks.all();
            SniperSpec spec = this.spec(owned);
            this.currentTarget = this.findEnemy(spec);
            if (this.currentTarget != null) {
                this.fire(this.currentTarget, owned, spec.steadyAim());
            }
        }
        this.clock.advance(this.fireRateCurrent());
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    /** One shot: shaped by the owned perks, landed, settled and reacted to, and the wait that follows. */
    private void fire(EnemyMob target, List<SniperPerk> owned, AimRules steadyAim) {
        this.shotsFired++;
        AimLock lock = this.focus.lock(target, steadyAim.stackCap(), steadyAim.survivesKill());
        ShotContext context = new ShotContext(target, lock, this.shotsFired, this.rangeShareOf(target));
        SniperShot shot = SniperShot.of(this.stats().attack());
        for (SniperPerk perk : owned) {
            shot = perk.shape(shot, context);
        }
        this.shotInFlight = shot;
        boolean critical = shot.piercing() ? this.pierce(shot, target) : this.land(shot, target, 1f);
        ShotResult result = new ShotResult(target, critical, target.isDead());
        for (SniperPerk perk : owned) {
            result = perk.settle(result, this.actions);
        }
        this.lastShot = this.traceOf(shot, target, result.critical());
        for (SniperPerk perk : owned) {
            perk.react(result, this.actions);
        }
        if (lock.stacks() > 0) {
            this.countDeedOfAttack();
        }
        float fireRate = shot.fireRateBonus();
        float tempoBonus = this.tempo.takeFireRateBonus(this.currentTick);
        this.clock.spend(1.0 / TowerBuff.fireRate(fireRate).combine(TowerBuff.fireRate(tempoBonus))
                .fireRateMultiplier());
    }

    /** Where the shot went: to its target, or for a piercing shot to the end of its line. */
    private ShotTrace traceOf(SniperShot shot, EnemyMob target, boolean critical) {
        if (!shot.piercing()) {
            return new ShotTrace((float) target.getX(), (float) target.getY(), critical, shot.type());
        }
        double heading = TurretAim.angleTo(this.centerX, this.centerY, target.getX(), target.getY());
        double reach = this.railReach();
        return new ShotTrace((float) (this.centerX + Math.cos(heading) * reach),
                (float) (this.centerY + Math.sin(heading) * reach), critical, shot.type());
    }

    private double railReach() {
        return this.rangeReal() + RAILGUN_EXTRA_REACH_CELLS * this.context.getBoard().scale();
    }

    private float rangeShareOf(EnemyMob target) {
        return (float) (Math.hypot(target.getX() - this.centerX, target.getY() - this.centerY) / this.rangeReal());
    }

    /** Lands {@code shot} on {@code target} at {@code share} of its damage; whether it crit. */
    private boolean land(SniperShot shot, EnemyMob target, float share) {
        int amount = Math.round(this.damageCurrent() * shot.damageFactor() * share);
        return this.dealDamage(target, Damage.of(shot.type(), amount), shot.attack());
    }

    /**
     * A shot through everything on the line from the Sniper past {@code target}; each enemy after
     * the first it passes takes less. Whether the aimed enemy's hit crit.
     */
    private boolean pierce(SniperShot shot, EnemyMob target) {
        float scale = this.context.getBoard().scale();
        double heading = TurretAim.angleTo(this.centerX, this.centerY, target.getX(), target.getY());
        double reach = this.railReach();
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

    /** A bounce hits the nearest enemy beside the last one struck that this shot has not hit yet. */
    private void ricochet(EnemyMob from, int bounces, float share, float reachCells) {
        SniperShot bounce = this.shotInFlight.withDamageFactor(share);
        float reach = reachCells * this.context.getBoard().scale();
        List<EnemyMob> struck = new ArrayList<>(List.of(from));
        EnemyMob last = from;
        for (int i = 0; i < bounces; i++) {
            List<EnemyMob> beside = InRangeTargetQuery.visible((int) last.getX(), (int) last.getY(), reach)
                    .matching(this.context.enemies());
            beside.removeAll(struck);
            Optional<EnemyMob> next = new NearestSelector(last.getX(), last.getY()).selectFrom(beside);
            if (next.isEmpty()) {
                return;
            }
            this.land(bounce, next.get(), 1f);
            struck.add(next.get());
            last = next.get();
        }
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    public boolean wasLastShotCritical() {
        return this.lastShot != null && this.lastShot.critical();
    }

    /** The last shot fired, for drawing its trace; empty before the first. */
    public Optional<ShotTrace> lastShot() {
        return Optional.ofNullable(this.lastShot);
    }

    /** How many Steady Aim stacks the next shot at the current target has; empty without Attune. */
    public OptionalInt steadyAimStacks() {
        if (this.currentTarget == null || !this.upgrades().owns(StandardBaseSlot.ATTUNE_ID)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(this.focus.stacksOn(this.currentTarget));
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public float getCoolDownFraction() {
        return this.clock.remaining();
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        BehaviourLine targets = new BehaviourLine(BehaviourMarker.TARGETING, "Targets", this.spec(this.perks.all()).chosenAim().label());
        if (!this.upgrades().owns(HOLLOW_POINT.id())) {
            return List.of(targets);
        }
        return List.of(targets, new BehaviourLine(BehaviourMarker.VULNERABLE, "Crits apply", "vulnerable"));
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSniperTower(this);
    }

    /**
     * Where a shot went and what it was.
     *
     * @param toX      where its line ends, in pixels
     * @param toY      where its line ends, in pixels
     * @param critical whether it crit its target
     * @param type     what damage it dealt
     */
    public record ShotTrace(float toX, float toY, boolean critical, DamageType type) {
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
        public void applyStacks(EnemyMob target, EffectKind kind, int stacks) {
            SniperTower.this.applyStacks(target, kind, stacks);
        }

        @Override
        public void ricochet(EnemyMob from, int bounces, float share, float reachCells) {
            SniperTower.this.ricochet(from, bounces, share, reachCells);
        }
    }
}
