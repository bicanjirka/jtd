package td.tower;

import td.damage.AttackOrigin;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.damage.Delivery;
import td.economy.EconomyDelta;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyWalk;
import td.enemy.HitResult;
import td.stat.DisruptionPenalty;
import td.stat.StatModifiers;
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
import td.tower.upgrade.Perk;
import td.tower.upgrade.PerkCatalogue;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeBonus;
import td.tower.upgrade.UpgradeDecision;
import td.tower.upgrade.UpgradeLine;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    /** How long a vulnerability lasts from its latest application. */
    private static final float VULNERABLE_SECONDS = 4f;
    private static final float SUNDERED_SECONDS = 5f;
    private static final float RESONATING_SECONDS = 4f;
    private static final float UNRAVELED_SECONDS = 5f;

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
    private final TowerFactory.Type type;
    private final int price;
    private final TowerExperience experience = new TowerExperience();
    /** Who this tower's hits come from, so an effect can tell them from another tower's. */
    private final AttackOrigin origin = AttackOrigin.fresh();
    /** How many mobs had gone live when this tower was built; only later ones count for its XP. */
    private final long entriesBeforeBuilt;
    // damageDealt is a long read on the EDT; a non-volatile long read may tear.
    protected volatile boolean selected = false;
    protected volatile long damageDealt = 0;
    protected volatile int killCount = 0;
    private volatile UpgradeState upgrades = UpgradeState.none();
    private volatile TowerStats stats;
    private volatile boolean removed = false;
    // Sampled by the tick thread; also read when the EDT republishes stats after a purchase.
    private volatile DisruptionPenalty disruption = DisruptionPenalty.none();
    private final ActiveTowerStatuses statuses = new ActiveTowerStatuses();
    private volatile InspectionCache inspectionCache;
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

    public float getRange() {
        return this.rangeBase;
    }

    public float getRangeReal() {
        return this.stats.rangeReal();
    }

    /**
     * Three quarters of everything paid: the tower at the price it was bought for, and every
     * upgrade bought for it.
     */
    public int getSellPrice() {
        int upgradesPaid = this.upgrades().owned().stream().mapToInt(UpgradeNode::price).sum();
        return (int) Math.round(0.75 * (this.price + upgradesPaid));
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
        boolean wasAlive = !enemy.isDead();
        HitResult result = enemy.takeHit(damage, attacker.withOrigin(this.origin));
        if (result.dodged()) {
            return result;
        }
        this.wither(enemy, attacker);
        if (wasAlive) {
            this.damageDealt += result.credited();
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

    /** Withering Field: the first hit in every interval applies a stack of Vulnerable; periodic damage is no hit. */
    private void wither(EnemyMob enemy, AttackProfile attacker) {
        int every = this.stats.witherTicks();
        if (every > 0 && attacker.delivery() == Delivery.HIT && !enemy.isDead()
                && (long) this.currentTick - this.lastWitherTick >= every) {
            this.lastWitherTick = this.currentTick;
            this.applyStacks(enemy, EffectKind.VULNERABLE, 1);
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
        target.applyEffect(length == 1f ? applied : applied.withDurationScaledBy(length));
    }

    /** {@code ticks} as long as the effects this tower makes last, for what a tower leaves on the ground. */
    protected int lengthened(int ticks) {
        return Math.round(ticks * this.stats.effectLength());
    }

    /**
     * Adds {@code stacks} of a stacking debuff to {@code target}, on the debuff's own clock; the
     * stacks belong to the enemy, not this tower.
     */
    protected void applyStacks(EnemyMob target, EffectKind kind, int stacks) {
        this.applyEffect(target, sink -> switch (kind) {
            case VULNERABLE -> Effect.vulnerable(stacks, Math.round(VULNERABLE_SECONDS * TICKS_PER_SECOND), sink);
            case SUNDERED -> Effect.sundered(stacks, Math.round(SUNDERED_SECONDS * TICKS_PER_SECOND), sink);
            case RESONATING -> Effect.resonating(stacks, Math.round(RESONATING_SECONDS * TICKS_PER_SECOND), sink);
            case UNRAVELED -> Effect.unraveled(stacks, Math.round(UNRAVELED_SECONDS * TICKS_PER_SECOND), sink);
            default -> throw new IllegalArgumentException(kind + " is not a stacking debuff");
        });
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

    /** Tells the procs how a landed attack went, after the tower's own perks settled it. */
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
        DisruptionPenalty sampled = this.context.disruptions().penaltyAt(this.centerX, this.centerY);
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
    public void restore(TowerTally tally) {
        this.experience.restore(tally.xp(), tally.deeds());
        this.damageDealt = tally.damageDealt();
        this.killCount = tally.kills();
    }

    /** No upgrades by default. */
    public UpgradeTree upgradeTree() {
        return UpgradeTree.none();
    }

    public UpgradeState upgrades() {
        return this.upgrades;
    }

    /** Which perks each node brings; none by default. */
    protected PerkCatalogue<? extends Perk> perkCatalogue() {
        return PerkCatalogue.empty();
    }

    public List<UpgradeBonus> bonusesOf(UpgradeNode node) {
        List<UpgradeBonus> bonuses = new ArrayList<>(node.bonuses());
        for (TermText text : this.perkCatalogue().describe(node)) {
            bonuses.add(new UpgradeBonus(text.capitalized().plain(), ""));
        }
        return bonuses;
    }

    @Override
    public NodeWords wordsOf(UpgradeNode node) {
        List<TermText> sentences = new ArrayList<>(node.effects().sentences());
        sentences.addAll(this.perkCatalogue().describe(node));
        return NodeWords.of(node.displayName(), node.effects().statRows(), sentences);
    }

    public boolean buyUpgrade(UpgradeNode node) {
        if (!this.upgradeTree().offers(node, this, this.context) || !node.gateMet(this, this.context)) {
            return false;
        }
        if (!this.context.economy().doPay(node.price())) {
            return false;
        }
        this.onUpgradeBought(node);
        UpgradeState nextState = this.upgrades.with(node);
        this.publishStats(nextState);
        this.upgrades = nextState;
        // This tower's contribution to its neighbours may have changed, so every tower recomputes.
        for (Tower t : this.context.towers().all()) {
            t.recalculateStats();
        }
        return true;
    }

    /**
     * Hook for an upgrade effect that no stat expresses; a no-op by default.
     * Compare {@code node} by equality, not reference.
     */
    protected void onUpgradeBought(UpgradeNode node) {
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

    public int getBoardY() {
        return this.boardY;
    }

    public TowerFactory.Type getType() {
        return this.type;
    }

    /**
     * How often the tower attacks, as authored and now; empty for a tower that never attacks. A
     * tower whose cadence is not a cooldown overrides this.
     */
    protected Optional<TowerStatLine> cadence() {
        if (this.isPassive()) {
            return Optional.empty();
        }
        float base = TICKS_PER_SECOND / (this.coolDownMax + 1);
        return Optional.of(new TowerStatLine(TowerStatRow.FIRE_RATE, base, (float) (base * this.attackRate())));
    }

    /** Physical by default. */
    protected DamageType damageType() {
        return DamageType.PHYSICAL;
    }

    /** Stats of this tower's own, such as a splash radius, shown after the shared ones. */
    protected List<TowerStatLine> ownStats() {
        return List.of();
    }
    /** What no row says, for the shop; empty by default. */
    protected String description() {
        return "";
    }

    /** Whether this tower stands on the board, rather than being a shop preview. */
    protected boolean isPlaced() {
        return this.context.towers().all().contains(this);
    }

    /**
     * Every buff and debuff this tower carries now, buffs first. Game loop only: reads statuses and perk state.
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
     * perks in purchase order. Game loop only.
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
            for (Perk perk : this.perkCatalogue().perksOf(node)) {
                for (Term term : perk.applies()) {
                    byTerm.computeIfAbsent(term, t -> new ArrayList<>())
                            .add(new AppliedEffect.Source(node.displayName(), perk.describe()));
                }
            }
        }
        return byTerm.entrySet().stream().map(e -> new AppliedEffect(e.getKey(), e.getValue())).toList();
    }

    private Optional<Term> appliedBy(ProcPayload payload) {
        return switch (payload) {
            case ProcPayload.ApplyStacks stacks -> Optional.of(stacks.kind());
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

    /** Buffs and debuffs of the tower's own: its rhythms and its perks' states. None by default. */
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
        StatModifiers<TowerStat> received = this.stats.received();
        List<String> parts = new ArrayList<>();
        for (TowerStat stat : received.stats()) {
            parts.add(stat.describe(received.get(stat)) + " " + stat.label().toLowerCase(Locale.ROOT));
        }
        TowerBuffState state = TowerBuffState.of(TowerTerm.AURA, TermText.of(String.join(", ", parts)));
        return Optional.of(givers > 1 ? state.withStacks((int) givers) : state);
    }

    private Optional<TowerBuffState> jammedCondition() {
        DisruptionPenalty jam = this.stats.disruption();
        if (jam.isNone()) {
            return Optional.empty();
        }
        return Optional.of(TowerBuffState.of(TowerTerm.JAMMED, TermText.of(
                TermNumbers.signedPercent(-jam.attackSpeed()) + " attack speed, "
                        + TermNumbers.signed(-jam.rangeCells()) + " range")));
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

    public TowerInspection inspect() {
        InspectionCache cached = this.cachedForUpgrades();
        TowerStats now = this.stats;
        List<TowerStatLine> lines = new ArrayList<>();
        lines.add(new TowerStatLine(TowerStatRow.RANGE, this.rangeBase, now.range()));
        if (!this.isPassive()) {
            DamageType type = now.has(TowerCapability.MAGIC_DAMAGE) ? DamageType.MAGIC : this.damageType();
            TowerStatRow damage = type == DamageType.MAGIC ? TowerStatRow.MAGIC_DAMAGE : TowerStatRow.PHYSICAL_DAMAGE;
            lines.add(new TowerStatLine(damage, DamageUnits.inPoints(this.damageBase), DamageUnits.inPoints(now.damage())));
            this.cadence().ifPresent(lines::add);
            if (this.critChanceBase > 0f || now.critChance() > 0f) {
                lines.add(new TowerStatLine(TowerStatRow.CRIT_CHANCE, this.critChanceBase, now.critChance()));
            }
        }
        lines.addAll(this.ownStats());
        int auras = (int) this.context.towers().all().stream()
                .filter(other -> !other.buffFor(this).isNone())
                .count();
        return TowerInspection.of(this.type, this.price, lines)
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
        public void applyStacks(EnemyMob target, EffectKind kind, int stacks) {
            AbstractTower.this.applyStacks(target, kind, stacks);
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
