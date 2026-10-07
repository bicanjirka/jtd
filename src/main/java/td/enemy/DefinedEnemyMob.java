package td.enemy;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.damage.Delivery;
import td.economy.EconomyDelta;
import td.effect.ActiveEffects;
import td.effect.DiminishingReturns;
import td.effect.Effect;
import td.effect.EffectCategory;
import td.effect.EffectKind;
import td.effect.EffectTransitions;
import td.enemy.MobMoments.Moment;
import td.stat.BaseStats;
import td.stat.EnemyStat;
import td.stat.StatAccumulator;
import td.stat.StatSheet;
import td.stat.StatView;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The one concrete {@link EnemyMob}: behaviour comes entirely from its {@link EnemyDefinition}'s
 * traits and abilities. Reaching the path's end charges a leak and kills the mob through the same
 * fade path as a combat kill.
 * <p>
 * Everything a mob is born with is {@code final} and set by the constructor; mutable state is
 * owned by the game-loop thread.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class DefinedEnemyMob implements EnemyMob {

    private final GameWorld gameWorld;
    private final EnemyDefinition definition;
    private final List<Trait> traits;
    private final Rank rank;
    private final int price;
    private final int healthMax;
    private final int pathIndex;
    private final float bodyScale;
    private final List<AbilityState> abilityStates;
    private final ActiveEffects activeEffects = new ActiveEffects();
    private final EffectTransitions effectTransitions = new EffectTransitions();
    private final PathMotion motion;
    private final MobMoments moments = new MobMoments();
    private final Map<EffectKind, DiminishingReturns> diminishing = new EnumMap<>(EffectKind.class);
    private final StatSheet stats;

    private int health;
    private EnemyInspection.Fate fate = EnemyInspection.Fate.ALIVE;
    private LifeStage stage;
    // Only meaningful while LIVE: off-board mobs walk in from and out to off-screen untargetable.
    private boolean onBoard;
    private int delay;
    private long entryOrdinal;
    // Null until the first cast; never exposed as null.
    private AbilityCast lastAbilityCast;
    private double facingRadians;
    private int ticksSinceSpawn;
    // Counts from spawn, so a fresh mob does not trivially satisfy an idle threshold.
    private int ticksSinceLastHit;
    // The part of a heal too small to land as a whole unit this tick; paid out once it adds up.
    private float regenCarry;

    public DefinedEnemyMob(EnemyDefinition definition, GameWorld gameWorld, SpawnParameters spawnParameters, Rank rank) {
        this.gameWorld = gameWorld;
        this.definition = definition;
        this.traits = definition.traitsFor(gameWorld.damageTally().mix());
        this.rank = rank;
        this.price = spawnParameters.price();
        // Divided after the spawn shape's multiplier, which the wave already applied to the health.
        int healthPoints = Math.round(spawnParameters.health() / definition.healthDivisor());
        this.health = DamageUnits.ofPoints(healthPoints);
        this.healthMax = DamageUnits.ofPoints(healthPoints);
        this.pathIndex = spawnParameters.pathIndex();
        BaseStats base = definition.baseStats()
                .with(EnemyStat.MOVE_SPEED, definition.baseSpeed() * spawnParameters.speedMultiplier());
        this.stats = new StatSheet(base, this::contributeStats);
        this.bodyScale = bodyScaleFor(definition.archetype(), gameWorld.getBoard().scale(), rank) * spawnParameters.sizeMultiplier();
        this.abilityStates = definition.abilities().stream().map(a -> AbilityState.forTrigger(a.trigger())).toList();
        this.motion = new PathMotion(gameWorld.level().pathAt(this.pathIndex), spawnParameters.localOffset(), gameWorld::getBoard);
        this.delay = spawnParameters.delayTicks();
        // From the rounded tick count, not the slot position: a fractional delay can round to zero
        // ticks, and an inactive mob with no delay to count down would never activate.
        this.stage = this.delay > 0 ? LifeStage.WAITING : LifeStage.LIVE;
        this.entryOrdinal = this.stage == LifeStage.LIVE ? gameWorld.enemies().recordEntry() : 0;
        this.onBoard = true;
    }

    private static float bodyScaleFor(BodyArchetype archetype, int scale, Rank rank) {
        return switch (archetype) {
            case CIRCLE -> scale / 6f;
            case SQUARE, TRIANGLE, GHOST -> scale / (float) (7 - rank.ordinal());
            // Fixed, so it always reads as the biggest thing on the board.
            case WARDEN -> scale / 1.5f;
            case WARDEN_EGG -> scale / 3f;
            case MENDER -> scale / (float) (7 - rank.ordinal());
        };
    }

    public <R> R accept(EnemyMobVisitor<R> visitor) {
        return visitor.visitDefined(this);
    }

    public int getHealth() {
        return this.health;
    }

    /** Full health in points, after every divisor and spawn-shape multiplier. */
    public int getMaxHealthPoints() {
        return this.healthMax / DamageUnits.PER_POINT;
    }

    public int getBounty() {
        return this.price;
    }

    @Override
    public long entryOrdinal() {
        return this.entryOrdinal;
    }

    @Override
    public boolean walkedWithin(double x, double y, double radius) {
        return this.motion.walkedWithin(x, y, radius);
    }

    @Override
    public float getHealthFraction() {
        return (float) this.health / this.healthMax;
    }

    public boolean isInactive() {
        return this.stage == LifeStage.WAITING;
    }

    public boolean isDead() {
        return this.stage == LifeStage.DEAD;
    }

    /** Inherited by an ability spawn. */
    @Override
    public Rank getRank() {
        return this.rank;
    }

    public EnemyDefinition definition() {
        return this.definition;
    }

    public float getBodyScale() {
        return this.bodyScale;
    }

    public BodyArchetype archetype() {
        return this.definition.archetype();
    }

    public Optional<SupportAura> supportAura() {
        return this.definition.supportAura();
    }

    public List<Trait> traits() {
        return this.traits;
    }

    public double getX() {
        return this.motion.x();
    }

    public double getY() {
        return this.motion.y();
    }

    /**
     * Position as of the previous tick, the interpolation source; equal to the current position at
     * spawn.
     */
    public double getPrevX() {
        return this.motion.prevX();
    }

    public double getPrevY() {
        return this.motion.prevY();
    }

    public double getFacingRadians() {
        return switch (this.definition.movement()) {
            case FixedMovement ignored -> 0.0;
            case PulseMovement ignored -> 0.0;
            case PathDirectionalMovement ignored -> this.motion.facingRadians();
            case RotorMovement ignored -> this.facingRadians;
        };
    }

    public int getProgression() {
        return this.motion.progression();
    }

    /** Places this mob anywhere along its path, so an ability spawn appears where its parent was. */
    void jumpToDistance(double distanceIntoLap) {
        this.motion.jumpTo(distanceIntoLap);
    }

    /** Passed on by an ability spawn so the new mob stays on its parent's path. */
    int pathIndex() {
        return this.pathIndex;
    }

    /** Places this freshly built mob where {@code other} is on the path. */
    void spawnAtSamePositionAs(DefinedEnemyMob other) {
        this.jumpToDistance(other.motion.distanceIntoLap());
    }

    /** Resolved {@link EnemyStat#MOVE_SPEED}, after traits and effects. */
    public float getSpeed() {
        return this.stats.value(EnemyStat.MOVE_SPEED);
    }

    /** This mob's resolved stats. */
    public StatView stats() {
        return this.stats;
    }

    private void contributeStats(StatAccumulator accumulator) {
        TraitContext context = this.traitContext();
        for (Trait trait : this.traits) {
            trait.modifiers(context).applyTo(accumulator);
        }
        this.activeEffects.contributeTo(accumulator);
    }

    /**
     * Applies the effect with its duration shortened by this mob's resistance to its kind and, for
     * hard crowd control, by diminishing returns. One that would last under a tick is blocked.
     */
    public void applyEffect(Effect effect) {
        float factor = effect.kind().resistedBy().map(stat -> 1f - this.stats.value(stat)).orElse(1f);
        Optional<DiminishingReturns> ladder = this.ladderFor(effect.kind());
        boolean active = this.activeEffects.has(effect.kind());
        if (ladder.isPresent()) {
            factor *= ladder.get().factorAt(this.ticksSinceSpawn, active);
        }
        Effect scaled = factor < 1f ? effect.withDurationScaledBy(factor) : effect;
        if (factor < 1f && scaled.authoredDurationTicks() < 1) {
            return;
        }
        ladder.ifPresent(steps -> steps.recordAt(this.ticksSinceSpawn, active));
        this.activeEffects.apply(scaled);
        this.stats.invalidate();
    }

    /** Hard crowd control climbs its own ladder of diminishing returns; nothing else has one. */
    private Optional<DiminishingReturns> ladderFor(EffectKind kind) {
        if (kind.category() != EffectCategory.HARD_CC) {
            return Optional.empty();
        }
        return Optional.of(this.diminishing.computeIfAbsent(kind, DiminishingReturns::of));
    }

    /**
     * For each kind whose diminishing returns are running, the share of its duration the next fresh
     * application would get, in kind order.
     */
    public Map<EffectKind, Float> diminishedFactors() {
        Map<EffectKind, Float> factors = new EnumMap<>(EffectKind.class);
        this.diminishing.forEach((kind, ladder) -> {
            if (ladder.stepAt(this.ticksSinceSpawn) > 0) {
                factors.put(kind, ladder.factorAt(this.ticksSinceSpawn, false));
            }
        });
        return factors;
    }

    public float reductionAgainst(DamageType type) {
        return HitResolution.reductionAgainst(type, this.stats);
    }

    public void breakShield(float fraction) {
        this.activeEffects.weakenShield(fraction);
        this.stats.invalidate();
    }

    public boolean hasEffect(EffectKind kind) {
        return this.activeEffects.has(kind);
    }

    public float critFactorFor(AttackProfile attacker) {
        return HitResolution.critFactor(attacker, this.stats);
    }

    /** The kinds an active effect currently keeps out. */
    public Set<EffectKind> blockedEffectKinds() {
        return this.activeEffects.blockedKinds();
    }

    /** How much speed the active chill takes away; {@code 0} for any other kind or when inactive. */
    public float effectLevel(EffectKind kind) {
        return kind == EffectKind.CHILL ? this.activeEffects.chillLevel() : 0f;
    }

    /** Stacks of the active {@code kind}; {@code 0} when inactive or the kind does not stack. */
    public int effectStacks(EffectKind kind) {
        return this.activeEffects.stacks(kind);
    }

    /** Ticks left on the active {@code kind}; empty when inactive or for a fuel pool. */
    public OptionalInt effectRemainingTicks(EffectKind kind) {
        return this.activeEffects.remainingTicks(kind);
    }

    public EnemyInspection.Fate fate() {
        return this.fate;
    }

    /** A snapshot of this mob for display. */
    public EnemyInspection inspect() {
        return EnemyInspection.of(this);
    }

    public Set<EffectKind> activeEffectKinds() {
        return this.activeEffects.activeKinds();
    }

    /** {@code -1} if never gained. */
    public int ticksSinceEffectGained(EffectKind kind, int gameTime) {
        return this.effectTransitions.ticksSinceGained(kind, gameTime);
    }

    /** {@code -1} if never lost. */
    public int ticksSinceEffectLost(EffectKind kind, int gameTime) {
        return this.effectTransitions.ticksSinceLost(kind, gameTime);
    }

    /** Spawned, on the board and alive. */
    public boolean validTarget() {
        return this.stage == LifeStage.LIVE && this.onBoard;
    }

    /**
     * Fully stealthed. The one place that reads the stat, so whatever lowers stealth (a reveal)
     * shows through to every tower and to selection at once.
     */
    @Override
    public boolean isHidden() {
        return this.stats.value(EnemyStat.STEALTH) >= 1f;
    }

    /**
     * Applies a hit through {@link HitResolution}, paying the bounty if it kills. A no-op on a dead
     * mob, so only the first of several same-tick hits counts as the kill.
     *
     * @return the damage that landed, capped at remaining health so overkill is not credited, and
     * {@link Damage#none()} if the mob is dead or untargetable
     */
    public Damage doDamage(Damage damage, AttackProfile attacker) {
        this.ticksSinceLastHit = 0;
        Damage landed = this.land(damage, attacker);
        // Health-dependent traits re-derive from the new health.
        this.stats.invalidate();
        return landed;
    }

    private Damage land(Damage damage, AttackProfile attacker) {
        if (this.isDead()) {
            return Damage.none();
        }
        Damage landed = Damage.none();
        if (this.validTarget()) {
            landed = HitResolution.resolve(damage, this.spendMark(attacker), this.stats, this.health,
                    this.gameWorld.random());
            this.health -= landed.amount();
            this.gameWorld.damageTally().record(landed);
            if (landed.amount() > 0) {
                this.moments.markPending(Moment.DAMAGE_TAKEN);
            }
            if (landed.critical()) {
                this.moments.markPending(Moment.CRITICAL_HIT);
            }
        }
        if (this.health <= 0) {
            this.die();
            this.fate = EnemyInspection.Fate.KILLED;
            int score = Math.round(this.price * this.rank.scoreMultiplier());
            this.gameWorld.economy().apply(EconomyDelta.kill(this.price, score));
            this.gameWorld.enemies().reportDeath(this);
        }
        return landed;
    }

    /**
     * A hit on a marked enemy is a guaranteed crit and ends the mark. Periodic damage never spends it,
     * nor does a hit on a crit-immune enemy: the mark waits.
     */
    private AttackProfile spendMark(AttackProfile attacker) {
        boolean spent = attacker.delivery() == Delivery.HIT && this.stats.value(EnemyStat.RESILIENCE) < 100f
                && this.activeEffects.consume(EffectKind.MARKED);
        return spent ? attacker.withGuaranteedCrit() : attacker;
    }

    private TraitContext traitContext() {
        return new TraitContext(this.getHealthFraction());
    }

    /**
     * {@code -1} while alive. Captured on the first {@code doTick} after death, so the fade follows
     * the simulation clock rather than the repaint rate.
     */
    public int ticksSinceDeath(int gameTime) {
        return this.moments.ticksSince(Moment.DEATH, gameTime);
    }

    /**
     * {@code -1} if never. Captured on the next {@code doTick}, like {@link #ticksSinceDeath},
     * because {@code doDamage} has no game time.
     */
    public int ticksSinceCriticalHit(int gameTime) {
        return this.moments.ticksSince(Moment.CRITICAL_HIT, gameTime);
    }

    /** {@code -1} if never. Captured like {@link #ticksSinceCriticalHit}. */
    public int ticksSinceDamageTaken(int gameTime) {
        return this.moments.ticksSince(Moment.DAMAGE_TAKEN, gameTime);
    }

    public void recordAbilityCast(EffectKind kind, float radius, int gameTime) {
        this.lastAbilityCast = new AbilityCast(kind, radius, gameTime);
    }

    public Optional<AbilityCast> lastAbilityCast() {
        return Optional.ofNullable(this.lastAbilityCast);
    }

    public void recordAbilitySpawn(int gameTime) {
        this.moments.record(Moment.ABILITY_SPAWN, gameTime);
    }

    int ticksSinceSpawn() {
        return this.ticksSinceSpawn;
    }

    int ticksSinceLastHit() {
        return this.ticksSinceLastHit;
    }

    /** {@code -1} for a mob that came from a wave. */
    public int ticksSinceAbilitySpawn(int gameTime) {
        return this.moments.ticksSince(Moment.ABILITY_SPAWN, gameTime);
    }

    /**
     * Counts down the spawn delay; for a live mob resolves effects, moves and evaluates abilities;
     * on the tick its death is captured, evaluates abilities once more. A damage-over-time kill
     * skips movement.
     */
    public void doTick(int gameTime) {
        // doDamage() has no game time, and a hit can land in another phase of the same tick.
        this.moments.capturePending(gameTime);
        if (this.stage == LifeStage.WAITING) {
            this.delay--;
            if (this.delay == 0) {
                this.stage = LifeStage.LIVE;
                this.entryOrdinal = this.gameWorld.enemies().recordEntry();
                this.doTick(gameTime);
            }
            return;
        }
        if (this.isDead()) {
            // Gated on the death capture, not on death: a tower can kill this mob after its own
            // doTick already ran this tick.
            if (this.ticksSinceDeath(gameTime) == 0) {
                this.evaluateAbilities(gameTime);
            }
            return;
        }
        // Read before tick(): an effect in its last tick must still slow this tick's movement.
        float speed = this.stats.value(EnemyStat.MOVE_SPEED);
        // Applied here, not through tick()'s sink: a heal is not a negative damagePerTick.
        this.regenCarry += this.stats.value(EnemyStat.REGENERATION);
        int whole = (int) this.regenCarry;
        this.regenCarry -= whole;
        int healed = Math.min(this.healthMax, this.health + whole);
        boolean healthChanged = healed != this.health;
        this.health = healed;
        if (healthChanged || !this.activeEffects.isEmpty()) {
            // The pace of stack decay is the same scale that spirit puts on heals and shields.
            this.activeEffects.tick(Math.max(0f, 1f + this.stats.value(EnemyStat.SPIRIT) / 100f));
            this.stats.invalidate();
        }
        // After tick() so an expiry is seen the tick it happens; before the dead-return so a lethal
        // damage-over-time tick still records the loss.
        this.effectTransitions.observe(this.activeEffects.activeKinds(), gameTime);
        if (this.isDead()) {
            return;
        }
        if (this.motion.advance(speed)) {
            this.leak();
            return;
        }
        this.onBoard = this.motion.isOnBoard();
        if (this.onBoard) {
            this.definition.disruption().ifPresent(aura -> this.gameWorld.disruptions().add(this.getX(), this.getY(), aura));
        }
        this.ticksSinceSpawn++;
        if (this.ticksSinceLastHit < Integer.MAX_VALUE) {
            this.ticksSinceLastHit++;
        }
        switch (this.definition.movement()) {
            case RotorMovement rotor -> this.facingRadians += rotor.radiansPerTick();
            case FixedMovement ignored -> {
            }
            case PathDirectionalMovement ignored -> {
            }
            case PulseMovement ignored -> {
            }
        }
        if (!this.isStopped()) {
            this.evaluateAbilities(gameTime);
        }
    }

    /**
     * Costs a life and the score this mob's bounty would have paid, then kills it through the
     * normal fade path. It does not loop back, so no tower gets a second chance at its bounty.
     */
    private void leak() {
        this.die();
        this.fate = EnemyInspection.Fate.LEAKED;
        this.gameWorld.economy().apply(EconomyDelta.leak(this.price));
        this.gameWorld.enemies().reportDeath(this);
    }

    private void die() {
        this.stage = LifeStage.DEAD;
        this.moments.markPending(Moment.DEATH);
    }

    private void evaluateAbilities(int gameTime) {
        List<Ability> abilities = this.definition.abilities();
        for (int i = 0; i < abilities.size(); i++) {
            Ability ability = abilities.get(i);
            AbilityState state = this.abilityStates.get(i);
            AbilityContext context = new MobAbilityContext(this, this.gameWorld, gameTime);
            if (AbilityEvaluator.shouldFire(ability.trigger(), state, context)) {
                AbilityEvaluator.execute(ability.action(), context);
            }
        }
    }

    private enum LifeStage {
        /** Counting down its spawn delay: not on the board yet. */
        WAITING,
        LIVE,
        /** Fading out; stays in the roster until the fade completes. */
        DEAD
    }
}
