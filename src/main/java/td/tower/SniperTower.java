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
import td.tower.sniper.ExecutionerMechanic;
import td.tower.sniper.HeadhunterMechanic;
import td.tower.sniper.RicochetMechanic;
import td.tower.sniper.ShotActions;
import td.tower.sniper.ShotContext;
import td.tower.sniper.ShotResult;
import td.tower.sniper.SniperAim;
import td.tower.sniper.SniperMechanic;
import td.tower.sniper.SniperShot;
import td.tower.sniper.SniperSpec;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.TargetPriority;
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
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The rhythmic single-target tower: each shot builds Steady Aim, whichever enemy it hits, until it stops firing for a
 * while. It fires at the visible enemy in range furthest along the path, and its crits are what its upgrades pay out.
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
    private static final int STEADY_AIM_STACKS = 5;

    /** Enough to kill anything, so that an execution takes exactly what health is left. */
    private static final int EXECUTION_DAMAGE = 1_000_000_000;
    /** How far a bullet flies in a tick, in cells: fast, yet its travel is seen. */
    private static final float BULLET_CELLS_PER_TICK = 2f;
    private static final int MIN_FLIGHT_TICKS = 2;
    /** The shot every Fourth Shot node primes. */
    private static final int FOURTH = 4;

    private static final List<HitBonus> STEADY_AIM = List.of(HitBonus.per(new HitScaling.PerFocus(TowerTerm.STEADY_AIM, 0),
            HitAdjustment.attackSpeed(0.04f).plus(HitAdjustment.critChance(0.02f))));

    /** Reads the same whichever node grants it. */
    private static final ProcPayload FRENZY = new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.FRENZY, 3,
            TowerModifiers.attackSpeed(0.35f)).lengthenedByBuffs());

    private static final UpgradeNode RIFLING = UpgradeNode.of("sniper.rifling", "Rifling", 1)
            .withModifiers(TowerModifiers.critChance(0.05f));
    private static final UpgradeNode LONG_BARREL = UpgradeNode.of(RangeNodes.RANGE_ID, "Long Barrel", 10)
            .withModifiers(TowerModifiers.range(1f));
    private static final UpgradeNode HEAVY_ROUNDS = UpgradeNode.of("sniper.heavy_rounds", "Heavy Rounds", 11)
            .withModifiers(TowerModifiers.damage(12f));

    private static final UpgradeNode FOURTH_SHOT = UpgradeNode.of("sniper.special.fifth_shot", "Fourth Shot", 6)
            .withProc(Proc.everyNthShot(FOURTH, new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.FOURTH_SHOT,
                    1, StatModifiers.none()).withCapability(TowerCapability.GUARANTEED_CRIT))))
            .inTier(1);
    private static final UpgradeNode DEEP_BREATH = UpgradeNode.of("sniper.deep_breath", "Deep Breath", 5)
            .withModifiers(TowerModifiers.aimStacks(3))
            .inTier(1);
    private static final UpgradeNode KEEN_EYE = UpgradeNode.of("sniper.head.marksmans_eye.1", "Keen Eye", 5)
            .withHitBonus(HitBonus.per(new HitScaling.PerFocus(TowerTerm.STEADY_AIM, 0), HitAdjustment.critChance(0.02f)))
            .inTier(1);
    private static final UpgradeNode KILLSHOT = UpgradeNode.of("sniper.special.shatter_shot", "Killshot", 7)
            .withModifiers(TowerModifiers.critMultiplier(0.5f))
            .inTier(1);

    private static final UpgradeNode FRENZY_NODE = UpgradeNode.of("sniper.head.focused_optics.2", "Frenzy", 10)
            .withProc(Proc.onCrit(FRENZY))
            .inTier(2);
    private static final UpgradeNode RICOCHET = UpgradeNode.of("sniper.special.ricochet", "Ricochet", 10)
            .inTier(2);
    private static final UpgradeNode SUNDERING_CRITS = UpgradeNode.of("sniper.head.marksmans_eye.4b", "Sundering Crits", 10)
            .withProc(Proc.onCrit(new ProcPayload.ApplyStacks(StackingDebuff.SUNDERED, 2)))
            .inTier(2);
    private static final UpgradeNode SILVER_ROUND = UpgradeNode.of("sniper.extra.tradecraft.3", "Silver Round", 5)
            .withProc(Proc.everyNthShot(FOURTH, new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.SILVER_ROUND,
                    1, TowerModifiers.magicPenetrationFlat(50f)).withCapability(TowerCapability.MAGIC_DAMAGE))))
            .after(FOURTH_SHOT)
            .inTier(2);
    private static final UpgradeNode HOLLOW_POINT = UpgradeNode.of("sniper.special.hollow_point", "Hollow Point", 5)
            .withProc(Proc.everyNthShot(FOURTH, new ProcPayload.GrantStatus(TowerStatus.nextShots(TowerTerm.HOLLOW_POINT,
                    1, TowerModifiers.damagePercent(0.33f)))))
            .after(FOURTH_SHOT)
            .inTier(2);
    private static final UpgradeNode LONG_BARREL_2 = UpgradeNode.of(RangeNodes.RANGE_2_ID, "Long Barrel II", 18)
            .withModifiers(TowerModifiers.range(1f))
            .inTier(2);

    private static final UpgradeNode HEAVY_ROUNDS_2 = UpgradeNode.of("sniper.heavy_rounds.2", "Heavy Rounds II", 23)
            .withModifiers(TowerModifiers.damage(12f))
            .inTier(3);
    private static final UpgradeNode EXECUTIONER = UpgradeNode.of("sniper.head.focused_optics.4b", "Executioner", 6)
            .inTier(3);
    private static final UpgradeNode HEADHUNTER = UpgradeNode.of("sniper.special.headhunter", "Headhunter", 6)
            .inTier(3);
    private static final UpgradeNode UNBROKEN_AIM = UpgradeNode.of("sniper.head.marksmans_eye.4a", "Unbroken Aim", 1)
            .withModifiers(TowerModifiers.aimStacksPerShot(1))
            .inTier(3);
    private static final UpgradeNode MOMENTUM = UpgradeNode.of("sniper.special.momentum", "Momentum", 9)
            .withProc(Proc.onKill(FRENZY))
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(RIFLING, LONG_BARREL, HEAVY_ROUNDS))
            .with(FOURTH_SHOT, DEEP_BREATH, KEEN_EYE, KILLSHOT,
                    FRENZY_NODE, RICOCHET, SUNDERING_CRITS, SILVER_ROUND, HOLLOW_POINT, LONG_BARREL_2,
                    HEAVY_ROUNDS_2, EXECUTIONER, HEADHUNTER, UNBROKEN_AIM, MOMENTUM)
            .withCluster(UpgradeCluster.of("Steady Aim", DEEP_BREATH, KEEN_EYE))
            .withCluster(UpgradeCluster.of("On crit", FRENZY_NODE, RICOCHET, SUNDERING_CRITS))
            .withCluster(UpgradeCluster.of("Ammo", SILVER_ROUND, HOLLOW_POINT))
            .withCluster(UpgradeCluster.of("Finisher", EXECUTIONER, HEADHUNTER));

    private static final MechanicCatalogue<SniperMechanic> MECHANICS = MechanicCatalogue.<SniperMechanic>empty()
            .with(RICOCHET.id(), RicochetMechanic::new)
            .with(EXECUTIONER.id(), ExecutionerMechanic::new)
            .with(HEADHUNTER.id(), HeadhunterMechanic::new);

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK, this.restHeading());
    private final ShotActions actions = new Actions();
    private final OwnedMechanics<SniperMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private AimFocus focus = new AimFocus();
    private FireClock clock = new FireClock(COOLDOWN_MAX);
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Hop> hops = new ArrayList<>();
    private Bullet landing;
    private EnemyMob currentTarget;
    private ShotTrace lastShot;
    private int tickNow;

    public SniperTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SNIPER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .withCritChance(CRIT_CHANCE).withCritMultiplier(CRIT_MULTIPLIER)
                .with(TowerStat.AIM_STACKS, STEADY_AIM_STACKS).with(TowerStat.AIM_STACKS_PER_SHOT, 1f), context, x, y);
    }

    @Override
    protected OwnedMechanics<SniperMechanic> ownedMechanics() {
        return this.mechanics;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected List<HitBonus> baseHitBonuses() {
        return STEADY_AIM;
    }

    /** Whom it may shoot and how it picks. */
    private SniperSpec spec() {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        SniperSpec spec = SniperSpec.from(view);
        Optional<TargetPriority> priority = this.upgrades().targeting();
        if (priority.isPresent()) {
            spec = spec.aimingAt(new SniperAim(priority.get().selector(view.cellSize()), priority.get().label()));
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
            this.currentTarget = this.findEnemy(this.spec());
            if (this.currentTarget != null) {
                this.fire(this.currentTarget, this.mechanics.all());
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
        this.hops.clear();
        this.landing = null;
        this.currentTarget = null;
    }

    @Override
    protected void takeRestPose() {
        this.turretAim.rest();
    }

    /** One shot: shaped by the owned mechanics and sent on its way, and the wait that follows. */
    private void fire(EnemyMob target, List<SniperMechanic> owned) {
        TowerStats stats = this.beginShot();
        AimLock lock = this.focus.lock(target, this.aimStackCap(), this.aimStacksPerShot());
        float rangeShare = (float) (Math.hypot(target.getX() - this.centerX, target.getY() - this.centerY)
                / this.rangeReal());
        HitAdjustment adjustment = this.hitAdjustment(target, HitKind.DIRECT, rangeShare, stats, lock.stacks(),
                lock.fresh());
        SniperShot shot = SniperShot.of(adjustment.applyTo(stats.attack())).scaledBy(adjustment.damageFactor())
                .withAttackSpeedBonus(adjustment.attackSpeed());
        if (stats.has(TowerCapability.MAGIC_DAMAGE)) {
            shot = shot.withType(DamageType.MAGIC);
        }
        ShotContext context = new ShotContext(target);
        for (SniperMechanic mechanic : owned) {
            shot = mechanic.shape(shot, context);
        }
        this.bullets.add(new Bullet(shot, stats, target,
                this.flightFrom(this.centerX, this.centerY, target)));
        double rate = this.attackRate();
        this.clock.spend(rate / (rate + shot.attackSpeedBonus()));
    }

    /** A flight from {@code x, y} to {@code target}, starting now. */
    private Flight flightFrom(double x, double y, EnemyMob target) {
        double distance = Math.hypot(target.getX() - x, target.getY() - y);
        int ticks = Math.max(MIN_FLIGHT_TICKS,
                (int) Math.ceil(distance / (BULLET_CELLS_PER_TICK * this.context.getBoard().scale())));
        return new Flight(this.tickNow, this.tickNow + ticks);
    }

    /** Lands every bullet and every hop whose flight ends now. */
    private void landArrived() {
        while (!this.hops.isEmpty() && this.hops.getFirst().bullet().landsAt() <= this.tickNow) {
            this.landHop(this.hops.removeFirst());
        }
        while (!this.bullets.isEmpty() && this.bullets.getFirst().landsAt() <= this.tickNow) {
            this.land(this.bullets.removeFirst());
        }
    }

    /**
     * A bullet arrives with the stats it was fired with: it hits its target, or is lost when the target is gone; then
     * what landed is settled and reacted to.
     */
    private void land(Bullet bullet) {
        EnemyMob target = bullet.target();
        this.landing = bullet;
        if (target.isDead()) {
            this.lastShot = this.traceOf(bullet, false);
            return;
        }
        HitResult hit = this.land(bullet.shot().aimed(), target, bullet.stats());
        if (hit.dodged()) {
            this.reportDodge(target, HitKind.DIRECT);
            this.lastShot = this.traceOf(bullet, false);
        } else {
            this.settle(new ShotResult(target, hit.critical(), target.isDead()), bullet, this.mechanics.all());
        }
    }

    /** A hop lands as the shot it came from, unable to crit, and nothing triggers off it. */
    private void landHop(Hop hop) {
        Bullet bullet = hop.bullet();
        if (!bullet.target().isDead()) {
            this.land(bullet.shot().aimed(), bullet.target(), bullet.stats());
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

    private ShotTrace traceOf(Bullet bullet, boolean critical) {
        return new ShotTrace(new Vec2(bullet.target().getX(), bullet.target().getY()), critical, bullet.shot().type(),
                this.tickNow);
    }

    private HitResult land(SniperShot shot, EnemyMob target, TowerStats stats) {
        return this.landHit(target, Damage.of(shot.type(), shot.amountOn(stats.damage())), shot.attack());
    }

    /** Sends the landing shot on from {@code from} to the nearest other enemy within {@code reachCells}. */
    private void ricochet(EnemyMob from, float reachCells) {
        float reach = reachCells * this.context.getBoard().scale();
        List<EnemyMob> beside = new ArrayList<>(InRangeTargetQuery.visible((int) from.getX(), (int) from.getY(), reach)
                .matching(this.context.enemies()));
        beside.remove(from);
        Optional<EnemyMob> next = new NearestSelector(from.getX(), from.getY()).selectFrom(beside);
        if (next.isEmpty()) {
            return;
        }
        SniperShot hop = this.landing.shot().withAttack(AttackProfile::withoutCrits);
        this.hops.add(new Hop(new Vec2(from.getX(), from.getY()), new Bullet(hop, this.landing.stats(), next.get(),
                this.flightFrom(from.getX(), from.getY(), next.get()))));
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

    /** The ricochets on their way, oldest first. */
    public List<Hop> hopsInFlight() {
        return List.copyOf(this.hops);
    }

    /** How many Steady Aim stacks the next shot has, whichever enemy it hits. */
    public int steadyAimStacks() {
        return Math.min(this.aimStackCap(), this.focus.stacks());
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
            List<HitBonus> bonuses = new ArrayList<>(STEADY_AIM);
            bonuses.addAll(this.upgrades().hitBonuses());
            List<TermText> parts = new ArrayList<>(bonuses.stream()
                    .filter(bonus -> bonus.scaling().focusCounter().filter(TowerTerm.STEADY_AIM::equals).isPresent())
                    .map(bonus -> bonus.adjustment().describe()).toList());
            parts.add(TermText.of("up to " + this.aimStackCap() + " stacks"));
            states.add(TowerBuffState.of(TowerTerm.STEADY_AIM, TermText.join("; ", parts)).withStacks(stacks)
                    .withClock(this.focus.ticksUntilFade(), AimFocus.STACK_TICKS));
        }
        this.mechanics.all().forEach(mechanic -> mechanic.condition().ifPresent(states::add));
        return states;
    }

    @Override
    protected String targetingLabel() {
        return this.spec().chosenAim().label();
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
     * @param flight when it flies
     */
    public record Bullet(SniperShot shot, TowerStats stats, EnemyMob target, Flight flight) {

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
     * A shot that hopped on from one enemy to the next.
     *
     * @param from   where it hopped from, in pixels
     * @param bullet the hop itself, aimed at the next enemy
     */
    public record Hop(Vec2 from, Bullet bullet) {
    }

    /**
     * When a bullet flies.
     *
     * @param firedAt the tick it was fired
     * @param landsAt the tick it arrives
     */
    public record Flight(int firedAt, int landsAt) {
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
        public void ricochet(EnemyMob from, float reachCells) {
            SniperTower.this.ricochet(from, reachCells);
        }
    }
}
