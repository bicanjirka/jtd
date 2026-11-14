package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.StackingDebuff;
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
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.NodeEffects;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.RangeNodes;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.Vec2;

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
    /** How far a bullet flies in a tick, in cells: fast, yet its travel is seen. */
    private static final float BULLET_CELLS_PER_TICK = 2f;
    private static final int MIN_FLIGHT_TICKS = 2;

    private static final ProcPayload FRENZY = new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.FRENZY, 3,
            TowerModifiers.attackSpeed(1f)).keepingARunningOne().lengthenedByBuffs());

    private static final UpgradeNode ATTUNE = UpgradeNode.of("base.attune", "Attune", 12)
            .withHitBonus(HitBonus.per(new HitScaling.PerFocus(TowerTerm.STEADY_AIM, 0),
                    HitAdjustment.critChance(0.1f).plus(HitAdjustment.accuracy(10f))));

    private static final UpgradeNode FOCUSED_OPTICS_1 = UpgradeNode.of("sniper.head.focused_optics.1", "Focused Optics", 15)
            .withHitBonus(HitBonus.when(new HitCondition.FreshTarget(), HitAdjustment.critChance(0.5f)))
            .withHitBonus(HitBonus.when(new HitCondition.FocusAtLeast(TowerTerm.STEADY_AIM, 1), HitAdjustment.attackSpeed(0.25f)));
    private static final UpgradeNode FOCUSED_OPTICS_2 = UpgradeNode.of("sniper.head.focused_optics.2", "Focused Optics II", 23)
            .withModifiers(TowerModifiers.damage(16f))
            .withProc(Proc.onCrit(FRENZY))
            .withProc(Proc.onDodge(FRENZY))
            .after(FOCUSED_OPTICS_1)
            .inTier(1);
    private static final UpgradeNode FOCUSED_OPTICS_3 = UpgradeNode.of("sniper.head.focused_optics.3", "Focused Optics III", 38)
            .withModifiers(TowerModifiers.damage(12f))
            .after(FOCUSED_OPTICS_2)
            .inTier(2);
    private static final UpgradeNode RAILGUN = UpgradeNode.of("sniper.head.focused_optics.4a", "Railgun", 60)
            .after(FOCUSED_OPTICS_3)
            .inTier(3);
    private static final UpgradeNode EXECUTIONER = UpgradeNode.of("sniper.head.focused_optics.4b", "Executioner", 60)
            .withHitBonus(HitBonus.when(new HitCondition.All(List.of(new HitCondition.TargetRankAtLeast(Rank.BOSS),
                    new HitCondition.TargetHealthBelow(0.25f))), HitAdjustment.damage(0.5f)))
            .after(FOCUSED_OPTICS_3)
            .inTier(3);
    private static final UpgradeNode MARKSMANS_EYE_1 = UpgradeNode.of("sniper.head.marksmans_eye.1", "Marksman's Eye", 15)
            .withModifiers(TowerModifiers.aimStacks(2))
            .withHitBonus(HitBonus.per(new HitScaling.PerFocus(TowerTerm.STEADY_AIM, 0), HitAdjustment.damage(0.2f)))
            .withProc(Proc.onDodge(new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.PATIENCE, 1,
                    StatModifiers.none()).withCapability(TowerCapability.GUARANTEED_CRIT))));
    private static final UpgradeNode MARKSMANS_EYE_2 = UpgradeNode.of("sniper.head.marksmans_eye.2", "Marksman's Eye II", 23)
            .withModifiers(TowerModifiers.attackSpeed(0.25f).plus(TowerModifiers.armorPenetrationFlat(30f)))
            .after(MARKSMANS_EYE_1)
            .inTier(1);
    private static final UpgradeNode MARKSMANS_EYE_3 = UpgradeNode.of("sniper.head.marksmans_eye.3", "Marksman's Eye III", 38)
            .withCapability(TowerCapability.CRITS_PIERCE_SHIELDS)
            .after(MARKSMANS_EYE_2)
            .inTier(2);
    private static final UpgradeNode UNBROKEN_AIM = UpgradeNode.of("sniper.head.marksmans_eye.4a", "Unbroken Aim", 60)
            .withModifiers(TowerModifiers.aimStacks(2))
            .after(MARKSMANS_EYE_3)
            .inTier(3);
    private static final UpgradeNode SUNDER_ROUNDS = UpgradeNode.of("sniper.head.marksmans_eye.4b", "Sunder Rounds", 60)
            .withProc(Proc.onHit(new ProcPayload.ApplyStacks(StackingDebuff.SUNDERED, 1)).when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT))))
            .after(MARKSMANS_EYE_3)
            .inTier(3);
    private static final UpgradeNode TRADECRAFT_1 = UpgradeNode.of("sniper.extra.tradecraft.1", "Tradecraft", 12);
    private static final UpgradeNode TRADECRAFT_2 = UpgradeNode.of("sniper.extra.tradecraft.2", "Tradecraft II", 18)
            .withHitBonus(HitBonus.when(new HitCondition.BeyondRangeShare(2f / 3f), HitAdjustment.damage(0.25f)))
            .after(TRADECRAFT_1)
            .inTier(1);
    private static final UpgradeNode SILVER_ROUNDS = UpgradeNode.of("sniper.extra.tradecraft.3", "Silver Rounds", 30)
            .withProc(Proc.everyNthShot(3, new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.SILVER_ROUND, 1,
                    TowerModifiers.magicPenetration(0.25f)).withCapability(TowerCapability.MAGIC_DAMAGE))))
            .after(TRADECRAFT_2)
            .inTier(2);
    private static final UpgradeNode SPOTTER_UPLINK = UpgradeNode.of("sniper.extra.tradecraft.4", "Spotter Uplink", 45)
            .after(SILVER_ROUNDS)
            .inTier(3);
    private static final UpgradeNode MOMENTUM = UpgradeNode.of("sniper.special.momentum", "Momentum", 60)
            .withTargeting(TargetPriority.HIGHEST_RANK)
            .withProc(Proc.onCrit(new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.CHARGED_SHOT, 1,
                    TowerModifiers.damageTimes(3f).plus(TowerModifiers.armorPenetration(1f))
                            .plus(TowerModifiers.flatPenetration(1f))))).when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT))))
            .withProc(Proc.onKill(new ProcPayload.GrantStatus(TowerStatus.timed(TowerTerm.HASTE,
                    TowerModifiers.attackSpeed(1f), 100))).when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT))))
            .after(FOCUSED_OPTICS_1)
            .inTier(2);
    private static final UpgradeNode RICOCHET = UpgradeNode.of("sniper.special.ricochet", "Ricochet", 60)
            .withTargeting(TargetPriority.MOST_NEIGHBOURS)
            .after(FOCUSED_OPTICS_1)
            .inTier(2);
    private static final UpgradeNode HEADHUNTER = UpgradeNode.of("sniper.special.headhunter", "Headhunter", 60)
            .withTargeting(TargetPriority.HIGHEST_RANK)
            .withHitBonus(HitBonus.when(new HitCondition.TargetRankAtLeast(Rank.ELITE), HitAdjustment.damage(0.75f)))
            .after(FOCUSED_OPTICS_1)
            .inTier(3);
    private static final UpgradeNode HOLLOW_POINT = UpgradeNode.of("sniper.special.hollow_point", "Hollow Point", 60)
            .withProc(Proc.onHit(new ProcPayload.ApplyStacks(StackingDebuff.VULNERABLE, 1)).when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT))))
            .after(MARKSMANS_EYE_1)
            .inTier(2);
    // The id keeps its old word so saved games still load the node.
    private static final UpgradeNode FOURTH_SHOT = UpgradeNode.of("sniper.special.fifth_shot", "Fourth Shot", 60)
            .withProc(Proc.everyNthShot(4, new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.DEADEYE_ROUND, 1,
                    TowerModifiers.critMultiplier(1.5f)).withCapability(TowerCapability.GUARANTEED_CRIT))))
            .after(MARKSMANS_EYE_1)
            .inTier(2);
    private static final UpgradeNode SHATTER_SHOT = UpgradeNode.of("sniper.special.shatter_shot", "Shatter Shot", 60)
            .withModifiers(TowerModifiers.critChance(0.15f).plus(TowerModifiers.critMultiplier(0.5f)))
            .after(MARKSMANS_EYE_1)
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(RangeNodes.of(PRICE, RANGE, OVERWATCH_RANGE_BONUS,
                    NodeEffects.none().withModifiers(TowerModifiers.deadZone(2f))))
            .with(ATTUNE, FOCUSED_OPTICS_1, MARKSMANS_EYE_1, TRADECRAFT_1,
                    FOCUSED_OPTICS_2, MARKSMANS_EYE_2, TRADECRAFT_2,
                    FOCUSED_OPTICS_3, MOMENTUM, RICOCHET, MARKSMANS_EYE_3, HOLLOW_POINT, FOURTH_SHOT, SILVER_ROUNDS,
                    RAILGUN, EXECUTIONER, HEADHUNTER, UNBROKEN_AIM, SUNDER_ROUNDS, SHATTER_SHOT, SPOTTER_UPLINK)
            .withCluster(UpgradeCluster.of("Discipline", FOCUSED_OPTICS_1, MARKSMANS_EYE_1))
            .withCluster(UpgradeCluster.of("Special", MOMENTUM, RICOCHET))
            .withCluster(UpgradeCluster.of("Special", HOLLOW_POINT, FOURTH_SHOT))
            .withCluster(UpgradeCluster.of("Mastery", RAILGUN, EXECUTIONER))
            .withCluster(UpgradeCluster.of("Mastery", UNBROKEN_AIM, SUNDER_ROUNDS));

    private static final MechanicCatalogue<SniperMechanic> MECHANICS = MechanicCatalogue.<SniperMechanic>empty()
            .with(FOCUSED_OPTICS_3.id(), WeakSpotMechanic::new)
            .with(RAILGUN.id(), RailgunMechanic::new)
            .with(EXECUTIONER.id(), ExecutionerMechanic::new)
            .with(TRADECRAFT_1.id(), CritStreakMechanic::new)
            .with(SPOTTER_UPLINK.id(), SpotterUplinkMechanic::new)
            .with(RICOCHET.id(), RicochetMechanic::new);

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK, this.restHeading());
    private final ShotActions actions = new Actions();
    private final OwnedMechanics<SniperMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private AimFocus focus = new AimFocus();
    private FireClock clock = new FireClock(COOLDOWN_MAX);
    private final List<Bullet> bullets = new ArrayList<>();
    private SniperShot shotLanding;
    private TowerStats statsLanding;
    private EnemyMob currentTarget;
    private ShotTrace lastShot;
    private int tickNow;

    public SniperTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SNIPER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .withCritChance(CRIT_CHANCE).withCritMultiplier(CRIT_MULTIPLIER)
                .with(TowerStat.DEAD_ZONE, 0f).with(TowerStat.AIM_STACKS, 1f), context, x, y);
    }

    @Override
    protected OwnedMechanics<SniperMechanic> ownedMechanics() {
        return this.mechanics;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
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
        this.tickNow = gameTime;
        this.focus.advance();
        this.landArrived();
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
        } else if (this.betweenWaves()) {
            this.turretAim.settle();
        }
    }

    @Override
    protected void endFight() {
        this.focus = new AimFocus();
        this.clock = new FireClock(COOLDOWN_MAX);
        this.bullets.clear();
        this.shotLanding = null;
        this.statsLanding = null;
        this.currentTarget = null;
    }

    @Override
    protected void takeRestPose() {
        this.turretAim.rest();
    }

    /** One shot: shaped by the owned mechanics and sent on its way, and the wait that follows. */
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
        double heading = TurretAim.angleTo(this.centerX, this.centerY, target.getX(), target.getY());
        double distance = Math.hypot(target.getX() - this.centerX, target.getY() - this.centerY);
        int flight = Math.max(MIN_FLIGHT_TICKS,
                (int) Math.ceil(distance / (BULLET_CELLS_PER_TICK * this.context.getBoard().scale())));
        float reach = shot.piercing() ? (float) this.railReach(shot.pierce()) : 0f;
        this.bullets.add(new Bullet(shot, stats, target, new Flight(heading, reach, this.tickNow, this.tickNow + flight)));
        double rate = this.attackRate();
        this.clock.spend(rate / (rate + shot.attackSpeedBonus()));
    }

    /** Lands every bullet whose flight ends now. */
    private void landArrived() {
        while (!this.bullets.isEmpty() && this.bullets.getFirst().landsAt() <= this.tickNow) {
            this.land(this.bullets.removeFirst());
        }
    }

    /**
     * A bullet arrives with the stats it was fired with: a piercing one runs its line, an aimed one hits
     * its target, or is lost when the target is gone; then what landed is settled and reacted to.
     */
    private void land(Bullet bullet) {
        EnemyMob target = bullet.target();
        SniperShot shot = bullet.shot();
        this.shotLanding = shot;
        this.statsLanding = bullet.stats();
        if (!shot.piercing() && target.isDead()) {
            this.lastShot = this.traceOf(bullet, false);
            return;
        }
        HitResult hit = shot.piercing() ? this.pierce(bullet) : this.land(shot.aimed(), target, 1f, bullet.stats());
        if (hit.dodged()) {
            this.reportDodge(target, HitKind.DIRECT);
            this.lastShot = this.traceOf(bullet, false);
        } else {
            this.settle(new ShotResult(target, hit.critical(), target.isDead()), bullet, this.mechanics.all());
        }
    }

    /** A shot that landed: the mechanics decide what it counts as, the procs hear of it, then the mechanics react. */
    private void settle(ShotResult landed, Bullet bullet, List<SniperMechanic> owned) {
        ShotResult result = landed;
        for (SniperMechanic mechanic : owned) {
            result = mechanic.settle(result, this.actions);
        }
        this.report(result.target(), HitKind.DIRECT, result.critical(), result.killed());
        this.lastShot = this.traceOf(bullet, result.critical());
        for (SniperMechanic mechanic : owned) {
            mechanic.react(result, this.actions);
        }
    }

    /** Where the shot went: to its target, or for a piercing shot to the end of its line. */
    private ShotTrace traceOf(Bullet bullet, boolean critical) {
        DamageType type = bullet.shot().type();
        if (!bullet.piercing()) {
            return new ShotTrace(new Vec2(bullet.target().getX(), bullet.target().getY()), critical, type,
                    this.tickNow);
        }
        return new ShotTrace(new Vec2(this.centerX + Math.cos(bullet.heading()) * bullet.reach(),
                this.centerY + Math.sin(bullet.heading()) * bullet.reach()), critical, type, this.tickNow);
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
     * A shot through everything on the line it was fired along, as the line stands when it arrives;
     * each enemy after the first it passes takes less. A line is not aimed, so nothing on it dodges.
     * What it did to the enemy it was aimed at.
     */
    private HitResult pierce(Bullet bullet) {
        float scale = this.context.getBoard().scale();
        SniperShot shot = bullet.shot();
        double heading = bullet.heading();
        List<EnemyMob> line = new OnSegmentTargetQuery(this.centerX, this.centerY,
                this.centerX + Math.cos(heading) * bullet.reach(), this.centerY + Math.sin(heading) * bullet.reach(),
                shot.pierce().halfWidthCells() * scale).matching(this.context.enemies());
        HitResult aimedHit = HitResult.none();
        float share = 1f;
        for (EnemyMob enemy : line) {
            HitResult hit = this.land(shot, enemy, share, bullet.stats());
            if (enemy == bullet.target()) {
                aimedHit = hit;
            }
            share *= shot.pierce().falloff();
        }
        return aimedHit;
    }

    /**
     * A bounce hits the nearest enemy beside the last one struck that this shot has not hit yet. Each
     * bounce is aimed, so each may be dodged; the next bounce goes on from the enemy that dodged.
     */
    private void ricochet(EnemyMob from, int bounces, float share, float reachCells) {
        SniperShot bounce = this.shotLanding.withDamageFactor(share).aimed();
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
            this.land(bounce, next.get(), 1f, this.statsLanding);
            struck.add(next.get());
            last = next.get();
        }
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    /** The last shot that arrived, for drawing its trace; empty before the first. */
    public Optional<ShotTrace> lastShot() {
        return Optional.ofNullable(this.lastShot);
    }

    /** The bullets on their way, oldest first. */
    public List<Bullet> bulletsInFlight() {
        return List.copyOf(this.bullets);
    }

    /** How many Steady Aim stacks the next shot has, whichever enemy it hits; empty while no bonus counts them. */
    public OptionalInt steadyAimStacks() {
        if (this.upgrades().hitBonuses().stream()
                .noneMatch(bonus -> bonus.scaling().focusCounter().filter(TowerTerm.STEADY_AIM::equals).isPresent())) {
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
                    .map(bonus -> bonus.adjustment().describe()).toList());
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
     * @param to       where its line ends, in pixels
     * @param critical whether it crit its target
     * @param type     what damage it dealt
     * @param landedAt the tick it arrived
     */
    public record ShotTrace(Vec2 to, boolean critical, DamageType type, int landedAt) {

        public float toX() {
            return (float) this.to.x();
        }

        public float toY() {
            return (float) this.to.y();
        }
    }

    /**
     * A shot on its way. It carries the stats it was fired with, so a charge spent on it is not lost
     * however the tower changes before it lands.
     *
     * @param shot   the shot as the mechanics shaped it
     * @param stats  the stats it was fired with
     * @param target the enemy it was aimed at
     * @param flight where and when it flies
     */
    public record Bullet(SniperShot shot, TowerStats stats, EnemyMob target, Flight flight) {

        public boolean piercing() {
            return this.shot.piercing();
        }

        public double heading() {
            return this.flight.heading();
        }

        public float reach() {
            return this.flight.reach();
        }

        public int firedAt() {
            return this.flight.firedAt();
        }

        public int landsAt() {
            return this.flight.landsAt();
        }

        /** Whether it is sure to crit, which shows before it lands. */
        public boolean sureCrit() {
            return this.stats.has(TowerCapability.GUARANTEED_CRIT);
        }
    }

    /**
     * A bullet's way to its enemy.
     *
     * @param heading the direction it was fired in, in radians
     * @param reach   how far a piercing shot's line runs, in pixels; {@code 0} for an aimed one
     * @param firedAt the tick it was fired
     * @param landsAt the tick it arrives
     */
    public record Flight(double heading, float reach, int firedAt, int landsAt) {
    }

    /** What the mechanics may make this tower do. */
    private final class Actions implements ShotActions {

        @Override
        public boolean execute(EnemyMob target) {
            AttackProfile finisher = AttackProfile.none().pastArmorAndPlating().asPeriodic();
            SniperTower.this.dealDamage(target, Damage.physical(EXECUTION_DAMAGE), finisher);
            return target.isDead();
        }

        @Override
        public void ricochet(EnemyMob from, int bounces, float share, float reachCells) {
            SniperTower.this.ricochet(from, bounces, share, reachCells);
        }
    }
}
