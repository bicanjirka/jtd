package td.tower;

import td.damage.AttackOrigin;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.economy.EconomyDelta;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyWalk;
import td.stat.DisruptionPenalty;
import td.tower.buff.TowerBuff;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    // A temporary buff a tower grants itself; published like an upgrade's buff, expired on the tick thread.
    private volatile TowerBuff timedBuff = TowerBuff.none();
    private int timedBuffEndsAt;
    private int currentTick;
    private boolean inKillHook;

    /**
     * Converts the cell coordinates to the pixel centre and range. A tower with no cooldown passes
     * {@code 0} and overrides {@link #cadence()}.
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
        this.stats = TowerStats.of(base, TowerBuff.none(), DisruptionPenalty.none(), scale);
    }

    /** Current stats as one snapshot. Never null. */
    protected TowerStats stats() {
        return this.stats;
    }

    /** Damage per hit, in hundredths. */
    protected int damageCurrent() {
        return this.stats.damage();
    }

    /** Ticks between shots. */
    protected int coolDownCurrent() {
        return this.stats.coolDown();
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
        TowerBuff externalBuff = this.context.towers().all().stream()
                .map(t -> t.buffFor(this))
                .reduce(TowerBuff.none(), TowerBuff::combine);
        TowerBuff totalBuff = externalBuff.combine(upgrades.totalBuff()).combine(this.timedBuff);
        this.stats = TowerStats.of(this.baseStats, totalBuff, this.disruption, this.context.getBoard().scale());
    }

    /**
     * Every hit goes through here, so accounting is right whichever subclass fires. The hit carries
     * this tower's current {@link AttackProfile}; the target rolls the crit.
     * <p>
     * Counts the damage that actually landed, not the damage fired. A kill adds the upgrades'
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
        if (this.removed) {
            return false;
        }
        boolean wasAlive = !enemy.isDead();
        Damage landed = enemy.doDamage(damage, attacker.withOrigin(this.origin));
        if (wasAlive) {
            this.damageDealt += landed.amount();
            if (enemy.isDead()) {
                this.killCount++;
                float bountyBonus = this.upgrades.totalBuff().bountyBonus();
                if (bountyBonus != 0f) {
                    this.context.economy().apply(EconomyDelta.credits(
                            Math.round(enemy.getBounty() * bountyBonus)));
                }
                this.runKillHook(enemy);
            }
        }
        return landed.critical();
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
     * How much stronger a burn this tower sets on {@code target} starts: its crit factor against it if
     * the ignition rolls a critical, otherwise {@code 1}. An ignition is no hit, so it spends nothing.
     */
    protected float potencyOfIgnition(EnemyMob target) {
        return target.rollsCrit(this.stats.attack()) ? target.critFactorFor(this.stats.attack()) : 1f;
    }

    /**
     * Puts an effect on {@code target}. Whatever damage it deals is credited to this tower as
     * periodic damage, so it never crits and never spends a mark.
     */
    protected void applyEffect(EnemyMob target, Function<DamageSink, Effect> effect) {
        target.applyEffect(effect.apply(d -> this.dealPeriodicDamage(target, d)));
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

    /**
     * Buffs this tower for {@code durationTicks} from now, replacing any timed buff (it never
     * stacks; granting again restarts the clock).
     */
    protected void grantTimedBuff(TowerBuff buff, int durationTicks) {
        this.timedBuffEndsAt = this.currentTick + durationTicks;
        this.timedBuff = buff;
        this.publishStats(this.upgrades);
    }

    /**
     * Start of this tower's turn: ends a timed buff that has run out, and re-reads the disruption at
     * its centre. Stats are republished only when one of them changed; compared against the
     * published stats rather than the last sample, so a republish from the EDT racing this one is
     * corrected on the next tick.
     */
    public void beginTick(int gameTime) {
        this.currentTick = gameTime;
        if (this.timedBuff != TowerBuff.none() && gameTime >= this.timedBuffEndsAt) {
            this.timedBuff = TowerBuff.none();
            this.publishStats(this.upgrades);
        }
        DisruptionPenalty sampled = this.context.disruptions().penaltyAt(this.centerX, this.centerY);
        this.disruption = sampled;
        if (!sampled.equals(this.stats.disruption())) {
            this.publishStats(this.upgrades);
        }
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
        this.experience.earn(bounty, this.currentTick);
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

    /** No upgrades by default. */
    public UpgradeTree upgradeTree() {
        return UpgradeTree.none();
    }

    public UpgradeState upgrades() {
        return this.upgrades;
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
     * Hook for an upgrade effect that no {@link TowerBuff} axis expresses; a no-op by default.
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
        return Optional.of(new TowerStatLine(TowerStat.FIRE_RATE, TICKS_PER_SECOND / (this.coolDownMax + 1),
                TICKS_PER_SECOND / (this.stats.coolDown() + 1)));
    }

    /** Physical by default. */
    protected DamageType damageType() {
        return DamageType.PHYSICAL;
    }

    /** Stats of this tower's own, such as a splash radius, shown after the shared ones. */
    protected List<TowerStatLine> ownStats() {
        return List.of();
    }

    /** What the tower does beyond its stats; nothing by default. */
    protected List<BehaviourLine> behaviours() {
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

    public TowerInspection inspect() {
        TowerStats now = this.stats;
        List<TowerStatLine> lines = new ArrayList<>();
        lines.add(new TowerStatLine(TowerStat.RANGE, this.rangeBase, now.range()));
        if (!this.isPassive()) {
            TowerStat damage = this.damageType() == DamageType.MAGIC ? TowerStat.MAGIC_DAMAGE : TowerStat.PHYSICAL_DAMAGE;
            lines.add(new TowerStatLine(damage, DamageUnits.inPoints(this.damageBase), DamageUnits.inPoints(now.damage())));
            this.cadence().ifPresent(lines::add);
            if (this.critChanceBase > 0f || now.critChance() > 0f) {
                lines.add(new TowerStatLine(TowerStat.CRIT_CHANCE, this.critChanceBase, now.critChance()));
            }
        }
        lines.addAll(this.ownStats());
        int auras = (int) this.context.towers().all().stream()
                .filter(other -> !other.buffFor(this).equals(TowerBuff.none()))
                .count();
        return TowerInspection.of(this.type, this.price, lines)
                .withName(this.displayName())
                .withBehaviours(this.behaviours())
                .withDescription(this.description())
                .withRecord(this.killCount, this.damageDealt)
                .withXp(this.experience.xp())
                .withUpgrades(this.upgrades())
                .withDisruption(now.disruption())
                .withAuras(auras);
    }

    /** Contributes nothing by default. */
    public TowerBuff buffFor(Tower other) {
        return TowerBuff.none();
    }

    /** Marks this tower gone, so effects it applied stop crediting it after the refund. */
    public void doCleanup() {
        this.removed = true;
    }
}
