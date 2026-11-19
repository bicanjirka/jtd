package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.StackingDebuff;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.stat.StatModifiers;
import td.term.TermText;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitBonus;
import td.tower.proc.HitKind;
import td.tower.proc.HitScaling;
import td.tower.proc.Proc;
import td.tower.proc.ProcPayload;
import td.tower.sniper.AimFocus;
import td.tower.sniper.AimLock;
import td.tower.sniper.Bullet;
import td.tower.sniper.Execution;
import td.tower.sniper.ExecutionerMechanic;
import td.tower.sniper.HeadhunterMechanic;
import td.tower.sniper.Hop;
import td.tower.sniper.RicochetMechanic;
import td.tower.sniper.ShotActions;
import td.tower.sniper.ShotResult;
import td.tower.sniper.ShotTrace;
import td.tower.sniper.SniperAim;
import td.tower.sniper.SniperMechanic;
import td.tower.sniper.SniperShot;
import td.tower.sniper.SniperSpec;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.TargetSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.RangeNodes;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static td.tower.upgrade.MechanicCatalogue.entry;

/**
 * The rhythmic single-target tower: once it owns Steady Aim, each shot builds on the last, whichever enemy it hits, until
 * it stops firing for a while. It fires at the visible enemy in range furthest along the path, and its crits are what
 * its upgrades pay out.
 * The turret turns at a capped rate and holds its heading when idle.
 * <p>
 * What each owned node does lives in its data or in a {@link SniperMechanic}: the tower shapes every shot through them
 * and lets them react to how it landed.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SniperTower extends AbstractTower {

    public static final int PRICE = 15;
    public static final float DAMAGE_POINTS = 32f;
    public static final float RANGE = 3.0f;
    public static final float CRIT_CHANCE = 0f;
    public static final float CRIT_MULTIPLIER = 1.5f;
    private static final double MAX_TURN_RADIANS_PER_TICK = 0.4;
    /** A shot every 2.5 s. */
    private static final int COOLDOWN_MAX = 49;

    /** More damage than any enemy has health, so that no reduction leaves an executed enemy alive. */
    private static final int EXECUTION_DAMAGE = 1_000_000_000;
    /** How far a bullet flies in a tick, in cells: fast, yet its travel is seen. */
    private static final float BULLET_CELLS_PER_TICK = 2f;
    private static final int MIN_FLIGHT_TICKS = 2;
    /** The shot every Fourth Shot node primes. */
    private static final int FOURTH = 4;
    /** What a counted shot can be, as its bullet and the rifle show it. */
    private static final List<TowerTerm> ROUNDS = List.of(TowerTerm.FOURTH_SHOT, TowerTerm.SILVER_ROUND,
            TowerTerm.HOLLOW_POINT);

    /** Reads the same whichever node grants it. */
    private static final ProcPayload GRANTS_FRENZY = new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.FRENZY, 3,
            TowerModifiers.attackSpeed(0.35f)).lengthenedByBuffs());

    private static final HitBonus STEADY_AIM_BONUS = perSteadyAimStack(
            HitAdjustment.attackSpeed(0.04f).plus(HitAdjustment.critChance(0.02f)));

    private static final UpgradeNode STEADY_AIM = UpgradeNode.of("sniper.steady_aim", "Steady Aim", 5)
            .withModifiers(TowerModifiers.aimStacks(5))
            .withHitBonus(STEADY_AIM_BONUS);
    private static final UpgradeNode RIFLING = UpgradeNode.of("sniper.rifling", "Rifling", 2)
            .withModifiers(TowerModifiers.critChance(0.05f));
    private static final UpgradeNode LONG_BARREL = UpgradeNode.of(RangeNodes.RANGE_ID, "Long Barrel", 5)
            .withModifiers(TowerModifiers.range(1f));
    private static final UpgradeNode HEAVY_ROUNDS = UpgradeNode.of("sniper.heavy_rounds", "Heavy Rounds", 9)
            .withModifiers(TowerModifiers.damage(12f));

    private static final UpgradeNode FOURTH_SHOT = UpgradeNode.of("sniper.fourth_shot", "Fourth Shot", 7)
            .withProc(everyFourthShotIs(TowerStatus.nextShots(TowerTerm.FOURTH_SHOT, 1, StatModifiers.none())
                    .withCapability(TowerCapability.GUARANTEED_CRIT)))
            .inTier(1);
    private static final UpgradeNode DEEP_BREATH = UpgradeNode.of("sniper.deep_breath", "Deep Breath", 3)
            .withModifiers(TowerModifiers.aimStacks(3))
            .after(STEADY_AIM)
            .inTier(1);
    private static final UpgradeNode KEEN_EYE = UpgradeNode.of("sniper.keen_eye", "Keen Eye", 3)
            .withHitBonus(perSteadyAimStack(HitAdjustment.critChance(0.02f)))
            .after(STEADY_AIM)
            .inTier(1);
    private static final UpgradeNode KILLSHOT = UpgradeNode.of("sniper.killshot", "Killshot", 5)
            .withModifiers(TowerModifiers.critMultiplier(0.5f))
            .inTier(1);

    private static final UpgradeNode FRENZY = UpgradeNode.of("sniper.frenzy", "Frenzy", 8)
            .withProc(Proc.onCrit(GRANTS_FRENZY))
            .inTier(2);
    private static final UpgradeNode RICOCHET = UpgradeNode.of("sniper.ricochet", "Ricochet", 8)
            .inTier(2);
    private static final UpgradeNode SUNDERING_CRITS = UpgradeNode.of("sniper.sundering_crits", "Sundering Crits", 8)
            .withProc(Proc.onCrit(new ProcPayload.ApplyStacks(StackingDebuff.SUNDERED, 2)))
            .inTier(2);
    private static final UpgradeNode SILVER_ROUND = UpgradeNode.of("sniper.silver_round", "Silver Round", 11)
            .withProc(everyFourthShotIs(TowerStatus.nextShots(TowerTerm.SILVER_ROUND, 1,
                    TowerModifiers.magicPenetrationFlat(50f)).withCapability(TowerCapability.MAGIC_DAMAGE)))
            .after(FOURTH_SHOT)
            .inTier(2);
    private static final UpgradeNode HOLLOW_POINT = UpgradeNode.of("sniper.hollow_point", "Hollow Point", 11)
            .withProc(everyFourthShotIs(TowerStatus.nextShots(TowerTerm.HOLLOW_POINT, 1,
                    TowerModifiers.damagePercent(0.33f))))
            .after(FOURTH_SHOT)
            .inTier(2);
    private static final UpgradeNode LONG_BARREL_2 = UpgradeNode.of(RangeNodes.RANGE_2_ID, "Long Barrel II", 15)
            .withModifiers(TowerModifiers.range(1f))
            .inTier(2);

    private static final UpgradeNode HEAVY_ROUNDS_2 = UpgradeNode.of("sniper.heavy_rounds.2", "Heavy Rounds II", 27)
            .withModifiers(TowerModifiers.damage(12f))
            .inTier(3);
    private static final UpgradeNode EXECUTIONER = UpgradeNode.of("sniper.executioner", "Executioner", 6)
            .inTier(3);
    private static final UpgradeNode HEADHUNTER = UpgradeNode.of("sniper.headhunter", "Headhunter", 6)
            .inTier(3);
    private static final UpgradeNode UNBROKEN_AIM = UpgradeNode.of("sniper.unbroken_aim", "Unbroken Aim", 6)
            .withModifiers(TowerModifiers.aimStacksPerShot(1))
            .after(STEADY_AIM)
            .inTier(3);
    private static final UpgradeNode MOMENTUM = UpgradeNode.of("sniper.momentum", "Momentum", 9)
            .withProc(Proc.onKill(GRANTS_FRENZY))
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(STEADY_AIM, RIFLING, LONG_BARREL, HEAVY_ROUNDS))
            .with(FOURTH_SHOT, DEEP_BREATH, KEEN_EYE, KILLSHOT,
                    FRENZY, RICOCHET, SUNDERING_CRITS, SILVER_ROUND, HOLLOW_POINT, LONG_BARREL_2,
                    HEAVY_ROUNDS_2, EXECUTIONER, HEADHUNTER, UNBROKEN_AIM, MOMENTUM)
            .withCluster(UpgradeCluster.of("Steady Aim", DEEP_BREATH, KEEN_EYE))
            .withCluster(UpgradeCluster.of("On crit", FRENZY, RICOCHET, SUNDERING_CRITS))
            .withCluster(UpgradeCluster.of("Ammo", SILVER_ROUND, HOLLOW_POINT))
            .withCluster(UpgradeCluster.of("Finisher", EXECUTIONER, HEADHUNTER));

    private static final MechanicCatalogue<SniperMechanic> MECHANICS = MechanicCatalogue.of(
            entry(RICOCHET.id(), RicochetMechanic::new),
            entry(EXECUTIONER.id(), ExecutionerMechanic::new),
            entry(HEADHUNTER.id(), HeadhunterMechanic::new));

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK, this.restHeading());
    private final OwnedMechanics<SniperMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private AimFocus focus = new AimFocus();
    private FireClock clock = new FireClock(COOLDOWN_MAX);
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Hop> hops = new ArrayList<>();
    private Optional<EnemyMob> target = Optional.empty();
    private Optional<ShotTrace> lastShot = Optional.empty();
    private Optional<Execution> lastExecution = Optional.empty();
    private OptionalInt firedAt = OptionalInt.empty();

    public SniperTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SNIPER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .withCritChance(CRIT_CHANCE).withCritMultiplier(CRIT_MULTIPLIER)
                .with(TowerStat.AIM_STACKS, 0f).with(TowerStat.AIM_STACKS_PER_SHOT, 1f), context, x, y);
    }

    private static HitBonus perSteadyAimStack(HitAdjustment adjustment) {
        return HitBonus.per(new HitScaling.PerFocus(TowerTerm.STEADY_AIM, 0), adjustment);
    }

    private static boolean growsWithSteadyAim(HitBonus bonus) {
        return bonus.scaling().focusCounter().equals(Optional.of(TowerTerm.STEADY_AIM));
    }

    /** Every fourth shot is {@code round}. */
    private static Proc everyFourthShotIs(TowerStatus round) {
        return Proc.everyNthShot(FOURTH, new ProcPayload.GrantStatus(round));
    }

    @Override
    protected OwnedMechanics<SniperMechanic> ownedMechanics() {
        return this.mechanics;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /** Whom it may shoot and how it picks. */
    private SniperSpec spec() {
        Viewpoint view = this.viewpoint();
        SniperSpec spec = SniperSpec.from(view);
        return this.upgrades().targeting()
                .map(priority -> spec.aimingAt(new SniperAim(priority.selector(view.cellSize()), priority.label())))
                .orElse(spec);
    }

    private Optional<EnemyMob> findEnemy(SniperSpec spec) {
        TargetSelector selector = PreferringSelector.priority(spec.aim().selector());
        return selector.selectFrom(spec.reach().matching(this.context.enemies()));
    }

    @Override
    protected void act(int gameTime) {
        this.focus.advance();
        this.landArrived();
        if (this.clock.isReady()) {
            this.target = this.findEnemy(this.spec());
            this.target.ifPresent(enemy -> this.fire(enemy, this.mechanics.all()));
        }
        this.clock.advance(this.attackRate());
        this.turnTurret(this.turretAim, this.target);
    }

    @Override
    protected void endFight() {
        this.focus = new AimFocus();
        this.clock = new FireClock(COOLDOWN_MAX);
        this.bullets.clear();
        this.hops.clear();
        this.target = Optional.empty();
        this.lastExecution = Optional.empty();
    }

    @Override
    protected void takeRestPose() {
        this.turretAim.rest();
    }

    /** One shot: shaped by the owned mechanics and sent on its way, and the wait that follows. */
    private void fire(EnemyMob target, List<SniperMechanic> owned) {
        List<TowerTerm> rounds = ROUNDS.stream().filter(this::primes).toList();
        TowerStats stats = this.beginShot();
        AimLock lock = this.focus.lock(target, this.aimStackCap(), this.aimStacksPerShot());
        float rangeShare = (float) (Math.hypot(target.getX() - this.centerX, target.getY() - this.centerY)
                / this.rangeReal());
        HitAdjustment adjustment = this.hitAdjustment(target, HitKind.DIRECT, rangeShare, stats, lock.stacks(),
                lock.fresh());
        SniperShot shot = SniperShot.of(adjustment.applyTo(stats.attack())).scaledBy(adjustment.damageFactor())
                .withAttackSpeedBonus(adjustment.attackSpeed()).asRounds(rounds);
        if (stats.has(TowerCapability.MAGIC_DAMAGE)) {
            shot = shot.withType(DamageType.MAGIC);
        }
        for (SniperMechanic mechanic : owned) {
            shot = mechanic.shape(shot, target);
        }
        this.bullets.add(new Bullet(shot, stats, target, this.flightFrom(this.centerX, this.centerY, target)));
        this.firedAt = OptionalInt.of(this.tickNow());
        // A bonus this shot carries hastens only the wait after it: that wait is the period at the faster rate, as a
        // share of the period the clock counts at the tower's own rate.
        double rate = this.attackRate();
        this.clock.spend(rate / (rate + shot.attackSpeedBonus()));
    }

    /** A flight from {@code x, y} to {@code target}, starting now. */
    private ShotFlight flightFrom(double x, double y, EnemyMob target) {
        return ShotFlight.across(this.tickNow(), Math.hypot(target.getX() - x, target.getY() - y),
                BULLET_CELLS_PER_TICK * this.cellPixels(), MIN_FLIGHT_TICKS);
    }

    /** Lands every hop and every bullet whose flight ends now. */
    private void landArrived() {
        Arrivals.takeDue(this.hops, hop -> hop.bullet().landsAt(), this.tickNow()).forEach(this::landHop);
        Arrivals.takeDue(this.bullets, Bullet::landsAt, this.tickNow()).forEach(this::land);
    }

    /**
     * A bullet arrives with the stats it was fired with: it hits its target, or is lost when the target is gone; then
     * what landed is settled and reacted to.
     */
    private void land(Bullet bullet) {
        EnemyMob target = bullet.target();
        if (target.isDead()) {
            this.lastShot = Optional.of(this.traceOf(bullet, false));
            return;
        }
        HitResult hit = this.strike(bullet);
        if (hit.dodged()) {
            this.lastShot = Optional.of(this.traceOf(bullet, false));
        } else {
            this.settle(new ShotResult(target, hit.critical(), target.isDead()), bullet, this.mechanics.all());
        }
    }

    /** A hop lands as the shot it came from, unable to crit, and nothing triggers off it. */
    private void landHop(Hop hop) {
        if (!hop.bullet().target().isDead()) {
            this.strike(hop.bullet());
        }
    }

    /** A shot that landed: the mechanics decide what it counts as, the procs hear of it, then the mechanics react. */
    private void settle(ShotResult landed, Bullet bullet, List<SniperMechanic> owned) {
        ShotActions actions = new Actions(bullet);
        ShotResult result = landed;
        for (SniperMechanic mechanic : owned) {
            result = mechanic.settle(result, actions);
        }
        this.report(result.target(), HitKind.DIRECT, result.critical(), result.killed());
        this.lastShot = Optional.of(this.traceOf(bullet, result.critical()));
        for (SniperMechanic mechanic : owned) {
            mechanic.react(result, actions);
        }
    }

    private ShotTrace traceOf(Bullet bullet, boolean critical) {
        return new ShotTrace(bullet.target().position(), critical, bullet.shot(), this.tickNow());
    }

    /** The bullet's hit on its target, with the stats it was fired with. */
    private HitResult strike(Bullet bullet) {
        SniperShot shot = bullet.shot().aimed();
        return this.landHit(bullet.target(), Damage.of(shot.type(), shot.amountOn(bullet.stats().damage())),
                shot.attack());
    }

    public EnemyMob getCurrentTarget() {
        return this.target.orElse(null);
    }

    /** The last shot that arrived, for drawing its trace; empty before the first. */
    public Optional<ShotTrace> lastShot() {
        return this.lastShot;
    }

    /** The ticks since its last shot left, while they are within {@code window}. */
    public OptionalInt ticksSinceFired(int gameTime, int window) {
        return within(this.firedAt, gameTime, window);
    }

    /** The last enemy an execution killed, for drawing its moment; empty before the first. */
    public Optional<Execution> lastExecution() {
        return this.lastExecution;
    }

    /** Whether the next shot is {@code round}, one of the counted shots its nodes prime. */
    public boolean nextShotIs(TowerTerm round) {
        return this.primes(round);
    }

    /** Whether Frenzy runs now. */
    public boolean frenzied() {
        return this.holdsStatus(TowerTerm.FRENZY);
    }

    /** Whether a mechanic it owns singles out the enemy it aims at now. */
    public boolean huntsItsTarget() {
        return this.target.filter(enemy -> !enemy.isDead())
                .filter(enemy -> this.mechanics.all().stream().anyMatch(mechanic -> mechanic.hunts(enemy)))
                .isPresent();
    }

    /** The bullets on their way, oldest first. */
    public List<Bullet> bulletsInFlight() {
        return List.copyOf(this.bullets);
    }

    /** The ricochets on their way, oldest first. */
    public List<Hop> hopsInFlight() {
        return List.copyOf(this.hops);
    }

    /** How many Steady Aim stacks the next shot has, whichever enemy it hits. */
    public int steadyAimStacks() {
        return Math.min(this.aimStackCap(), this.focus.stacks());
    }

    /** Whether its focus is full and a hit bonus beyond Steady Aim's own grows with every stack of it. */
    public boolean keenAtFullFocus() {
        return this.focus.stacks() >= this.aimStackCap()
                && this.upgrades().hitBonuses().stream()
                        .anyMatch(bonus -> growsWithSteadyAim(bonus) && !bonus.equals(STEADY_AIM_BONUS));
    }

    private int aimStackCap() {
        return Math.round(this.stats().value(TowerStat.AIM_STACKS));
    }

    private int aimStacksPerShot() {
        return Math.round(this.stats().value(TowerStat.AIM_STACKS_PER_SHOT));
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
        int stacks = this.steadyAimStacks();
        if (stacks > 0) {
            List<TermText> parts = new ArrayList<>(this.upgrades().hitBonuses().stream()
                    .filter(SniperTower::growsWithSteadyAim)
                    .map(bonus -> bonus.adjustment().describe())
                    .toList());
            parts.add(TermText.of("up to " + this.aimStackCap() + " stacks"));
            states.add(TowerBuffState.of(TowerTerm.STEADY_AIM, TermText.join("; ", parts)).withStacks(stacks)
                    .withClock(this.focus.ticksUntilFade(), AimFocus.STACK_TICKS));
        }
        this.mechanics.all().forEach(mechanic -> mechanic.condition().ifPresent(states::add));
        return states;
    }

    @Override
    protected String targetingLabel() {
        return this.spec().aim().label();
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSniperTower(this);
    }

    /** What the mechanics may make this tower do about the bullet that just landed. */
    private final class Actions implements ShotActions {

        private final Bullet landed;

        private Actions(Bullet landed) {
            this.landed = landed;
        }

        @Override
        public boolean execute(EnemyMob target) {
            AttackProfile finisher = AttackProfile.none().pastArmorAndPlating().asPeriodic();
            SniperTower.this.dealDamage(target, Damage.physical(EXECUTION_DAMAGE), finisher);
            if (target.isDead()) {
                SniperTower.this.lastExecution = Optional.of(new Execution(target.position(),
                        SniperTower.this.tickNow(), target));
            }
            return target.isDead();
        }

        @Override
        public void ricochet(EnemyMob from, float reachCells) {
            float reach = reachCells * SniperTower.this.cellPixels();
            List<EnemyMob> beside = new ArrayList<>(InRangeTargetQuery.visible(from.position(), reach)
                    .matching(SniperTower.this.context.enemies()));
            beside.remove(from);
            new NearestSelector(from.getX(), from.getY()).selectFrom(beside).ifPresent(next -> {
                SniperShot hop = this.landed.shot().withAttack(AttackProfile::withoutCrits).asRounds(List.of());
                SniperTower.this.hops.add(new Hop(from.position(), new Bullet(hop, this.landed.stats(), next,
                        SniperTower.this.flightFrom(from.getX(), from.getY(), next)), this.landed.shot().rounds()));
            });
        }
    }
}
