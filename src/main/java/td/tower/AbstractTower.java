package td.tower;

import td.damage.AttackOrigin;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.damage.Delivery;
import td.economy.EconomyDelta;
import td.economy.SellRefund;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.StackingDebuff;
import td.enemy.EnemyMob;
import td.enemy.EnemyWalk;
import td.enemy.HitResult;
import td.stat.DisruptionPenalty;
import td.stat.StatBreakdown;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.stat.StatPart;
import td.stat.StatPreview;
import td.stat.StatTerm;
import td.term.Side;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.proc.AttackOutcome;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitBonus;
import td.tower.proc.HitBonuses;
import td.tower.proc.HitContext;
import td.tower.proc.HitKind;
import td.tower.proc.Proc;
import td.tower.proc.ProcActions;
import td.tower.proc.ProcPayload;
import td.tower.proc.ProcRunner;
import td.tower.upgrade.Mechanic;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeBonus;
import td.tower.upgrade.UpgradeDecision;
import td.tower.upgrade.UpgradeLine;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradePath;
import td.tower.upgrade.UpgradeRemoval;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.PathRuntime;
import td.util.ThreadConfined;
import td.util.TickRate;
import td.wave.Vec2;
import td.wave.WaveAnnouncer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * What every tower shares: position, price, current stats, upgrades, and damage and kill
 * accounting. Subclasses supply targeting and {@code doTick}.
 * <p>
 * Everything derived from the board is computed in the constructor and {@code final}. Range checks
 * compare squared distances ({@link #rangeReal2()}) to avoid a square root per scan.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public abstract class AbstractTower implements Tower {

    /** Ticks per second at normal speed, from the one place the tick rate is defined. */
    protected static final float TICKS_PER_SECOND = TickRate.TICKS_PER_SECOND;
    // Shared, since an empty catalogue never brings a mechanic to hold.
    private static final OwnedMechanics<Mechanic> NO_MECHANICS = new OwnedMechanics<>(MechanicCatalogue.empty());

    protected final GameWorld context;
    protected final int boardX;
    protected final int boardY;
    protected final int centerX;
    protected final int centerY;
    protected final float rangeBase;
    protected final int damageBase;
    protected final int coolDownMax;
    protected final float critChanceBase;
    private final TowerBaseStats baseStats;
    /** The authored stats resolved with nothing on them: what every row's base reads from. */
    private final TowerStats authored;
    private final TowerFactory.Type type;
    /** Where a turret faces while nothing is in reach, fixed by where the tower stands. */
    private final double restHeading;
    private final TowerExperience experience = new TowerExperience();
    /** Who this tower's hits come from, so an effect can tell them from another tower's. */
    private final AttackOrigin origin = AttackOrigin.fresh();
    /** How many mobs had gone live when this tower was built; only later ones count for its XP. */
    private final long entriesBeforeBuilt;
    // damageDealt is a long read on the EDT; a non-volatile long read may tear.
    protected volatile boolean selected = false;
    protected volatile long damageDealt = 0;
    private volatile WaveDamageTally waveDamage = WaveDamageTally.none();
    protected volatile int killCount = 0;
    private volatile UpgradeState upgrades = UpgradeState.none();
    private volatile TowerStats stats;
    private volatile boolean removed = false;
    // Read on the EDT for the sell price; a load sets it to what the saved game paid.
    private volatile int price;
    // Sampled by the tick thread; also read when the EDT republishes stats after a purchase.
    private volatile DisruptionPenalty disruption = DisruptionPenalty.none();
    private final ActiveTowerStatuses statuses = new ActiveTowerStatuses();
    // Bought and downgraded on the EDT, read wherever a refund is worked out.
    private final AtomicReference<PurchaseStamps> purchases;
    // Downgraded on the EDT, forgotten by the tick thread at the start of the tower's next turn.
    private final AtomicReference<List<UpgradeNode>> removedSinceTick = new AtomicReference<>(List.of());
    private volatile InspectionCache inspectionCache;
    // Asked for every tower on every frame build, so rebuilt only when the owned nodes change.
    private volatile Optional<OfferedUpgrades> lastOffered = Optional.empty();
    private final ProcActions procActions = new TowerProcActions();
    // What the statuses add, published for the EDT's republish; the live list stays on the tick thread.
    private volatile StatusLayer statusLayer = StatusLayer.NONE;
    private int currentTick;
    private int shotsBegun;
    // The bonuses and procs of the upgrades last seen, so a hit with none allocates nothing.
    private UpgradeState armedFrom;
    private List<HitBonus> armedBonuses = List.of();
    private List<Proc> armedProcs = List.of();
    private float xpRemainder;
    private int lastWitherTick = Integer.MIN_VALUE;
    private boolean inKillHook;

    /**
     * Converts the cell coordinates to the pixel centre and range. A tower that hits every tick
     * passes {@code 0}; one whose cadence is not a cooldown also overrides {@link #cadence()}.
     */
    protected AbstractTower(TowerFactory.Type t, TowerBaseStats base, GameWorld context, int cellX, int cellY) {
        // Built after its price is charged and before it joins the roster, so this is what was paid.
        this.price = context.towers().priceOf(t);
        this.entriesBeforeBuilt = context.enemies().entries();
        this.type = t;
        this.damageBase = base.damage();
        this.rangeBase = base.range();
        this.coolDownMax = base.coolDownMax();
        this.critChanceBase = base.critChanceBase();
        this.baseStats = base;
        this.context = context;
        int scale = context.getBoard().scale();
        this.boardX = cellX * scale;
        this.boardY = cellY * scale;
        this.centerX = this.boardX + scale / 2;
        this.centerY = this.boardY + scale / 2;
        this.stats = TowerStats.of(base, StatModifiers.none(), StatModifiers.none(), DisruptionPenalty.none(), scale,
                Set.of());
        this.authored = this.stats;
        this.restHeading = restHeadingOf(context, this.centerX, this.centerY);
        this.purchases = new AtomicReference<>(PurchaseStamps.built(this.currentBreak()));
    }

    /** Toward the nearest point of any path, so a turret at rest loses little time turning to its first target. */
    private static double restHeadingOf(GameWorld context, int x, int y) {
        Vec2 nearest = null;
        double best = Double.MAX_VALUE;
        for (PathRuntime path : context.level().paths()) {
            for (Vec2 point : path.path().points()) {
                double dx = point.x() - x;
                double dy = point.y() - y;
                if (dx * dx + dy * dy < best) {
                    best = dx * dx + dy * dy;
                    nearest = point;
                }
            }
        }
        return nearest == null ? 0d : TurretAim.angleTo(x, y, nearest.x(), nearest.y());
    }

    /** The heading a turret or beam rests at between waves and starts each wave at. */
    protected final double restHeading() {
        return this.restHeading;
    }

    /**
     * The wave is over: drops everything it built up (statuses, shot counts, mechanic state and the tower's own fight
     * state), leaving the tower as waiting with nothing in reach leaves it. A turret keeps its pose and settles.
     */
    @Override
    public final void endWave() {
        this.shotsBegun = 0;
        this.lastWitherTick = Integer.MIN_VALUE;
        this.statuses.clear();
        this.disruption = DisruptionPenalty.none();
        this.experience.endWave();
        this.ownedMechanics().renew();
        this.endFight();
        this.refreshStatusLayer();
    }

    /** {@link #endWave()}, with the turret or beam at its rest pose: how every tower starts a wave. */
    @Override
    public final void readyForWave() {
        this.endWave();
        this.takeRestPose();
    }

    /** Hook dropping the tower's own fight state: its clock, targets, counters and stores; a no-op by default. */
    protected void endFight() {
    }

    /** Hook putting a turret or beam at {@link #restHeading()} at once; a no-op by default. */
    protected void takeRestPose() {
    }

    /** Whether no wave is out, so a turret with nothing to aim at turns to its rest pose. */
    protected final boolean betweenWaves() {
        return this.context.waves().betweenWaves();
    }

    /** The stats as authored, before any upgrade or buff. */
    TowerBaseStats baseStats() {
        return this.baseStats;
    }

    /** Current stats as one snapshot. Never null. */
    protected TowerStats stats() {
        return this.stats;
    }

    /** Damage per hit, in hundredths. */
    protected int damageCurrent() {
        return this.stats.damage();
    }

    /** How many times its base rate the tower attacks now; {@code 1} is the base. */
    protected double attackRate() {
        return this.stats.attackRate();
    }

    /** Range in pixels. */
    protected float rangeReal() {
        return this.stats.rangeReal();
    }

    /** Range in pixels, squared. */
    protected float rangeReal2() {
        return this.stats.rangeReal2();
    }

    /** Chance in {@code [0, 1]} that the next hit is critical. */
    protected float critChance() {
        return this.stats.critChance();
    }

    /** Whether this tower never attacks and only buffs its neighbours. */
    protected boolean isPassive() {
        return false;
    }

    public float baseRange() {
        return this.rangeBase;
    }

    public float getRangeReal() {
        return this.stats.rangeReal();
    }

    /**
     * What selling it pays back of everything paid: the tower at the price it was bought for, and
     * every upgrade bought for it. Nothing during free build.
     */
    public int getSellPrice() {
        return this.context.economy().refundOf(this.shareOf(this.upgrades().owned()));
    }

    /**
     * What selling the tower with {@code owned} gives back of what was paid: the share of what has been used, and all
     * of what has not.
     */
    private int shareOf(List<UpgradeNode> owned) {
        TowerUse now = this.use();
        int usedPaid = now.towerUsed() ? this.price : 0;
        int unusedPaid = now.towerUsed() ? 0 : this.price;
        for (UpgradeNode node : owned) {
            if (now.isUnused(node)) {
                unusedPaid += node.price();
            } else {
                usedPaid += node.price();
            }
        }
        return SellRefund.of(usedPaid) + unusedPaid;
    }

    @Override
    public TowerUse use() {
        return this.purchases.get().useAfter(this.context.waves().started());
    }

    /** The break between waves now running, counted by the waves started; none while a wave is out. */
    private OptionalLong currentBreak() {
        WaveAnnouncer waves = this.context.waves();
        return waves.betweenWaves() ? OptionalLong.of(waves.started()) : OptionalLong.empty();
    }

    /**
     * Recomputes stats from the buffs other towers contribute and this tower's own upgrades, and
     * publishes them as one {@link TowerStats} snapshot.
     * <p>
     * Buffs are asked of every tower on each call instead of being tracked, so there is no index
     * that can drift out of sync. Called on every tower whenever the tower set or any tower's
     * upgrades change.
     */
    public void recalculateStats() {
        this.publishStats(this.upgrades);
    }

    /**
     * Publishes stats for {@code upgrades}. Separate so that buying publishes the new stats before
     * the new upgrade state: a tick between the two writes then never pays a node's bounty bonus on
     * a kill made at the old damage.
     */
    private void publishStats(UpgradeState upgrades) {
        StatModifiers<TowerStat> received = this.context.towers().all().stream()
                .map(t -> t.buffFor(this))
                .reduce(StatModifiers.none(), StatModifiers::plus);
        StatusLayer layer = this.statusLayer;
        StatModifiers<TowerStat> own = upgrades.totalModifiers().plus(layer.modifiers());
        Set<TowerCapability> capabilities = EnumSet.noneOf(TowerCapability.class);
        capabilities.addAll(upgrades.capabilities());
        capabilities.addAll(layer.capabilities());
        this.stats = TowerStats.of(this.baseStats, own, received, this.disruption, this.context.getBoard().scale(),
                capabilities);
    }

    /**
     * Every hit goes through here, so accounting is right whichever subclass fires. The hit carries
     * this tower's current {@link AttackProfile}; the target rolls the crit.
     * <p>
     * Counts the damage that actually landed, and what a shield absorbed of it, not the damage fired. A kill adds the upgrades'
     * bounty bonus in credits, not score. A hit on a mob already killed this tick is not a second
     * kill. A no-op once the tower is sold, so a lingering burn stops crediting it.
     *
     * @return whether the hit landed as a critical hit
     */
    protected boolean dealDamage(EnemyMob enemy, Damage damage) {
        return this.dealDamage(enemy, damage, this.stats.attack());
    }

    /** {@link #dealDamage(EnemyMob, Damage)} with a one-off attack profile, for a special shot. */
    protected boolean dealDamage(EnemyMob enemy, Damage damage, AttackProfile attacker) {
        return this.landHit(enemy, damage, attacker).critical();
    }

    /**
     * {@link #dealDamage(EnemyMob, Damage, AttackProfile)} for a tower that needs the whole result: an aimed
     * hit can be dodged, and a dodge is no hit at all, so it withers nothing and credits nothing.
     */
    protected HitResult landHit(EnemyMob enemy, Damage damage, AttackProfile attacker) {
        if (this.removed) {
            return HitResult.none();
        }
        if (attacker.delivery() == Delivery.HIT && this.isBlinded()) {
            return HitResult.dodge();
        }
        boolean wasAlive = !enemy.isDead();
        HitResult result = enemy.takeHit(damage, attacker.withOrigin(this.origin));
        if (result.dodged()) {
            return result;
        }
        this.wither(enemy, attacker);
        if (wasAlive) {
            this.damageDealt += result.credited();
            if (result.credited() > 0) {
                this.waveDamage = this.waveDamage.plus(result.credited(), this.context.waves().started(),
                        this.currentTick);
            }
            this.context.towers().reportHit(this, enemy, result);
            if (enemy.isDead()) {
                this.killCount++;
                float bountyBonus = this.stats.bountyBonus();
                if (bountyBonus != 0f) {
                    this.context.economy().apply(EconomyDelta.credits(
                            Math.round(enemy.getBounty() * bountyBonus)));
                }
                this.runKillHook(enemy);
            }
        }
        return result;
    }

    /** Whether blinding fields over the tower make this hit miss; the random source is only drawn from when they do. */
    private boolean isBlinded() {
        float chance = this.stats.missChance();
        return chance > 0f && this.context.random().nextDouble() < chance;
    }

    /** Withering Field: the first hit in every interval applies a stack of Vulnerable; periodic damage is no hit. */
    private void wither(EnemyMob enemy, AttackProfile attacker) {
        int every = this.stats.witherTicks();
        if (every > 0 && attacker.delivery() == Delivery.HIT && !enemy.isDead()
                && (long) this.currentTick - this.lastWitherTick >= every) {
            this.lastWitherTick = this.currentTick;
            this.applyStacks(enemy, StackingDebuff.VULNERABLE, 1);
        }
    }

    /**
     * Runs {@link #onKill} for a kill this tower made. A kill made from inside the hook (an
     * explosion's) triggers nothing, so hooks never chain.
     */
    private void runKillHook(EnemyMob killed) {
        if (this.inKillHook) {
            return;
        }
        this.inKillHook = true;
        try {
            this.onKill(killed);
        } finally {
            this.inKillHook = false;
        }
    }

    /**
     * Damage that ticks (a burn or poison pulse, a field's tick), credited like a hit but never a
     * crit: the hit that starts it is the one that can crit.
     *
     * @return {@code false} always; periodic damage is never critical
     */
    protected boolean dealPeriodicDamage(EnemyMob enemy, Damage damage) {
        return this.dealDamage(enemy, damage, this.stats.attack().asPeriodic());
    }

    /**
     * How much stronger the damage over time a hit starts should be: this tower's crit factor
     * against {@code target} if the hit was critical, otherwise {@code 1}.
     */
    protected float potencyOfHit(EnemyMob target, boolean critical) {
        return critical ? target.critFactorFor(this.stats.attack()) : 1f;
    }

    /**
     * Puts an effect on {@code target}. Whatever damage it deals is credited to this tower as
     * periodic damage, so it never crits and never spends a mark.
     */
    protected void applyEffect(EnemyMob target, Function<DamageSink, Effect> effect) {
        Effect applied = effect.apply(d -> this.dealPeriodicDamage(target, d));
        float length = this.stats.effectLength();
        target.applyEffect(length == 1f ? applied : applied.withDurationScaledBy(length), this.origin);
    }

    /** {@code ticks} as long as the effects this tower makes last, for what a tower leaves on the ground. */
    protected int lengthened(int ticks) {
        return Math.round(ticks * this.stats.effectLength());
    }

    /**
     * Adds {@code stacks} of a stacking debuff to {@code target}, on the debuff's own clock; the
     * stacks belong to the enemy, not this tower.
     */
    protected void applyStacks(EnemyMob target, StackingDebuff debuff, int stacks) {
        this.applyEffect(target, sink -> debuff.effect(stacks, sink));
    }

    /**
     * Charges {@code target} for {@code durationTicks}: the next hit from another tower discharges
     * it, credited to this one.
     */
    protected void charge(EnemyMob target, int durationTicks) {
        this.applyEffect(target, sink -> Effect.charged(durationTicks, this.origin, sink));
    }

    /** Makes {@code target} targetable by every tower for {@code durationTicks}, even if invisible. */
    protected void reveal(EnemyMob target, int durationTicks) {
        this.applyEffect(target, sink -> Effect.revealed(durationTicks, sink));
    }

    /**
     * Hook for a kill this tower just made, with the mob still carrying the effects it died under.
     * A no-op by default.
     */
    protected void onKill(EnemyMob killed) {
    }

    /** Grants {@code status} to this tower as it would grant it to itself. */
    @Override
    public void receiveStatus(TowerStatus status) {
        this.grantStatus(status);
    }

    /**
     * Grants {@code status} now: the same id restarts unless it keeps a running copy, and different ids
     * stack. A timed status, and a shot status that says so, lasts longer with the buff duration.
     */
    protected final void grantStatus(TowerStatus status) {
        this.statuses.grant(status, this.currentTick, this.stats.timedBuffLength());
        this.refreshStatusLayer();
    }

    /**
     * Starts one attack: spends a shot of every shot status and returns the stats the attack uses. Call it
     * once per attack, before the attack, and use what it returns for that attack's damage and attack
     * profile: afterwards {@code stats()} may already be the next shot's.
     */
    protected final TowerStats beginShot() {
        this.shotsBegun++;
        List<Proc> procs = this.procs();
        if (!procs.isEmpty()) {
            ProcRunner.onShot(procs, this.shotsBegun, this.procActions);
        }
        TowerStats shot = this.stats;
        if (this.statuses.spendShot()) {
            this.refreshStatusLayer();
        }
        return shot;
    }

    private void refreshStatusLayer() {
        this.statusLayer = new StatusLayer(this.statuses.modifiers(), this.statuses.capabilities());
        this.publishStats(this.upgrades);
    }

    /** How many attacks this tower has begun, the one beginning now included. */
    protected final int shotNumber() {
        return this.shotsBegun;
    }

    /** Hit bonuses the tower has without any node; none by default. */
    protected List<HitBonus> baseHitBonuses() {
        return List.of();
    }

    /** Procs the tower has without any node; none by default. */
    protected List<Proc> baseProcs() {
        return List.of();
    }

    /** What the hit bonuses add to a hit on {@code target} at {@code rangeShare} of range, with no focus. */
    protected final HitAdjustment hitAdjustment(EnemyMob target, HitKind kind, float rangeShare, TowerStats shot) {
        return this.hitAdjustment(target, kind, rangeShare, shot, 0, false);
    }

    /**
     * What the hit bonuses add to a hit on {@code target}: {@code focus} attacks in a row on it, and
     * whether this is the first at it. Nothing to fold returns {@link HitAdjustment#none()} at once.
     */
    protected final HitAdjustment hitAdjustment(EnemyMob target, HitKind kind, float rangeShare, TowerStats shot,
                                                int focus, boolean freshTarget) {
        List<HitBonus> bonuses = this.hitBonuses();
        if (bonuses.isEmpty()) {
            return HitAdjustment.none();
        }
        return HitBonuses.fold(bonuses, HitContext.of(target, kind, rangeShare, shot).withFocus(focus, freshTarget));
    }

    /** Tells the procs how a landed attack went, after the tower's own mechanics settled it. */
    protected final void report(EnemyMob target, HitKind kind, boolean critical, boolean killed) {
        List<Proc> procs = this.procs();
        if (procs.isEmpty()) {
            return;
        }
        ProcRunner.onOutcome(procs, new AttackOutcome(target, kind, critical, killed), this.procActions);
    }

    /** Tells the procs that {@code target} dodged an attack of {@code kind}: only an on-dodge proc fires. */
    protected final void reportDodge(EnemyMob target, HitKind kind) {
        List<Proc> procs = this.procs();
        if (procs.isEmpty()) {
            return;
        }
        ProcRunner.onDodge(procs, new AttackOutcome(target, kind, false, false), this.procActions);
    }

    private List<HitBonus> hitBonuses() {
        this.arm();
        return this.armedBonuses;
    }

    private List<Proc> procs() {
        this.arm();
        return this.armedProcs;
    }

    private void arm() {
        UpgradeState upgrades = this.upgrades;
        if (upgrades == this.armedFrom) {
            return;
        }
        this.armedBonuses = concatenated(this.baseHitBonuses(), upgrades.hitBonuses());
        this.armedProcs = concatenated(this.baseProcs(), upgrades.procs());
        this.armedFrom = upgrades;
    }

    private static <T> List<T> concatenated(List<T> first, List<T> second) {
        if (second.isEmpty()) {
            return List.copyOf(first);
        }
        if (first.isEmpty()) {
            return List.copyOf(second);
        }
        List<T> all = new ArrayList<>(first);
        all.addAll(second);
        return List.copyOf(all);
    }

    /**
     * Start of this tower's turn: ends the timed statuses that have run out, and re-reads the disruption at
     * its centre. Stats are republished only when one of them changed; compared against the
     * published stats rather than the last sample, so a republish from the EDT racing this one is
     * corrected on the next tick.
     */
    @Override
    public boolean beginTick(int gameTime) {
        this.currentTick = gameTime;
        boolean republished = false;
        if (this.statuses.expire(gameTime)) {
            this.refreshStatusLayer();
            republished = true;
        }
        List<UpgradeNode> removed = this.removedSinceTick.getAndSet(List.of());
        if (!removed.isEmpty()) {
            if (this.statuses.revoke(grantedBy(removed))) {
                this.refreshStatusLayer();
                republished = true;
            }
            this.forgetRemovedUpgrades(removed);
        }
        DisruptionPenalty sampled = this.context.disruptions().penaltyAt(this.centerX, this.centerY)
                .plus(this.context.zones().disruptionAt(this.centerX, this.centerY));
        this.disruption = sampled;
        if (!sampled.equals(this.stats.disruption())) {
            this.publishStats(this.upgrades);
            republished = true;
        }
        return republished;
    }

    public boolean isDisrupted() {
        return !this.stats.disruption().isNone();
    }

    public long getDamageDealt() {
        return this.damageDealt;
    }

    public TowerExperience experience() {
        return this.experience;
    }

    /** Runs on the game-loop thread, so the rank-up is stamped with this tower's own tick. */
    public void earnXp(int bounty) {
        this.earnXp(bounty, 0f);
    }

    @Override
    public void earnXp(int bounty, float bonus) {
        float exact = bounty * (1f + bonus) + this.xpRemainder;
        int whole = (int) Math.floor(exact + 1e-4f);
        this.xpRemainder = Math.max(0f, exact - whole);
        this.experience.earn(whole, this.currentTick);
    }

    /**
     * Counts one deed of this tower's purpose, done by an attack: further calls in the same tick
     * (one per enemy the attack hit) don't add. Counting starts once Attune is owned.
     */
    protected void countDeedOfAttack() {
        if (this.upgrades.owns(StandardBaseSlot.ATTUNE_ID)) {
            this.experience.countDeedOfAttack(this.currentTick);
        }
    }

    /** Counts one deed done over time, at most once a second. Counting starts once Attune is owned. */
    protected void countDeedOfSecond() {
        if (this.upgrades.owns(StandardBaseSlot.ATTUNE_ID)) {
            this.experience.countDeedOfSecond(this.currentTick);
        }
    }

    public boolean reached(EnemyWalk walk) {
        return walk.entryOrdinal() > this.entriesBeforeBuilt
                && walk.walkedWithin(this.centerX, this.centerY, this.stats.reachReal());
    }

    public int getKillCount() {
        return this.killCount;
    }

    @Override
    public TowerTally tally() {
        return TowerTally.of(this.experience.xp(), this.experience.deeds(), this.damageDealt, this.killCount)
                .withXpCarried(this.xpRemainder);
    }

    @Override
    public void restore(TowerTally tally, int paid, TowerUse use) {
        this.purchases.set(PurchaseStamps.of(use, this.upgrades().owned(), this.context.waves().started()));
        this.experience.restore(tally.xp(), tally.deeds());
        this.damageDealt = tally.damageDealt();
        this.killCount = tally.kills();
        this.xpRemainder = tally.xpCarried();
        this.price = paid;
    }

    @Override
    public int paid() {
        return this.price;
    }

    /** No upgrades by default. */
    public UpgradeTree upgradeTree() {
        return UpgradeTree.none();
    }

    public UpgradeState upgrades() {
        return this.upgrades;
    }

    /** What is offered reads only the owned nodes (a gate is what reads the board), so it is kept until they change. */
    @Override
    public List<UpgradeNode> offeredUpgrades(GameWorld context) {
        UpgradeState owned = this.upgrades();
        Optional<OfferedUpgrades> last = this.lastOffered;
        if (last.isPresent() && last.get().state() == owned) {
            return last.get().nodes();
        }
        List<UpgradeNode> fresh = List.copyOf(this.upgradeTree().offered(this, context));
        this.lastOffered = Optional.of(new OfferedUpgrades(owned, fresh));
        return fresh;
    }

    private record OfferedUpgrades(UpgradeState state, List<UpgradeNode> nodes) {
    }

    /**
     * The mechanics this tower's owned nodes brought, which buying and downgrading keep up to date; none by
     * default. Return the same instance every call.
     */
    protected OwnedMechanics<? extends Mechanic> ownedMechanics() {
        return NO_MECHANICS;
    }

    /** Which mechanics each node brings. */
    protected final MechanicCatalogue<? extends Mechanic> mechanicCatalogue() {
        return this.ownedMechanics().catalogue();
    }

    @Override
    public NodeWords wordsOf(UpgradeNode node) {
        List<TermText> sentences = new ArrayList<>(node.effects().sentences());
        sentences.addAll(this.mechanicCatalogue().describe(node));
        return NodeWords.of(node.displayName(), this.previewed(node.effects().statRows(), node), sentences);
    }

    /**
     * Each of the tower's own bonuses with the row it moves, as its lasting stats have it now and would with
     * {@code node}: the first row whose source is the bonus's stat. A node already owned moves nothing, so its
     * bonuses carry no preview.
     */
    private List<UpgradeBonus> previewed(List<UpgradeBonus> bonuses, UpgradeNode node) {
        Function<TowerStat, List<StatPart>> noParts = stat -> List.of();
        boolean applies = !this.applies().isEmpty();
        List<TowerStatLine> before = this.statRows(this.lasting(List.of()), noParts, applies);
        List<TowerStatLine> after = this.statRows(this.lasting(List.of(node)), noParts, applies);
        return bonuses.stream().map(bonus -> {
            if (bonus.given()) {
                return bonus;
            }
            Optional<TowerStatLine> was = before.stream().filter(row -> row.source() == bonus.stat()).findFirst();
            Optional<TowerStatLine> will = after.stream().filter(row -> row.source() == bonus.stat()).findFirst();
            return was.isPresent() && will.isPresent() && was.get().stat() == will.get().stat()
                    && was.get().current() != will.get().current()
                    ? bonus.withPreview(new StatPreview(was.get().stat(), was.get().current(), will.get().current()))
                    : bonus;
        }).toList();
    }

    public boolean buyUpgrade(UpgradeNode node) {
        if (!this.upgradeTree().offers(node, this, this.context) || !node.gateMet(this, this.context)) {
            return false;
        }
        if (!this.context.economy().doPay(node.price())) {
            return false;
        }
        this.own(List.of(node));
        return true;
    }

    @Override
    public boolean buyPath(UpgradeNode target) {
        UpgradePath path = this.upgradeTree().planPath(target, this, this.context);
        if (!path.isOpen() || !this.context.economy().doPay(path.total())) {
            return false;
        }
        this.own(path.steps());
        return true;
    }

    /** Adds {@code bought}, paid for, in order. */
    private void own(List<UpgradeNode> bought) {
        UpgradeState nextState = this.upgrades;
        for (UpgradeNode node : bought) {
            this.ownedMechanics().add(node);
            nextState = nextState.with(node);
            OptionalLong inBreak = this.currentBreak();
            this.purchases.updateAndGet(now -> now.withBought(node, inBreak));
        }
        this.publishStats(nextState);
        this.upgrades = nextState;
        // This tower's contribution to its neighbours may have changed, so every tower recomputes.
        for (Tower t : this.context.towers().all()) {
            t.recalculateStats();
        }
    }


    @Override
    public int refundFor(UpgradeRemoval removal) {
        return this.context.economy().refundOf(this.shareOf(this.upgrades().owned())
                - this.shareOf(removal.kept().owned()));
    }

    @Override
    public boolean downgrade(UpgradeNode node) {
        UpgradeRemoval removal = this.upgradeTree().removalFor(node, this, this.context);
        if (removal.isEmpty()) {
            return false;
        }
        int refund = this.refundFor(removal);
        this.ownedMechanics().remove(removal.removed());
        this.publishStats(removal.kept());
        this.upgrades = removal.kept();
        this.removedSinceTick.accumulateAndGet(removal.removed(), AbstractTower::concatenated);
        removal.removed().forEach(gone -> this.purchases.updateAndGet(now -> now.withRemoved(gone)));
        for (Tower t : this.context.towers().all()) {
            t.recalculateStats();
        }
        this.context.economy().apply(EconomyDelta.credits(refund));
        return true;
    }


    /**
     * Hook clearing what the tower holds for {@code removed} outside its mechanics (a store, a counter),
     * on the game loop at the start of its first turn after the downgrade; a no-op by default.
     */
    protected void forgetRemovedUpgrades(List<UpgradeNode> removed) {
    }

    /** The statuses {@code nodes}' procs grant, which go with them. */
    private static Set<TowerTerm> grantedBy(List<UpgradeNode> nodes) {
        Set<TowerTerm> terms = new HashSet<>();
        for (UpgradeNode node : nodes) {
            for (Proc proc : node.effects().procs()) {
                proc.payload().grantedStatus().ifPresent(status -> terms.add(status.term()));
            }
        }
        return terms;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getX() {
        return centerX;
    }

    public int getY() {
        return centerY;
    }

    public int getBoardX() {
        return this.boardX;
    }

    @Override
    public AttackOrigin origin() {
        return this.origin;
    }

    public int getBoardY() {
        return this.boardY;
    }

    public TowerFactory.Type getType() {
        return this.type;
    }

    /**
     * How often the tower attacks at {@code stats}, its parts from {@code parts}. A tower whose cadence is not a
     * cooldown overrides this.
     */
    protected TowerStatLine cadence(TowerStats stats, Function<TowerStat, List<StatPart>> parts) {
        float perSecond = TICKS_PER_SECOND / (this.coolDownMax + 1);
        return this.derivedRow(TowerStatRow.FIRE_RATE, TowerStat.ATTACK_SPEED, stats, parts,
                at -> (float) (perSecond * at.attackRate()));
    }

    /** Whether it deals its damage every tick to whoever stands in its field, rather than hit by hit. */
    protected boolean dealsByField() {
        return false;
    }

    /** Physical by default. */
    protected DamageType damageType() {
        return DamageType.PHYSICAL;
    }

    /** Rows of this tower's own at {@code stats}, such as how its shots fly. */
    protected List<TowerStatLine> ownStats(TowerStats stats) {
        return List.of();
    }

    /** What it hands the towers around it; nothing by default. */
    protected StatModifiers<TowerStat> gives() {
        return StatModifiers.none();
    }

    /**
     * A row worked out from {@code source} by {@code read}: its base read from the authored stats, its sum from the
     * source's adding parts, its parts the source's.
     */
    protected final TowerStatLine derivedRow(StatTerm row, TowerStat source, TowerStats stats,
                                             Function<TowerStat, List<StatPart>> parts,
                                             Function<TowerStats, Float> read) {
        List<StatPart> sourceParts = parts.apply(source);
        float base = read.apply(this.authored);
        if (sourceParts.isEmpty()) {
            return new TowerStatLine(new StatBreakdown(row, base, List.of(), base, List.of(), read.apply(stats)),
                    source);
        }
        StatModifier adding = sourceParts.stream().filter(StatPart::adds).map(part -> part.added().modifier())
                .reduce(StatModifier.none(), StatModifier::plus);
        TowerStats summed = TowerStats.of(this.baseStats, StatModifiers.of(source, adding), StatModifiers.none(),
                DisruptionPenalty.none(), this.context.getBoard().scale(), Set.of());
        return new TowerStatLine(StatBreakdown.of(row, base, sourceParts, read.apply(summed), read.apply(stats)),
                source);
    }
    /** What no row says, for the shop; empty by default. */
    protected String description() {
        return "";
    }

    /**
     * Every buff and debuff this tower carries now, buffs first. Game loop only: reads statuses and mechanic state.
     */
    @Override
    public final List<TowerBuffState> conditions() {
        List<TowerBuffState> all = new ArrayList<>(this.statuses.states(this.currentTick));
        for (Proc proc : this.procs()) {
            int every = proc.trigger().shotInterval();
            Optional<TowerStatus> granted = proc.payload().grantedStatus();
            if (every > 0 && granted.isPresent() && (this.shotsBegun + 1) % every == 0) {
                all.add(TowerBuffState.of(granted.get().term(), granted.get().describe()));
            }
        }
        all.addAll(this.ownConditions());
        this.receivedCondition().ifPresent(all::add);
        this.jammedCondition().ifPresent(all::add);
        List<TowerBuffState> sorted = new ArrayList<>(all.stream().filter(s -> s.side() == Side.BUFF).toList());
        sorted.addAll(all.stream().filter(s -> s.side() == Side.DEBUFF).toList());
        return sorted;
    }

    /** How far each counting node it owns is toward its next go. */
    @Override
    public final List<NodeProgress> progress() {
        List<NodeProgress> all = new ArrayList<>();
        for (UpgradeNode node : this.upgrades().owned()) {
            for (Proc proc : node.effects().procs()) {
                int every = proc.trigger().shotInterval();
                if (every > 0) {
                    all.add(new NodeProgress(node.id(), this.shotsBegun % every, every));
                }
            }
        }
        all.addAll(this.ownProgress());
        return all;
    }

    /**
     * Every effect this tower puts on enemies or gives towers, first its own, then each owned node's procs and
     * mechanics in purchase order. Game loop only.
     */
    @Override
    public final List<AppliedEffect> applies() {
        Map<Term, List<AppliedEffect.Source>> byTerm = new LinkedHashMap<>();
        for (AppliedEffect own : this.baseApplies()) {
            byTerm.computeIfAbsent(own.term(), term -> new ArrayList<>()).addAll(own.sources());
        }
        for (UpgradeNode node : this.upgrades().owned()) {
            for (Proc proc : node.effects().procs()) {
                this.appliedBy(proc.payload()).ifPresent(term -> byTerm.computeIfAbsent(term, t -> new ArrayList<>())
                        .add(new AppliedEffect.Source(node.displayName(),
                                proc.describe())));
            }
            for (Mechanic mechanic : this.mechanicCatalogue().mechanicsOf(node)) {
                for (Term term : mechanic.applies()) {
                    byTerm.computeIfAbsent(term, t -> new ArrayList<>())
                            .add(new AppliedEffect.Source(node.displayName(), mechanic.describe()));
                }
            }
        }
        return byTerm.entrySet().stream().map(e -> new AppliedEffect(e.getKey(), e.getValue())).toList();
    }

    private Optional<Term> appliedBy(ProcPayload payload) {
        return switch (payload) {
            case ProcPayload.ApplyStacks stacks -> Optional.of(stacks.debuff().kind());
            case ProcPayload.Charge charge -> Optional.of(EffectKind.CHARGED);
            default -> Optional.empty();
        };
    }

    /** What the tower applies on its own, before any upgrade. None by default. */
    protected List<AppliedEffect> baseApplies() {
        return List.of();
    }

    /** Whom it aims at, as its row words it; empty for a tower that aims at no one. */
    protected String targetingLabel() {
        return "";
    }

    /** Buffs and debuffs of the tower's own: its rhythms and its mechanics' states. None by default. */
    protected List<TowerBuffState> ownConditions() {
        return List.of();
    }

    /** Counting nodes of the tower's own, beyond every-Nth-shot procs. None by default. */
    protected List<NodeProgress> ownProgress() {
        return List.of();
    }

    /** The counting node {@code nodeId} toward its next go, when this tower owns it. */
    protected final Optional<NodeProgress> countingNode(String nodeId, int count, int of) {
        return this.upgrades().owned().stream().anyMatch(node -> node.id().equals(nodeId))
                ? Optional.of(new NodeProgress(nodeId, count, of))
                : Optional.empty();
    }

    private Optional<TowerBuffState> receivedCondition() {
        long givers = this.context.towers().all().stream().filter(other -> !other.buffFor(this).isNone()).count();
        if (givers == 0) {
            return Optional.empty();
        }
        TowerBuffState state = TowerBuffState.of(TowerTerm.AURA, TowerTerm.AURA.describe())
                .withChanges(this.stats.received());
        return Optional.of(givers > 1 ? state.withStacks((int) givers) : state);
    }

    /** The jam as it reaches this tower: the enemy's penalty times what its disruption taken lets through. */
    private Optional<TowerBuffState> jammedCondition() {
        TowerStats now = this.stats;
        if (now.disruption().isNone()) {
            return Optional.empty();
        }
        StatModifiers<TowerStat> changes = jamChanges(now);
        TermText blinded = now.missChance() > 0f
                ? TermText.of(TermNumbers.percent(now.missChance()) + " of its hits miss")
                : TowerTerm.JAMMED.describe();
        return Optional.of(TowerBuffState.of(TowerTerm.JAMMED, blinded).withChanges(changes));
    }

    /** What each owned node says, with the nodes it was chosen over. */
    private Map<String, NodeWords> wordsOfOwned() {
        UpgradeState owned = this.upgrades();
        Map<String, List<String>> passedOver = new LinkedHashMap<>();
        for (UpgradeDecision decision : this.upgradeTree().decisions(owned)) {
            passedOver.put(decision.chosen().id(), decision.passedOver().stream().map(UpgradeNode::displayName).toList());
        }
        Map<String, NodeWords> words = new LinkedHashMap<>();
        for (UpgradeNode node : owned.owned()) {
            words.put(node.id(), this.wordsOf(node).withChosenOver(passedOver.getOrDefault(node.id(), List.of())));
        }
        return words;
    }

    /** What changes only when an upgrade is bought, rebuilt only then. */
    private InspectionCache cachedForUpgrades() {
        UpgradeState owned = this.upgrades();
        InspectionCache last = this.inspectionCache;
        if (last != null && last.state() == owned) {
            return last;
        }
        InspectionCache fresh = new InspectionCache(owned, this.upgradeTree().lines(owned), this.applies(),
                this.wordsOfOwned());
        this.inspectionCache = fresh;
        return fresh;
    }

    private record InspectionCache(UpgradeState state, List<UpgradeLine> lines, List<AppliedEffect> applies,
                                   Map<String, NodeWords> words) {
    }

    /**
     * The sheet's rows at {@code stats}: range, how it deals damage, its own rows, then every stat whose row shows.
     *
     * @param applies whether it puts any effect on enemies or towers
     */
    private List<TowerStatLine> statRows(TowerStats stats, Function<TowerStat, List<StatPart>> parts,
                                         boolean applies) {
        List<TowerStatLine> rows = new ArrayList<>();
        rows.add(this.statRow(TowerStat.RANGE, stats, parts));
        if (!this.isPassive()) {
            rows.addAll(this.attackRows(stats, parts));
        }
        rows.addAll(this.ownStats(stats));
        for (TowerStat stat : TowerStat.values()) {
            if (this.showsRow(stat, stats, applies)) {
                rows.add(this.statRow(stat, stats, parts));
            }
        }
        return rows;
    }

    /** Damage, then how often: a fire rate with the damage a second it comes to, or a field's damage a second. */
    private List<TowerStatLine> attackRows(TowerStats stats, Function<TowerStat, List<StatPart>> parts) {
        boolean magic = this.dealsMagic(stats);
        if (this.dealsByField()) {
            return List.of(this.derivedRow(TowerStatRow.damage(magic, true), TowerStat.DAMAGE, stats, parts,
                    at -> DamageUnits.inPoints(at.damage()) * TICKS_PER_SECOND * (float) at.attackRate()));
        }
        TowerStatLine damage = this.derivedRow(TowerStatRow.damage(magic, false), TowerStat.DAMAGE, stats, parts,
                at -> (float) at.damage());
        TowerStatLine cadence = this.cadence(stats, parts);
        if (cadence.stat() != TowerStatRow.FIRE_RATE) {
            return List.of(damage, cadence);
        }
        return List.of(damage, cadence, this.dpsRow(stats, cadence));
    }

    private boolean dealsMagic(TowerStats stats) {
        return stats.has(TowerCapability.MAGIC_DAMAGE) || this.damageType() == DamageType.MAGIC;
    }

    /** What one target takes a second at {@code stats}: what a hit deals, as often as it fires. */
    protected TowerStatLine dpsRow(TowerStats stats, TowerStatLine cadence) {
        return dpsLine(TowerStatRow.dps(this.dealsMagic(stats)), DamageUnits.inPoints(this.authored.damage()) * cadence.base() * averageCrit(this.authored),
                DamageUnits.inPoints(stats.damage()) * cadence.current() * averageCrit(stats));
    }

    /** A damage-a-second row, which no node changes by itself: it follows the stats it comes from. */
    protected static TowerStatLine dpsLine(TowerStatRow row, float base, float now) {
        return new TowerStatLine(new StatBreakdown(row, base, List.of(), base, List.of(), now), TowerStat.DAMAGE);
    }

    /** The stats as authored, resolved with nothing on them. */
    protected final TowerStats authoredStats() {
        return this.authored;
    }

    /** What a hit deals on average for its crits, as a share of a plain one. */
    protected static float averageCrit(TowerStats stats) {
        return 1f + stats.critChance() * (stats.value(TowerStat.CRIT_MULTIPLIER) - 1f);
    }

    private TowerStatLine statRow(TowerStat stat, TowerStats stats, Function<TowerStat, List<StatPart>> parts) {
        return new TowerStatLine(StatBreakdown.resolved(stat, this.baseStats.stats().value(stat), parts.apply(stat),
                stats.value(stat)), stat);
    }

    private boolean showsRow(TowerStat stat, TowerStats stats, boolean applies) {
        // A stat whose range starts above its default resolves to that floor, which is still its default.
        float unset = stat.clamp(stat.defaultBase());
        boolean offDefault = stat.clamp(this.baseStats.stats().value(stat)) != unset || stats.value(stat) != unset;
        return switch (stat.shows()) {
            case IN_CORE_ROW -> false;
            case OFF_DEFAULT -> offDefault;
            case WHILE_IT_CRITS -> offDefault || stats.critChance() > 0f;
            case WHILE_IT_APPLIES -> offDefault || applies;
        };
    }

    /**
     * Each kind of source that changes {@code stat} now, once with how many of it there are: every owned node,
     * every held status, what the towers around it give, and the jam. Game loop only.
     */
    private List<StatPart> partsOf(TowerStat stat, StatModifiers<TowerStat> jam) {
        List<StatPart> parts = new ArrayList<>();
        for (UpgradeNode node : this.upgrades().owned()) {
            StatModifier modifier = node.effects().modifiers().get(stat);
            if (!modifier.equals(StatModifier.none())) {
                parts.add(new StatPart(TermText.of(node.displayName()), 1, stat, modifier));
            }
        }
        Map<Term, List<TowerStatus>> byTerm = new LinkedHashMap<>();
        this.statuses.held().forEach(status -> byTerm.computeIfAbsent(status.term(), t -> new ArrayList<>())
                .add(status));
        byTerm.forEach((term, held) -> {
            StatModifier modifier = held.stream().map(status -> status.modifiers().get(stat))
                    .reduce(StatModifier.none(), StatModifier::plus);
            if (!modifier.equals(StatModifier.none())) {
                parts.add(new StatPart(TermText.of(term), held.size(), stat, modifier));
            }
        });
        StatModifier received = this.stats.received().get(stat);
        if (!received.equals(StatModifier.none())) {
            int givers = (int) this.context.towers().all().stream()
                    .filter(other -> !other.buffFor(this).get(stat).equals(StatModifier.none())).count();
            parts.add(new StatPart(TermText.of(TowerTerm.AURA), Math.max(1, givers), stat, received));
        }
        StatModifier jammed = jam.get(stat);
        if (!jammed.equals(StatModifier.none())) {
            parts.add(new StatPart(TermText.of(TowerTerm.JAMMED), 1, stat, jammed));
        }
        return parts;
    }

    /** The jam as it reaches this tower: the enemy's penalty times what its disruption taken lets through. */
    private static StatModifiers<TowerStat> jamChanges(TowerStats stats) {
        DisruptionPenalty jam = stats.disruption();
        float taken = stats.value(TowerStat.DISRUPTION_TAKEN);
        StatModifiers<TowerStat> changes = StatModifiers.none();
        if (jam.attackSpeed() != 0f) {
            changes = changes.plus(TowerModifiers.attackSpeed(-jam.attackSpeed() * taken));
        }
        if (jam.rangeCells() != 0f) {
            changes = changes.plus(TowerModifiers.range(-jam.rangeCells() * taken));
        }
        return changes;
    }

    /** The stats that last: its base, its upgrades with {@code extra} and what it receives; no jam, no status. */
    private TowerStats lasting(List<UpgradeNode> extra) {
        UpgradeState upgrades = this.upgrades();
        for (UpgradeNode node : extra) {
            upgrades = upgrades.owned().contains(node) ? upgrades : upgrades.with(node);
        }
        return TowerStats.of(this.baseStats, upgrades.totalModifiers(), this.stats.received(),
                DisruptionPenalty.none(), this.context.getBoard().scale(), upgrades.capabilities());
    }

    public TowerInspection inspect() {
        InspectionCache cached = this.cachedForUpgrades();
        TowerStats now = this.stats;
        StatModifiers<TowerStat> jam = jamChanges(now);
        List<TowerStatLine> lines = new ArrayList<>(
                this.statRows(now, stat -> this.partsOf(stat, jam), !cached.applies().isEmpty()));
        this.waveDamage.pointsPerSecondIn(this.context.waves().started()).ifPresent(rate -> lines.add(
                TowerStatLine.fixed(TowerStatRow.WAVE_DPS, TowerStat.DAMAGE, (float) rate)));
        int auras = (int) this.context.towers().all().stream()
                .filter(other -> !other.buffFor(this).isNone())
                .count();
        return TowerInspection.of(this.type, this.price, lines)
                .withGives(this.gives())
                .withName(this.displayName())
                .withDescription(this.description())
                .withRecord(this.killCount, this.damageDealt)
                .withXp(this.experience.xp())
                .withUpgrades(this.upgrades())
                .withDisruption(now.disruption())
                .withAuras(auras)
                .withLines(cached.lines())
                .withTargeting(this.targetingLabel())
                .withApplies(cached.applies())
                .withWords(cached.words());
    }

    /** Contributes nothing by default. */
    @Override
    public StatModifiers<TowerStat> buffFor(Tower other) {
        return StatModifiers.none();
    }

    /** What the statuses held now add to the tower's own layer, as one published value. */
    private record StatusLayer(StatModifiers<TowerStat> modifiers, Set<TowerCapability> capabilities) {

        private static final StatusLayer NONE = new StatusLayer(StatModifiers.none(), Set.of());
    }

    /** What a proc may make this tower do. */
    private final class TowerProcActions implements ProcActions {

        @Override
        public void applyStacks(EnemyMob target, StackingDebuff debuff, int stacks) {
            AbstractTower.this.applyStacks(target, debuff, stacks);
        }

        @Override
        public void charge(EnemyMob target, int ticks) {
            AbstractTower.this.charge(target, ticks);
        }

        @Override
        public void grantStatus(TowerStatus status) {
            AbstractTower.this.grantStatus(status);
        }
    }

    /** Marks this tower gone, so effects it applied stop crediting it after the refund. */
    public void doCleanup() {
        this.removed = true;
    }
}
