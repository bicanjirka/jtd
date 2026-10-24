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
import td.effect.ShieldGauge;
import td.enemy.MobMoments.Moment;
import td.stat.BaseStats;
import td.stat.EnemyStat;
import td.stat.StatAccumulator;
import td.stat.StatModifier;
import td.stat.StatRecorder;
import td.stat.StatSheet;
import td.stat.StatView;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
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
    private final int pathIndex;
    private final float bodyScale;
    private final List<AbilityState> abilityStates;
    private final ActiveEffects activeEffects = new ActiveEffects();
    private final EffectTransitions effectTransitions = new EffectTransitions();
    private final PathMotion motion;
    private final MobMoments moments = new MobMoments();
    private final MobHealth health;
    private final Map<EffectKind, DiminishingReturns> diminishing = new EnumMap<>(EffectKind.class);
    private final StatSheet stats;
    private final BaseStats<EnemyStat> base;

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
    private int dodges;

    public DefinedEnemyMob(EnemyDefinition definition, GameWorld gameWorld, SpawnParameters spawnParameters, Rank rank) {
        this.gameWorld = gameWorld;
        this.definition = definition;
        this.traits = definition.traitsFor(gameWorld.damageTally().mix());
        this.rank = rank;
        this.price = Math.round(spawnParameters.price() * gameWorld.playtestRules().bountyScale());
        // Divided after the spawn shape's multiplier, which the wave already applied to the health.
        int healthPoints = Math.round(spawnParameters.health() / definition.healthDivisor()
                * gameWorld.playtestRules().enemyHealthScale());
        this.health = new MobHealth(DamageUnits.ofPoints(healthPoints));
        this.pathIndex = spawnParameters.pathIndex();
        this.base = definition.baseStats()
                .with(EnemyStat.MOVE_SPEED, definition.baseSpeed() * spawnParameters.speedMultiplier());
        this.stats = new StatSheet(this.base, this::contributeStats);
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
            case CIRCLE, SALAMANDER -> scale / 6f;
            case SQUARE, TRIANGLE, GHOST, COURIER, SKIRMISHER -> scale / (float) (7 - rank.ordinal());
            case MITE -> scale / 10f;
            case YETI -> scale / 5f;
            case JUGGERNAUT -> scale / (float) (6 - rank.ordinal());
            // Fixed, so it always reads as the biggest thing on the board.
            case WARDEN -> scale / 1.5f;
            case WARDEN_EGG -> scale / 3f;
            case MENDER, SHIELDBEARER -> scale / (float) (7 - rank.ordinal());
        };
    }

    public <R> R accept(EnemyMobVisitor<R> visitor) {
        return visitor.visitDefined(this);
    }

    public int getHealth() {
        return this.health.current();
    }

    /** Full health in points, after every divisor and spawn-shape multiplier. */
    public int getMaxHealthPoints() {
        return this.health.max() / DamageUnits.PER_POINT;
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
        return this.health.fraction();
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

    @Override
    public boolean appliesEffects() {
        return this.definition.abilities().stream().anyMatch(ability -> ability.action().appliesEffects());
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

    @Override
    public Vec2 positionAfter(int ticks) {
        return this.motion.positionAfter(this.getSpeed() * ticks);
    }

    /** This mob's resolved stats. */
    public StatView stats() {
        return this.stats;
    }

    /** What {@code stat} is before any trait or effect. */
    public float baseStat(EnemyStat stat) {
        return this.base.value(stat);
    }

    /** The board's cell size in pixels. */
    float cellSize() {
        return this.gameWorld.getBoard().scale();
    }

    EnemyCatalog enemyCatalog() {
        return this.gameWorld.getEnemyCatalog();
    }

    /**
     * Every kind that is active or whose diminishing returns run, ordered by the tick it was last gained (oldest
     * first, ties in kind order).
     */
    public List<EffectKind> effectKindsByGain() {
        Set<EffectKind> kinds = EnumSet.noneOf(EffectKind.class);
        kinds.addAll(this.activeEffects.activeKinds());
        kinds.addAll(this.diminishedFactors().keySet());
        return this.effectTransitions.inGainedOrder(kinds);
    }

    /** What {@code stat} is made of now: base, each trait and active effect that changes it, and the result. */
    public StatBreakdown breakdown(EnemyStat stat) {
        TraitContext context = this.traitContext();
        List<StatPart> parts = new ArrayList<>();
        for (Trait trait : this.traits) {
            StatModifier modifier = trait.modifiers(context).get(stat);
            if (!modifier.equals(StatModifier.none())) {
                parts.add(new StatPart(trait.entry().term(), modifier));
            }
        }
        for (EffectKind kind : this.activeEffects.activeKinds()) {
            StatModifier modifier = StatRecorder.of(stat, acc -> this.activeEffects.contributionOf(kind, acc));
            if (!modifier.equals(StatModifier.none())) {
                parts.add(new StatPart(kind, modifier));
            }
        }
        return new StatBreakdown(stat, this.baseStat(stat), parts, this.stats.value(stat));
    }

    /** The active kind that keeps {@code kind} out now, if one does. */
    public Optional<EffectKind> keptOutBy(EffectKind kind) {
        return this.activeEffects.keptOutBy(kind);
    }

    public OptionalDouble effectFullness(EffectKind kind) {
        return this.activeEffects.fullness(kind);
    }

    public OptionalInt effectClockTicks(EffectKind kind) {
        return this.activeEffects.clockTicks(kind);
    }

    private void contributeStats(StatAccumulator<EnemyStat> accumulator) {
        TraitContext context = this.traitContext();
        for (Trait trait : this.traits) {
            trait.modifiers(context).applyTo(accumulator);
        }
        this.activeEffects.contributeTo(accumulator);
    }

    /**
     * Applies the effect with its duration shortened by this mob's resistance to its kind and, for
     * hard crowd control, by diminishing returns. One that would last under a tick is blocked. Under
     * Inversion a heal comes in as damage and a shield as one hit. A shield's pool is scaled once, as
     * it is applied, by spirit and healing received.
     */
    public void applyEffect(Effect effect) {
        Optional<Effect> inversion = this.activeEffects.find(EffectKind.INVERSION);
        if (inversion.isPresent() && effect.kind() == EffectKind.SHIELD) {
            this.strikeInsteadOfShielding(effect, inversion.get());
            return;
        }
        Effect received = effect.kind() == EffectKind.SHIELD
                ? effect.withShieldScaledBy(Math.max(0f, 1f + this.stats.value(EnemyStat.SPIRIT) / 100f)
                        * this.stats.value(EnemyStat.HEALING_RECEIVED))
                : effect;
        this.applyResisted(inversion.map(hex -> received.invertedThrough(hex.sink())).orElse(received));
    }

    /**
     * An Inverted enemy takes a shield as one hit of its authored pool, credited to whoever cast the
     * inversion; the shield itself never goes on.
     */
    private void strikeInsteadOfShielding(Effect shield, Effect inversion) {
        inversion.sink().apply(Damage.magic(Math.round(shield.shieldRemaining())));
    }

    private void applyResisted(Effect effect) {
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

    /** Ends every effect at once, a lasting mark included. */
    public void clearEffects() {
        this.activeEffects.activeKinds().forEach(this.activeEffects::consume);
        this.stats.invalidate();
    }

    /** Back to full health at once. Nothing is credited, and a dead mob stays dead. */
    public void restoreHealth() {
        if (!this.isDead()) {
            this.health.restore();
            this.stats.invalidate();
        }
    }

    @Override
    public void dispelRestoratives() {
        this.activeEffects.consume(EffectKind.SHIELD);
        this.activeEffects.consume(EffectKind.HEAL);
        this.stats.invalidate();
    }

    @Override
    public void dispelBuffs() {
        this.activeEffects.activeKinds().stream().filter(kind -> kind.category() == EffectCategory.BUFF)
                .forEach(this.activeEffects::consume);
        this.stats.invalidate();
    }

    @Override
    public void loseStacks(EffectKind kind, int stacks) {
        this.activeEffects.loseStacks(kind, stacks);
        this.stats.invalidate();
    }

    @Override
    public float spirit() {
        return this.stats.value(EnemyStat.SPIRIT);
    }

    @Override
    public boolean freezeDiminished() {
        return this.diminishedFactors().containsKey(EffectKind.FREEZE);
    }

    public void breakShield(float fraction) {
        this.activeEffects.weakenShield(fraction);
        this.stats.invalidate();
    }

    public boolean hasEffect(EffectKind kind) {
        return this.activeEffects.has(kind);
    }

    public boolean rollsCrit(AttackProfile attacker) {
        return attacker.delivery() == Delivery.HIT
                && HitResolution.rollsCrit(attacker, this.stats, this.gameWorld.random());
    }

    public float critFactorFor(AttackProfile attacker) {
        return HitResolution.critFactor(attacker, this.stats);
    }

    /** The kinds an active effect currently keeps out. */
    public Set<EffectKind> blockedEffectKinds() {
        return this.activeEffects.blockedKinds();
    }

    /** The active shield's pool, in points. */
    public ShieldGauge shieldGauge() {
        return this.activeEffects.shieldGauge();
    }

    /** The share of its pool the active shield has left; {@code 0} without one. */
    public float shieldFraction() {
        ShieldGauge gauge = this.activeEffects.shieldGauge();
        return gauge.max() > 0f ? gauge.remaining() / gauge.max() : 0f;
    }

    /** How much speed the active chill takes away; {@code 0} for any other kind or when inactive. */
    public float effectLevel(EffectKind kind) {
        return kind == EffectKind.CHILL ? this.activeEffects.chillLevel() : 0f;
    }

    @Override
    public List<Effect> activeEffects() {
        return this.activeEffects.effects();
    }

    @Override
    public long damageTaken() {
        return this.health.damageTaken();
    }

    @Override
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

    /** This mob was replaced by its own spawn: it is gone, but neither killed nor leaked. */
    void markHatched() {
        this.fate = EnemyInspection.Fate.HATCHED;
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
     * mob, so only the first of several same-tick hits counts as the kill. A dodged hit changes nothing
     * but the dodge count: it is not even a hit for the idle timer.
     *
     * @return what reached health, capped at remaining health so overkill is not credited, and what a
     * shield absorbed; {@link HitResult#none()} if the mob is dead or untargetable
     */
    @Override
    public HitResult takeHit(Damage damage, AttackProfile attacker) {
        AttackProfile attack = attacker;
        if (this.validTarget()) {
            attack = this.spendMark(attacker);
            if (HitResolution.dodges(attack, this.stats, this.gameWorld.random())) {
                this.dodges++;
                this.moments.markPending(Moment.DODGE);
                return HitResult.dodge();
            }
        }
        this.ticksSinceLastHit = 0;
        HitResult result = this.land(damage, attack);
        this.discharge(attack, result.landed());
        // Health-dependent traits re-derive from the new health.
        this.stats.invalidate();
        return result;
    }

    /** Armor and the rest of the hit formula first, then the shield's pool, then the cap at health. */
    private HitResult land(Damage damage, AttackProfile attacker) {
        if (this.isDead()) {
            return HitResult.none();
        }
        HitResult result = HitResult.none();
        if (this.validTarget()) {
            Damage mitigated = HitResolution.resolve(damage, attacker, this.stats, this.gameWorld.random());
            boolean bypasses = attacker.ignoresShields() || (mitigated.critical() && attacker.critsPierceShields());
            int absorbed = bypasses ? 0 : this.activeEffects.absorb(mitigated.type(), mitigated.amount());
            if (absorbed > 0) {
                this.gameWorld.enemies().reportShielded(this, Damage.of(mitigated.type(), absorbed));
            }
            Damage landed = this.health.lose(new Damage(mitigated.amount() - absorbed, mitigated.type(), mitigated.critical()));
            result = new HitResult(landed, absorbed);
            this.gameWorld.damageTally().record(Damage.of(mitigated.type(), result.credited()));
            if (result.credited() > 0) {
                this.moments.markPending(Moment.DAMAGE_TAKEN);
            }
            if (landed.critical()) {
                this.moments.markPending(Moment.CRITICAL_HIT);
            }
        }
        if (this.health.depleted()) {
            this.kill();
        }
        return result;
    }

    /** Pays the bounty and its score, then starts the fade. */
    private void kill() {
        this.die();
        this.fate = EnemyInspection.Fate.KILLED;
        int score = Math.round(this.price * this.rank.scoreMultiplier());
        this.gameWorld.economy().apply(EconomyDelta.kill(this.price, score));
        this.gameWorld.enemies().reportDeath(this);
    }

    /**
     * A hit from another attacker discharges a charge: it lands its share of the hit again as magic,
     * twice that on a crit, credited to whoever charged it. Periodic damage never discharges one.
     */
    private void discharge(AttackProfile attacker, Damage landed) {
        if (this.isDead() || attacker.delivery() != Delivery.HIT || landed.amount() == 0) {
            return;
        }
        Optional<Effect> charge = this.activeEffects.find(EffectKind.CHARGED);
        if (charge.isEmpty() || charge.get().origin() == attacker.origin()) {
            return;
        }
        this.activeEffects.consume(EffectKind.CHARGED);
        this.stats.invalidate();
        float share = Effect.CHARGE_DISCHARGE_SHARE * (landed.critical() ? 2f : 1f);
        charge.get().sink().apply(Damage.magic(Math.round(landed.amount() * share)));
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

    /** How many hits it has dodged. */
    int dodges() {
        return this.dodges;
    }

    /** {@code -1} if it never dodged. Captured like {@link #ticksSinceCriticalHit}. */
    public int ticksSinceDodge(int gameTime) {
        return this.moments.ticksSince(Moment.DODGE, gameTime);
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
            this.countDownSpawnDelay(gameTime);
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
        boolean healthChanged = this.health.regenerate(this.stats.value(EnemyStat.REGENERATION));
        if (healthChanged || !this.activeEffects.isEmpty()) {
            this.tickEffects(speed);
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
        this.trackBoardPresence();
        this.countLiveTick();
        this.turn();
        if (!this.isSilenced()) {
            this.evaluateAbilities(gameTime);
        }
    }

    private void countDownSpawnDelay(int gameTime) {
        this.delay--;
        if (this.delay == 0) {
            this.stage = LifeStage.LIVE;
            this.entryOrdinal = this.gameWorld.enemies().recordEntry();
            this.doTick(gameTime);
        }
    }

    private void tickEffects(float speed) {
        // The pace of stack decay is the same scale that spirit puts on heals and shields.
        int cellSize = this.gameWorld.getBoard().scale();
        this.activeEffects.tick(Math.max(0f, 1f + this.stats.value(EnemyStat.SPIRIT) / 100f),
                cellSize > 0 ? speed / cellSize : 0f);
        this.stats.invalidate();
    }

    private void trackBoardPresence() {
        this.onBoard = this.motion.isOnBoard();
        if (this.onBoard) {
            this.definition.disruption().ifPresent(aura -> this.gameWorld.disruptions().add(this.getX(), this.getY(), aura));
        }
    }

    private void countLiveTick() {
        this.ticksSinceSpawn++;
        if (this.ticksSinceLastHit < Integer.MAX_VALUE) {
            this.ticksSinceLastHit++;
        }
    }

    private void turn() {
        switch (this.definition.movement()) {
            case RotorMovement rotor -> this.facingRadians += rotor.radiansPerTick();
            case FixedMovement ignored -> {
            }
            case PathDirectionalMovement ignored -> {
            }
            case PulseMovement ignored -> {
            }
        }
    }

    /**
     * Costs a life and the score this mob's bounty would have paid, then kills it through the
     * normal fade path. It does not loop back, so no tower gets a second chance at its bounty.
     */
    private void leak() {
        this.die();
        this.fate = EnemyInspection.Fate.LEAKED;
        int lives = Math.min(this.definition.livesOnLeak(), Math.max(1, this.gameWorld.economy().getLives()));
        this.gameWorld.economy().apply(EconomyDelta.leak(this.price, lives));
        this.gameWorld.enemies().reportLeak(this);
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
