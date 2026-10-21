package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.enemy.Rank;
import td.stat.StatModifiers;
import td.term.TermText;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitBonus;
import td.tower.proc.HitCondition;
import td.tower.proc.HitKind;
import td.tower.proc.HitScaling;
import td.tower.proc.Proc;
import td.tower.proc.ProcCondition;
import td.tower.proc.ProcPayload;
import td.tower.sniper.AimFocus;
import td.tower.sniper.AimLock;
import td.tower.sniper.CritStreakMechanic;
import td.tower.sniper.ExecutionerMechanic;
import td.tower.sniper.PierceRule;
import td.tower.sniper.RailgunMechanic;
import td.tower.sniper.RicochetMechanic;
import td.tower.sniper.ShotActions;
import td.tower.sniper.ShotContext;
import td.tower.sniper.ShotResult;
import td.tower.sniper.SniperAim;
import td.tower.sniper.SniperMechanic;
import td.tower.sniper.SniperShot;
import td.tower.sniper.SniperSpec;
import td.tower.sniper.SpotterUplinkMechanic;
import td.tower.sniper.WeakSpotMechanic;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.tower.targeting.OnSegmentTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.TargetPriority;
import td.tower.targeting.TargetSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.BaseSlotMechanics;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.NodeEffects;
import td.tower.upgrade.OwnedMechanics;
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
import java.util.Set;

/**
 * The rhythmic single-target tower: each shot builds on the last, whichever enemy it hits, until it
 * stops firing for a while. It fires at the
 * visible enemy in range furthest along the path; the first special it owns decides whom it aims at
 * instead. The turret turns at a capped rate and holds its heading when idle.
 * <p>
 * What each owned node does lives in a {@link SniperMechanic}: the tower shapes every shot through them
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
    /** Enough to kill anything, so that an execution takes exactly what health is left. */
    private static final int EXECUTION_DAMAGE = 1_000_000_000;

    private static final ProcPayload FRENZY = new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.FRENZY, 3,
            TowerModifiers.attackSpeed(1f)).keepingARunningOne().lengthenedByBuffs());

    private static final BaseSlotMechanics BASE_MECHANICS = BaseSlotMechanics.none()
            .withAttune(NodeEffects.none()
                    .withHitBonus(HitBonus.per(new HitScaling.PerFocus(TowerTerm.STEADY_AIM, 0),
                            HitAdjustment.critChance(0.1f).plus(HitAdjustment.accuracy(10f)))))
            .withRangeThree(OVERWATCH_RANGE_BONUS, NodeEffects.none().withModifiers(TowerModifiers.deadZone(2f)));

    private static final TermText AIM_DEED = TermText.format("shots with {}", TowerTerm.STEADY_AIM);
    private static final UpgradeNode FOCUSED_OPTICS_1 = UpgradeTier.HEAD_1.node("sniper.head.focused_optics.1",
            "Focused Optics", PRICE)
            .withHitBonus(HitBonus.when(new HitCondition.FreshTarget(), HitAdjustment.critChance(0.5f)))
            .withHitBonus(HitBonus.when(new HitCondition.FocusAtLeast(TowerTerm.STEADY_AIM, 1), HitAdjustment.attackSpeed(0.25f)));
    private static final UpgradeNode FOCUSED_OPTICS_2 = UpgradeTier.HEAD_2.node("sniper.head.focused_optics.2",
            "Focused Optics II", PRICE)
            .withModifiers(TowerModifiers.damage(16f))
            .withProc(Proc.onCrit(FRENZY))
            .withProc(Proc.onDodge(FRENZY))
            .after(FOCUSED_OPTICS_1);
    private static final UpgradeNode FOCUSED_OPTICS_3 = UpgradeTier.HEAD_3.node("sniper.head.focused_optics.3",
            "Focused Optics III", PRICE)
            .withModifiers(TowerModifiers.damage(12f))
            .withGate(new PurposeCondition(AIM_DEED, 20))
            .after(FOCUSED_OPTICS_2);
    private static final UpgradeNode RAILGUN = UpgradeTier.HEAD_4.node("sniper.head.focused_optics.4a", "Railgun",
            PRICE)
            .after(FOCUSED_OPTICS_3);
    private static final UpgradeNode EXECUTIONER = UpgradeTier.HEAD_4.node("sniper.head.focused_optics.4b",
            "Executioner", PRICE)
            .withHitBonus(HitBonus.when(new HitCondition.All(List.of(new HitCondition.TargetRankAtLeast(Rank.BOSS),
                    new HitCondition.TargetHealthBelow(0.25f))), HitAdjustment.damage(0.5f)))
            .after(FOCUSED_OPTICS_3);
    private static final UpgradeNode MARKSMANS_EYE_1 = UpgradeTier.HEAD_1.node("sniper.head.marksmans_eye.1",
            "Marksman's Eye", PRICE)
            .withModifiers(TowerModifiers.aimStacks(2))
            .withHitBonus(HitBonus.per(new HitScaling.PerFocus(TowerTerm.STEADY_AIM, 0), HitAdjustment.damage(0.2f)))
            .withProc(Proc.onDodge(new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.PATIENCE, 1,
                    StatModifiers.none()).withCapability(TowerCapability.GUARANTEED_CRIT))));
    private static final UpgradeNode MARKSMANS_EYE_2 = UpgradeTier.HEAD_2.node("sniper.head.marksmans_eye.2",
            "Marksman's Eye II", PRICE)
            .withModifiers(TowerModifiers.attackSpeed(0.25f).plus(TowerModifiers.armorPenetrationFlat(30f)))
            .after(MARKSMANS_EYE_1);
    private static final UpgradeNode MARKSMANS_EYE_3 = UpgradeTier.HEAD_3.node("sniper.head.marksmans_eye.3",
            "Marksman's Eye III", PRICE)
            .withGate(new PurposeCondition(AIM_DEED, 20))
            .withCapability(TowerCapability.CRITS_PIERCE_SHIELDS)
            .after(MARKSMANS_EYE_2);
    private static final UpgradeNode UNBROKEN_AIM = UpgradeTier.HEAD_4.node("sniper.head.marksmans_eye.4a",
            "Unbroken Aim", PRICE)
            .withModifiers(TowerModifiers.aimStacks(2))
            .after(MARKSMANS_EYE_3);
    private static final UpgradeNode SUNDER_ROUNDS = UpgradeTier.HEAD_4.node("sniper.head.marksmans_eye.4b",
            "Sunder Rounds", PRICE)
            .withProc(Proc.onCrit(new ProcPayload.ApplyStacks(EffectKind.SUNDERED, 1)).when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT))))
            .after(MARKSMANS_EYE_3);
    private static final UpgradeNode TRADECRAFT_1 = UpgradeTier.EXTRA_1.node("sniper.extra.tradecraft.1",
            "Tradecraft", PRICE);
    private static final UpgradeNode TRADECRAFT_2 = UpgradeTier.EXTRA_2.node("sniper.extra.tradecraft.2",
            "Tradecraft II", PRICE)
            .withHitBonus(HitBonus.when(new HitCondition.BeyondRangeShare(2f / 3f), HitAdjustment.damage(0.25f)))
            .after(TRADECRAFT_1);
    private static final UpgradeNode SILVER_ROUNDS = UpgradeTier.EXTRA_3.node("sniper.extra.tradecraft.3",
            "Silver Rounds", PRICE)
            .withProc(Proc.everyNthShot(3, new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.SILVER_ROUND, 1,
                    TowerModifiers.magicPenetration(0.25f)).withCapability(TowerCapability.MAGIC_DAMAGE))))
            .after(TRADECRAFT_2);
    private static final UpgradeNode SPOTTER_UPLINK = UpgradeTier.EXTRA_4.node("sniper.extra.tradecraft.4",
            "Spotter Uplink", PRICE)
            .after(SILVER_ROUNDS);
    private static final UpgradeNode MOMENTUM = UpgradeTier.SPECIAL.node("sniper.special.momentum", "Momentum", PRICE)
            .withTargeting(TargetPriority.HIGHEST_RANK)
            .withProc(Proc.onCrit(new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.CHARGED_SHOT, 1,
                    TowerModifiers.damageTimes(3f).plus(TowerModifiers.armorPenetration(1f))
                            .plus(TowerModifiers.flatPenetration(1f))))).when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT))))
            .withProc(Proc.onKill(new ProcPayload.GrantStatus(TowerStatus.timed(TowerTerm.HASTE,
                    TowerModifiers.attackSpeed(1f), 100))).when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT))))
            .after(FOCUSED_OPTICS_1);
    private static final UpgradeNode RICOCHET = UpgradeTier.SPECIAL.node("sniper.special.ricochet", "Ricochet", PRICE)
            .withTargeting(TargetPriority.MOST_NEIGHBOURS)
            .after(FOCUSED_OPTICS_1);
    private static final UpgradeNode HEADHUNTER = UpgradeTier.SPECIAL.node("sniper.special.headhunter", "Headhunter",
            PRICE)
            .withTargeting(TargetPriority.HIGHEST_RANK)
            .withHitBonus(HitBonus.when(new HitCondition.TargetRankAtLeast(Rank.ELITE), HitAdjustment.damage(0.75f)))
            .after(FOCUSED_OPTICS_1);
    private static final UpgradeNode HOLLOW_POINT = UpgradeTier.SPECIAL.node("sniper.special.hollow_point",
            "Hollow Point", PRICE)
            .withProc(Proc.onHit(new ProcPayload.ApplyStacks(EffectKind.VULNERABLE, 1)).when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT))))
            .after(MARKSMANS_EYE_1);
    // The id keeps its old word so saved games still load the node.
    private static final UpgradeNode FOURTH_SHOT = UpgradeTier.SPECIAL.node("sniper.special.fifth_shot",
            "Fourth Shot", PRICE)
            .withProc(Proc.everyNthShot(4, new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.DEADEYE_ROUND, 1,
                    TowerModifiers.critMultiplier(1.5f)).withCapability(TowerCapability.GUARANTEED_CRIT))))
            .after(MARKSMANS_EYE_1);
    private static final UpgradeNode SHATTER_SHOT = UpgradeTier.SPECIAL.node("sniper.special.shatter_shot",
            "Shatter Shot", PRICE)
            .withHitBonus(HitBonus.when(new HitCondition.TargetStopped(), HitAdjustment.critChance(1f).plus(HitAdjustment.critMultiplier(1f))))
            .after(MARKSMANS_EYE_1);

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, RANGE, BASE_MECHANICS, FOCUSED_OPTICS_3,
            MARKSMANS_EYE_3))
            .with(FOCUSED_OPTICS_1, FOCUSED_OPTICS_2, FOCUSED_OPTICS_3, RAILGUN, EXECUTIONER, MARKSMANS_EYE_1,
                    MARKSMANS_EYE_2, MARKSMANS_EYE_3, UNBROKEN_AIM, SUNDER_ROUNDS, TRADECRAFT_1, TRADECRAFT_2,
                    SILVER_ROUNDS, SPOTTER_UPLINK, MOMENTUM, RICOCHET, HEADHUNTER, HOLLOW_POINT, FOURTH_SHOT,
                    SHATTER_SHOT)
            .withChoice(ExclusiveChoice.oneOf(FOCUSED_OPTICS_1, MARKSMANS_EYE_1))
            .withChoice(ExclusiveChoice.oneOf(RAILGUN, EXECUTIONER))
            .withChoice(ExclusiveChoice.oneOf(UNBROKEN_AIM, SUNDER_ROUNDS))
            .withChoice(ExclusiveChoice.specials(MOMENTUM, RICOCHET, HEADHUNTER, HOLLOW_POINT, FOURTH_SHOT,
                    SHATTER_SHOT));

    private static final MechanicCatalogue<SniperMechanic> MECHANICS = MechanicCatalogue.<SniperMechanic>empty()
            .with(FOCUSED_OPTICS_3.id(), WeakSpotMechanic::new)
            .with(RAILGUN.id(), RailgunMechanic::new)
            .with(EXECUTIONER.id(), ExecutionerMechanic::new)
            .with(TRADECRAFT_1.id(), CritStreakMechanic::new)
            .with(SPOTTER_UPLINK.id(), SpotterUplinkMechanic::new)
            .with(RICOCHET.id(), RicochetMechanic::new);

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final AimFocus focus = new AimFocus();
    private final ShotActions actions = new Actions();
    private final OwnedMechanics<SniperMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private final FireClock clock = new FireClock(COOLDOWN_MAX);
    private SniperShot shotInFlight;
    private TowerStats statsInFlight;
    private EnemyMob currentTarget;
    private ShotTrace lastShot;

    public SniperTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SNIPER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .withCritChance(CRIT_CHANCE).withCritMultiplier(CRIT_MULTIPLIER)
                .with(TowerStat.DEAD_ZONE, 0f).with(TowerStat.AIM_STACKS, 1f), context, x, y);
    }

    @Override
    protected MechanicCatalogue<SniperMechanic> mechanicCatalogue() {
        return MECHANICS;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.mechanics.add(node);
    }

    /** Whom it may shoot and how it picks, as the mechanics in {@code owned} make it. */
    private SniperSpec spec(List<SniperMechanic> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        SniperSpec spec = SniperSpec.from(view);
        Optional<TargetPriority> priority = this.upgrades().targeting();
        if (priority.isPresent()) {
            spec = spec.aimingAt(new SniperAim(priority.get().selector(view.cellSize()), priority.get().label()));
        }
        for (SniperMechanic mechanic : owned) {
            spec = mechanic.refineSpec(spec);
        }
        float deadZone = this.stats().value(TowerStat.DEAD_ZONE);
        if (deadZone > 0f) {
            spec = spec.withReach(spec.reach().withDeadZone(deadZone * view.cellSize()));
        }
        return spec;
    }

    private EnemyMob findEnemy(SniperSpec spec) {
        TargetSelector selector = PreferringSelector.priority(spec.chosenAim().selector());
        return selector.selectFrom(spec.reach().matching(this.context.enemies())).orElse(null);
    }

    public void doTick(int gameTime) {
        this.focus.advance();
        if (this.clock.isReady()) {
            List<SniperMechanic> owned = this.mechanics.all();
            SniperSpec spec = this.spec(owned);
            this.currentTarget = this.findEnemy(spec);
            if (this.currentTarget != null) {
                this.fire(this.currentTarget, owned);
            }
        }
        this.clock.advance(this.attackRate());
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    /** One shot: shaped by the owned mechanics, landed, settled and reacted to, and the wait that follows. */
    private void fire(EnemyMob target, List<SniperMechanic> owned) {
        TowerStats stats = this.beginShot();
        AimLock lock = this.focus.lock(target, this.aimStackCap());
        ShotContext context = new ShotContext(target, lock, this.shotNumber(), this.rangeShareOf(target));
        HitAdjustment adjustment = this.hitAdjustment(target, HitKind.DIRECT, context.rangeShare(), stats, lock.stacks(),
                lock.fresh());
        SniperShot shot = SniperShot.of(adjustment.applyTo(stats.attack())).scaledBy(adjustment.damageFactor())
                .withAttackSpeedBonus(adjustment.attackSpeed());
        if (stats.has(TowerCapability.MAGIC_DAMAGE)) {
            shot = shot.withType(DamageType.MAGIC);
        }
        for (SniperMechanic mechanic : owned) {
            shot = mechanic.shape(shot, context);
        }
        this.shotInFlight = shot;
        this.statsInFlight = stats;
        HitResult hit = shot.piercing() ? this.pierce(shot, target, stats) : this.land(shot.aimed(), target, 1f, stats);
        if (hit.dodged()) {
            this.reportDodge(target, HitKind.DIRECT);
            this.lastShot = this.traceOf(shot, target, false);
        } else {
            this.settle(new ShotResult(target, hit.critical(), target.isDead()), shot, owned);
        }
        if (lock.stacks() > 0) {
            this.countDeedOfAttack();
        }
        double rate = this.attackRate();
        this.clock.spend(rate / (rate + shot.attackSpeedBonus()));
    }

    /** A shot that landed: the mechanics decide what it counts as, the procs hear of it, then the mechanics react. */
    private void settle(ShotResult landed, SniperShot shot, List<SniperMechanic> owned) {
        ShotResult result = landed;
        for (SniperMechanic mechanic : owned) {
            result = mechanic.settle(result, this.actions);
        }
        this.report(result.target(), HitKind.DIRECT, result.critical(), result.killed());
        this.lastShot = this.traceOf(shot, result.target(), result.critical());
        for (SniperMechanic mechanic : owned) {
            mechanic.react(result, this.actions);
        }
    }

    /** Where the shot went: to its target, or for a piercing shot to the end of its line. */
    private ShotTrace traceOf(SniperShot shot, EnemyMob target, boolean critical) {
        if (!shot.piercing()) {
            return new ShotTrace((float) target.getX(), (float) target.getY(), critical, shot.type());
        }
        double heading = TurretAim.angleTo(this.centerX, this.centerY, target.getX(), target.getY());
        double reach = this.railReach(shot.pierce());
        return new ShotTrace((float) (this.centerX + Math.cos(heading) * reach),
                (float) (this.centerY + Math.sin(heading) * reach), critical, shot.type());
    }

    private double railReach(PierceRule pierce) {
        return this.rangeReal() + pierce.extraReachCells() * this.context.getBoard().scale();
    }

    private float rangeShareOf(EnemyMob target) {
        return (float) (Math.hypot(target.getX() - this.centerX, target.getY() - this.centerY) / this.rangeReal());
    }

    /** Lands {@code shot} on {@code target} at {@code share} of its damage. */
    private HitResult land(SniperShot shot, EnemyMob target, float share, TowerStats stats) {
        int amount = Math.round(stats.damage() * shot.damageFactor() * share);
        return this.landHit(target, Damage.of(shot.type(), amount), shot.attack());
    }

    /**
     * A shot through everything on the line from the Sniper past {@code target}; each enemy after
     * the first it passes takes less. A line is not aimed, so nothing on it dodges. What it did to
     * {@code target}.
     */
    private HitResult pierce(SniperShot shot, EnemyMob target, TowerStats stats) {
        float scale = this.context.getBoard().scale();
        double heading = TurretAim.angleTo(this.centerX, this.centerY, target.getX(), target.getY());
        PierceRule pierce = shot.pierce();
        double reach = this.railReach(pierce);
        List<EnemyMob> line = new OnSegmentTargetQuery(this.centerX, this.centerY,
                this.centerX + Math.cos(heading) * reach, this.centerY + Math.sin(heading) * reach,
                pierce.halfWidthCells() * scale).matching(this.context.enemies());
        HitResult aimedHit = HitResult.none();
        float share = 1f;
        for (EnemyMob enemy : line) {
            HitResult hit = this.land(shot, enemy, share, stats);
            if (enemy == target) {
                aimedHit = hit;
            }
            share *= pierce.falloff();
        }
        return aimedHit;
    }

    /**
     * A bounce hits the nearest enemy beside the last one struck that this shot has not hit yet. Each
     * bounce is aimed, so each may be dodged; the next bounce goes on from the enemy that dodged.
     */
    private void ricochet(EnemyMob from, int bounces, float share, float reachCells) {
        SniperShot bounce = this.shotInFlight.withDamageFactor(share).aimed();
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
            this.land(bounce, next.get(), 1f, this.statsInFlight);
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

    /** How many Steady Aim stacks the next shot has, whichever enemy it hits; empty without Attune. */
    public OptionalInt steadyAimStacks() {
        if (!this.upgrades().owns(StandardBaseSlot.ATTUNE_ID)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Math.min(this.aimStackCap(), this.focus.stacks()));
    }

    private int aimStackCap() {
        return Math.round(this.stats().value(TowerStat.AIM_STACKS));
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public float getCoolDownFraction() {
        return this.clock.remaining();
    }

    @Override
    protected List<TowerBuffState> ownConditions() {
        List<TowerBuffState> states = new ArrayList<>();
        int cap = this.aimStackCap();
        int stacks = Math.min(cap, this.focus.stacks());
        if (stacks > 0) {
            List<TermText> parts = new ArrayList<>(this.upgrades().hitBonuses().stream()
                    .filter(bonus -> bonus.scaling().focusCounter().filter(TowerTerm.STEADY_AIM::equals).isPresent())
                    .map(bonus -> TermText.of(bonus.adjustment().describe())).toList());
            parts.add(TermText.of("up to " + cap + " stacks"));
            states.add(TowerBuffState.of(TowerTerm.STEADY_AIM, TermText.join("; ", parts)).withStacks(stacks)
                    .withClock(this.focus.ticksUntilFade(), AimFocus.STACK_TICKS));
        }
        this.mechanics.all().forEach(mechanic -> mechanic.condition().ifPresent(states::add));
        return states;
    }

    @Override
    protected String targetingLabel() {
        return this.spec(this.mechanics.all()).chosenAim().label();
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

    /** What the mechanics may make this tower do. */
    private final class Actions implements ShotActions {

        @Override
        public boolean execute(EnemyMob target) {
            AttackProfile finisher = AttackProfile.none().withArmorPenetration(1f, 0f)
                    .withFlatPenetration(1f).asPeriodic();
            SniperTower.this.dealDamage(target, Damage.physical(EXECUTION_DAMAGE), finisher);
            return target.isDead();
        }

        @Override
        public void ricochet(EnemyMob from, int bounces, float share, float reachCells) {
            SniperTower.this.ricochet(from, bounces, share, reachCells);
        }
    }
}
