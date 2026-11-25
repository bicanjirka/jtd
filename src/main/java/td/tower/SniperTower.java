package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.StackingDebuff;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.stat.StatModifier;
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
import td.tower.sniper.ExposingMechanic;
import td.tower.sniper.HeadhunterMechanic;
import td.tower.sniper.HemorrhageMechanic;
import td.tower.sniper.Hop;
import td.tower.sniper.RicochetMechanic;
import td.tower.sniper.ShotActions;
import td.tower.sniper.ShotChain;
import td.tower.sniper.ShotResult;
import td.tower.sniper.ShotTrace;
import td.tower.sniper.SniperAim;
import td.tower.sniper.SniperMechanic;
import td.tower.sniper.SniperShot;
import td.tower.sniper.SniperSpec;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.PickContext;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.TargetPriority;
import td.tower.targeting.TargetSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTier;
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

    public static final int PRICE = 25;
    public static final float DAMAGE_POINTS = 20f;
    public static final float RANGE = 3.5f;
    public static final float CRIT_CHANCE = 0f;
    public static final float CRIT_MULTIPLIER = 2f;
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
            TowerModifiers.attackSpeed(0.30f)).lengthenedByBuffs());

    private static final HitBonus STEADY_AIM_BONUS = perSteadyAimStack(
            HitAdjustment.attackSpeed(0.05f).plus(HitAdjustment.critChance(0.02f)).plus(HitAdjustment.accuracy(2f))
                    .plus(HitAdjustment.range(0.2f)));

    private static final UpgradeNode STEADY_AIM = UpgradeNode.of("sniper.steady_aim", "Steady Aim", 5)
            .withModifiers(TowerModifiers.aimStacks(5).plus(TowerModifiers.aimStacksPerShot(1)))
            .withHitBonus(STEADY_AIM_BONUS);
    private static final UpgradeNode RIFLING = UpgradeNode.of("sniper.rifling", "Rifling", 5)
            .withModifiers(TowerModifiers.critChance(0.10f));

    private static final UpgradeNode FOURTH_SHOT = UpgradeNode.of("sniper.fourth_shot", "Fourth Shot", 10)
            .withProc(everyFourthShotIs(TowerStatus.nextShots(TowerTerm.FOURTH_SHOT, 1, StatModifiers.none())
                    .withCapability(TowerCapability.GUARANTEED_CRIT)))
            .inTier(1);
    private static final UpgradeNode DEEP_BREATH = UpgradeNode.of("sniper.deep_breath", "Deep Breath", 10)
            .withModifiers(TowerModifiers.aimStacks(5))
            .after(STEADY_AIM)
            .inTier(1);
    private static final UpgradeNode KEEN_EYE = UpgradeNode.of("sniper.keen_eye", "Keen Eye", 10)
            .withHitBonus(perSteadyAimStack(HitAdjustment.critChance(0.03f).plus(HitAdjustment.range(0.2f))))
            .after(STEADY_AIM)
            .inTier(1);
    private static final UpgradeNode DEADEYE = UpgradeNode.of("sniper.deadeye", "Deadeye", 10)
            .withModifiers(TowerModifiers.critMultiplier(1.0f))
            .inTier(1);

    private static final UpgradeNode HEMORRHAGE = UpgradeNode.of("sniper.hemorrhage", "Hemorrhage", 5);
    private static final UpgradeNode RICOCHET = UpgradeNode.of("sniper.ricochet", "Ricochet", 5);
    private static final UpgradeNode SUNDERING_ROUNDS = UpgradeNode.of("sniper.sundering_rounds", "Sundering Rounds", 5)
            .withProc(Proc.onHit(new ProcPayload.ApplyStacks(StackingDebuff.SUNDERED, 1)))
            .inTier(2);
    private static final UpgradeNode EXPOSING_CRITS = UpgradeNode.of("sniper.exposing_crits", "Exposing Crits", 5)
            .inTier(2);
    private static final UpgradeNode SILVER_ROUND = UpgradeNode.of("sniper.silver_round", "Silver Round", 15)
            .withProc(everyFourthShotIs(TowerStatus.nextShots(TowerTerm.SILVER_ROUND, 1,
                    TowerModifiers.damage(37f).plus(TowerModifiers.magicPenetration(1f))).withCapability(TowerCapability.MAGIC_DAMAGE)))
            .after(FOURTH_SHOT)
            .inTier(2);
    private static final UpgradeNode HOLLOW_POINT = UpgradeNode.of("sniper.hollow_point", "Hollow Point", 15)
            .withProc(everyFourthShotIs(TowerStatus.nextShots(TowerTerm.HOLLOW_POINT, 1,
                    TowerModifiers.damage(36f))))
            .after(FOURTH_SHOT)
            .inTier(2);

    private static final UpgradeNode EXECUTIONER = UpgradeNode.of("sniper.executioner", "Executioner", 10)
            .withTargeting(TargetPriority.EXECUTABLE_FIRST)
            .inTier(3);
    private static final UpgradeNode HEADHUNTER = UpgradeNode.of("sniper.headhunter", "Headhunter", 10)
            .withTargeting(TargetPriority.HUNTED_FIRST)
            .inTier(3);
    private static final UpgradeNode HEAVY_ROUNDS = UpgradeNode.of("sniper.heavy_rounds", "Heavy Rounds", 20)
            .withModifiers(TowerModifiers.damage(14f))
            .after(HEADHUNTER)
            .inTier(3);
    private static final UpgradeNode UNBROKEN_AIM = UpgradeNode.of("sniper.unbroken_aim", "Unbroken Aim", 10)
            .withModifiers(TowerModifiers.aimStacks(5))
            .after(DEEP_BREATH)
            .inTier(3);
    private static final UpgradeNode KILLING_SPREE = UpgradeNode.of("sniper.killing_spree", "Killing Spree", 10)
            .withProc(Proc.onKill(GRANTS_FRENZY))
            .after(EXECUTIONER)
            .withOwnArt()
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(STEADY_AIM, RIFLING, HEMORRHAGE, RICOCHET))
            .with(FOURTH_SHOT, DEEP_BREATH, KEEN_EYE, DEADEYE,
                    SUNDERING_ROUNDS, EXPOSING_CRITS, SILVER_ROUND, HOLLOW_POINT,
                    HEAVY_ROUNDS, EXECUTIONER, HEADHUNTER, UNBROKEN_AIM, KILLING_SPREE)
            .withCluster(UpgradeCluster.of("Aim", DEEP_BREATH, KEEN_EYE))
            .withCluster(UpgradeCluster.of("On crit", HEMORRHAGE, RICOCHET))
            .withCluster(UpgradeCluster.of("Debuff", SUNDERING_ROUNDS, EXPOSING_CRITS))
            .withCluster(UpgradeCluster.of("Ammo", SILVER_ROUND, HOLLOW_POINT))
            .withCluster(UpgradeCluster.of("Finisher", HEADHUNTER, EXECUTIONER))
            .withTiers(List.of(UpgradeTier.open(), UpgradeTier.gated(60, 0).withBonus(TowerModifiers.range(1f)),
                    UpgradeTier.gated(120, 0).withBonus(TowerModifiers.damage(12f)),
                    UpgradeTier.gated(200, 0).withBonus(TowerModifiers.attackSpeed(0.2f))));

    private static final MechanicCatalogue<SniperMechanic> MECHANICS = MechanicCatalogue.of(
            entry(HEMORRHAGE.id(), HemorrhageMechanic::new),
            entry(RICOCHET.id(), RicochetMechanic::new),
            entry(EXPOSING_CRITS.id(), ExposingMechanic::new),
            entry(EXECUTIONER.id(), ExecutionerMechanic::new),
            entry(HEADHUNTER.id(), HeadhunterMechanic::new));

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK, this.restHeading());
    private final OwnedMechanics<SniperMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private AimFocus focus = new AimFocus();
    private FireClock clock = new FireClock(COOLDOWN_MAX);
    private final List<Fired> bullets = new ArrayList<>();
    private final List<Hopping> hops = new ArrayList<>();
    private Optional<EnemyMob> target = Optional.empty();
    private Optional<ShotTrace> lastShot = Optional.empty();
    private Optional<Execution> lastExecution = Optional.empty();
    private OptionalInt firedAt = OptionalInt.empty();

    public SniperTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SNIPER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .withCritChance(CRIT_CHANCE).withCritMultiplier(CRIT_MULTIPLIER)
                .with(TowerStat.AIM_STACKS, 0f).with(TowerStat.AIM_STACKS_PER_SHOT, 0f), context, x, y);
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
        Viewpoint view = this.viewpoint(0f);
        SniperSpec spec = SniperSpec.from(view);
        return this.upgrades().targeting()
                .map(priority -> spec.aimingAt(new SniperAim(priority.selector(this.pickContext(view)), priority.label())))
                .orElse(spec);
    }

    /** The board's scale and what its next shot deals on a crit, before armor and hit bonuses. */
    private PickContext pickContext(Viewpoint view) {
        TowerStats now = this.stats();
        return new PickContext(view.cellSize(), Math.round(now.damage() * now.attack().critMultiplier()));
    }

    private Optional<EnemyMob> findEnemy(SniperSpec spec) {
        TargetSelector selector = PreferringSelector.priority(spec.aim().selector());
        return selector.selectFrom(spec.reach().matching(this.context.enemies()));
    }

    @Override
    protected void act(int gameTime) {
        this.focus.advance();
        this.holdFocus(this.steadyAimStacks());
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
        this.holdFocus(0);
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

    /**
     * One shot: shaped by the owned mechanics and sent on its way, and the wait that follows. Its stats hold the
     * Steady Aim stacks it is fired under; the stacks it builds are in the stats from then on.
     */
    private void fire(EnemyMob target, List<SniperMechanic> owned) {
        List<TowerTerm> rounds = ROUNDS.stream().filter(this::primes).toList();
        TowerStats stats = this.beginShot();
        // After the charge the shot spent, under the stacks it is fired with.
        double firedRate = this.attackRate();
        AimLock lock = this.focus.lock(target, this.aimStackCap(), this.aimStacksPerShot());
        this.holdFocus(this.steadyAimStacks());
        float rangeShare = (float) (Math.hypot(target.getX() - this.centerX, target.getY() - this.centerY)
                / stats.rangeReal());
        HitAdjustment adjustment = this.hitAdjustment(target, HitKind.DIRECT, rangeShare, stats, lock.stacks(),
                lock.fresh());
        SniperShot unshaped = SniperShot.of(adjustment.applyTo(stats.attack())).scaledBy(adjustment.damageFactor())
                .withAttackSpeedBonus(adjustment.attackSpeed()).asRounds(rounds);
        if (stats.has(TowerCapability.MAGIC_DAMAGE)) {
            unshaped = unshaped.withType(DamageType.MAGIC);
        }
        ShotChain chain = ShotChain.of(unshaped);
        SniperShot shot = chain.shapedFor(target, owned);
        this.bullets.add(new Fired(new Bullet(shot, stats, target, this.flightFrom(this.centerX, this.centerY, target)),
                chain));
        this.firedAt = OptionalInt.of(this.tickNow());
        // The wait after a shot is the period at the rate it was fired with, its own bonus added; the clock counts it
        // at the rate the stacks it built give, so it is paid as a share of the period at that rate.
        this.clock.spend(this.attackRate() / (firedRate + shot.attackSpeedBonus()));
    }

    /** A flight from {@code x, y} to {@code target}, starting now. */
    private ShotFlight flightFrom(double x, double y, EnemyMob target) {
        return ShotFlight.across(this.tickNow(), Math.hypot(target.getX() - x, target.getY() - y),
                BULLET_CELLS_PER_TICK * this.cellPixels(), MIN_FLIGHT_TICKS);
    }

    /** Lands every hop and every bullet whose flight ends now. */
    private void landArrived() {
        Arrivals.takeDue(this.hops, hopping -> hopping.hop().bullet().landsAt(), this.tickNow())
                .forEach(hopping -> this.land(hopping.hop().bullet(), hopping.chain()));
        Arrivals.takeDue(this.bullets, fired -> fired.bullet().landsAt(), this.tickNow())
                .forEach(fired -> this.lastShot = Optional.of(this.land(fired.bullet(), fired.chain())));
    }

    /**
     * A bullet or a hop arrives with the stats it was fired with: it hits its target, or is lost when the target is
     * gone; then what landed is settled and reacted to. Tells how it went, for drawing its trace.
     */
    private ShotTrace land(Bullet bullet, ShotChain chain) {
        EnemyMob target = bullet.target();
        if (target.isDead()) {
            return this.traceOf(bullet, false);
        }
        HitResult hit = this.strike(bullet);
        if (hit.dodged()) {
            return this.traceOf(bullet, false);
        }
        boolean critical = this.settle(new ShotResult(target, hit.critical(), target.isDead()),
                new Actions(bullet, chain.landedOn(target)), this.mechanics.all());
        return this.traceOf(bullet, critical);
    }

    /**
     * A shot that landed: the mechanics decide what it counts as, the procs hear of it, then the mechanics react.
     * Tells whether it counted as a crit.
     */
    private boolean settle(ShotResult landed, ShotActions actions, List<SniperMechanic> owned) {
        ShotResult result = landed;
        for (SniperMechanic mechanic : owned) {
            result = mechanic.settle(result, actions);
        }
        this.report(result.target(), HitKind.DIRECT, result.critical(), result.killed());
        for (SniperMechanic mechanic : owned) {
            mechanic.react(result, actions);
        }
        return result.critical();
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
        return this.bullets.stream().map(Fired::bullet).toList();
    }

    /** The ricochets on their way, oldest first. */
    public List<Hop> hopsInFlight() {
        return this.hops.stream().map(Hopping::hop).toList();
    }

    /** How many Steady Aim stacks the next shot has, whichever enemy it hits. */
    public int steadyAimStacks() {
        return Math.min(this.aimStackCap(), this.focus.stacks());
    }

    /**
     * A node that deepens Steady Aim also says what a stack of it then gives in all and how many it holds, the line's
     * whole rhythm rather than the node's own share of it.
     */
    @Override
    public NodeWords wordsOf(UpgradeNode node) {
        NodeWords words = super.wordsOf(node);
        boolean deepens = node.effects().hitBonuses().stream().anyMatch(SniperTower::growsWithSteadyAim)
                || !node.effects().modifiers().get(TowerStat.AIM_STACKS).equals(StatModifier.none());
        if (node.equals(STEADY_AIM) || !deepens) {
            return words;
        }
        List<UpgradeNode> with = TREE.pathTo(node);
        UpgradeState owned = this.upgrades();
        for (UpgradeNode added : with) {
            owned = owned.owned().contains(added) ? owned : owned.with(added);
        }
        HitAdjustment perStack = owned.hitBonuses().stream()
                .filter(SniperTower::growsWithSteadyAim).map(HitBonus::adjustment)
                .reduce(HitAdjustment.none(), HitAdjustment::plus);
        int cap = Math.round(this.lasting(with).value(TowerStat.AIM_STACKS));
        return words.withTotals(List.of(TermText.of(TowerTerm.STEADY_AIM).plus(" with it: ")
                .plus(perStack.describe()).plus(" per stack, up to " + cap + " stacks")));
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
    protected String description() {
        return "Fires one tracer at a time; it lands a moment after it leaves, and a shot at an enemy that dies in flight is lost.";
    }

    @Override
    protected String targetingLabel() {
        return this.spec().aim().label();
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSniperTower(this);
    }

    /** A bullet on its way, with the shot it starts. */
    private record Fired(Bullet bullet, ShotChain chain) {
    }

    /** A hop on its way, with the shot it belongs to. */
    private record Hopping(Hop hop, ShotChain chain) {
    }

    /** What the mechanics may make this tower do about the bullet or hop that just landed. */
    private final class Actions implements ShotActions {

        private final Bullet landed;
        private final ShotChain chain;

        private Actions(Bullet landed, ShotChain chain) {
            this.landed = landed;
            this.chain = chain;
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
        public void bleed(EnemyMob target, float sharePerCell, int ticks) {
            int perCell = Math.round(this.landed.stats().damage() * sharePerCell);
            AttackProfile bleed = this.landed.stats().attack().pastArmorAndPlating().asPeriodic();
            SniperTower.this.applyEffect(target, _ -> Effect.bleeding(perCell, ticks,
                    bled -> SniperTower.this.dealDamage(target, bled, bleed)));
        }

        @Override
        public void expose(EnemyMob target, int ticks) {
            SniperTower.this.applyEffect(target, sink -> Effect.exposed(ticks, sink));
        }

        @Override
        public void ricochet(EnemyMob from, float reachCells, int maxHops) {
            float reach = reachCells * SniperTower.this.cellPixels();
            List<EnemyMob> others = this.chain.hopTargets(InRangeTargetQuery.visible(from.position(), reach)
                    .matching(SniperTower.this.context.enemies()), maxHops);
            if (others.isEmpty()) {
                return;
            }
            EnemyMob next = others.get(SniperTower.this.context.random().nextIndex(others.size()));
            SniperShot hop = this.chain.shapedFor(next, SniperTower.this.mechanics.all()).asRounds(List.of());
            SniperTower.this.hops.add(new Hopping(new Hop(from.position(), new Bullet(hop, this.landed.stats(), next,
                    SniperTower.this.flightFrom(from.getX(), from.getY(), next)), this.chain.unshaped().rounds()),
                    this.chain));
        }
    }
}
