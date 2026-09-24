package td.enemy;

import td.damage.Damage;
import td.economy.EconomyDelta;
import td.effect.ActiveEffects;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.EffectTemplate;
import td.effect.EffectTransitions;
import td.enemy.MobMoments.Moment;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.Vec2;

import java.util.List;
import java.util.Optional;
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

    /**
     * Health is stored in hundredths, the unit towers deal damage in, so a percentage resist or a
     * damage-over-time tick can remove a fraction of a point.
     */
    private static final int HEALTH_UNITS_PER_POINT = 100;

    private final GameWorld gameWorld;
    private final EnemyDefinition definition;
    private final Rank rank;
    private final int price;
    private final int healthMax;
    private final int pathIndex;
    // Folded into every speed recomputation; applied once at construction, the first hit
    // would wipe it.
    private final float shapeSpeedMultiplier;
    private final float bodyScale;
    private final List<AbilityState> abilityStates;
    private final ActiveEffects activeEffects = new ActiveEffects();
    private final EffectTransitions effectTransitions = new EffectTransitions();
    private final PathMotion motion;
    private final MobMoments moments = new MobMoments();

    private int health;
    private boolean inactive;
    private boolean validTarget;
    private boolean dead = false;
    /** Intrinsic px/tick; effects multiply it on read, never baked in. */
    private float speed;
    private int delay;
    // Null until the first cast; never exposed as null.
    private AbilityCast lastAbilityCast;
    private double facingRadians;
    private int ticksSinceSpawn;
    // Counts from spawn, so a fresh mob does not trivially satisfy an idle threshold.
    private int ticksSinceLastHit;

    public DefinedEnemyMob(EnemyDefinition definition, GameWorld gameWorld, SpawnParameters spawnParameters, Rank rank) {
        this.gameWorld = gameWorld;
        this.definition = definition;
        this.rank = rank;
        this.price = spawnParameters.price();
        // Divided after the spawn shape's multiplier, which the wave already applied to the health.
        int healthPoints = Math.round(spawnParameters.health() / definition.healthDivisor());
        this.health = healthPoints * HEALTH_UNITS_PER_POINT;
        this.healthMax = healthPoints * HEALTH_UNITS_PER_POINT;
        this.pathIndex = spawnParameters.pathIndex();
        this.shapeSpeedMultiplier = spawnParameters.speedMultiplier();
        this.speed = definition.baseSpeed() * spawnParameters.speedMultiplier();
        this.bodyScale = bodyScaleFor(definition.archetype(), gameWorld.getBoard().scale(), rank) * spawnParameters.sizeMultiplier();
        this.abilityStates = definition.abilities().stream().map(a -> AbilityState.forTrigger(a.trigger())).toList();
        this.motion = new PathMotion(gameWorld.level().pathAt(this.pathIndex), spawnParameters.localOffset(), gameWorld::getBoard);
        this.delay = spawnParameters.delayTicks();
        // From the rounded tick count, not the slot position: a fractional delay can round to zero
        // ticks, and an inactive mob with no delay to count down would never activate.
        this.inactive = this.delay > 0;
        this.validTarget = !this.inactive;
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

    public int getBounty() {
        return this.price;
    }

    public float getHealthFraction() {
        return (float) this.health / this.healthMax;
    }

    public boolean isInactive() {
        return this.inactive;
    }

    public boolean isDead() {
        return this.dead;
    }

    /** Inherited by an ability spawn. */
    public Rank getRank() {
        return this.rank;
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
        return this.definition.traits();
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

    /** Places this freshly built mob where {@code other} is on the path. */
    void spawnAtSamePositionAs(DefinedEnemyMob other) {
        this.jumpToDistance(other.motion.distanceIntoLap());
    }

    /** Intrinsic speed times the active effects' multiplier. */
    public float getSpeed() {
        return this.speed * this.activeEffects.speedMultiplier();
    }

    /** Rejects the effect if any trait blocks its kind. */
    public void applyEffect(Effect effect) {
        for (Trait trait : this.definition.traits()) {
            if (trait.blocksEffect(effect.kind())) {
                return;
            }
        }
        this.activeEffects.apply(effect);
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

    /** Spawned, on the board, alive and not hidden by a trait. */
    public boolean validTarget() {
        if (this.inactive || !this.validTarget || this.dead) {
            return false;
        }
        TraitContext context = this.traitContext();
        for (Trait trait : this.definition.traits()) {
            if (!trait.isValidTarget(context)) {
                return false;
            }
        }
        return true;
    }

    public boolean validTarget(Type type) {
        return this.validTarget() && type.equals(this.effectiveType());
    }

    public boolean validTarget(Type type1, Type type2) {
        return this.validTarget(type1) || this.validTarget(type2);
    }

    /**
     * {@link Type#INVISIBLE} while an invisibility effect is active, otherwise the authored type,
     * so any enemy can be made invisible by an effect.
     */
    private Type effectiveType() {
        return this.activeEffects.isInvisible() ? Type.INVISIBLE : this.definition.mobType();
    }

    /**
     * Applies a hit, paying the bounty if it kills, then recomputes speed from the traits. A no-op
     * on a dead mob, so only the first of several same-tick hits counts as the kill.
     *
     * @return the damage that landed: after traits and shield, capped at remaining health so
     * overkill is not credited, and {@link Damage#none()} if the mob is dead or untargetable
     */
    public Damage doDamage(Damage damage) {
        this.ticksSinceLastHit = 0;
        Damage landed = this.land(damage);
        TraitContext context = this.traitContext();
        float factor = 1f;
        for (Trait trait : this.definition.traits()) {
            factor *= trait.speedFactor(context);
        }
        this.speed = this.definition.baseSpeed() * this.shapeSpeedMultiplier * factor;
        return landed;
    }

    private Damage land(Damage damage) {
        if (this.dead) {
            return Damage.none();
        }
        Damage landed = Damage.none();
        if (this.validTarget()) {
            landed = this.activeEffects.applyShield(this.absorb(damage)).cappedAt(this.health);
            this.health -= landed.amount();
            if (landed.amount() > 0) {
                this.moments.markPending(Moment.DAMAGE_TAKEN);
            }
            if (landed.critical()) {
                this.moments.markPending(Moment.CRITICAL_HIT);
            }
        }
        if (this.health <= 0) {
            this.die();
            int score = Math.round(this.price * this.rank.scoreMultiplier());
            this.gameWorld.economy().apply(EconomyDelta.kill(this.price, score));
            this.gameWorld.enemies().reportDeath();
        }
        return landed;
    }

    private Damage absorb(Damage incoming) {
        Damage result = incoming;
        TraitContext context = this.traitContext();
        for (Trait trait : this.definition.traits()) {
            result = trait.onHit(result, context);
        }
        return result;
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

    public int fadeDurationTicks() {
        return 3 * this.rank.ordinal() + 6;
    }

    public boolean isFadeComplete(int gameTime) {
        return this.ticksSinceDeath(gameTime) > this.fadeDurationTicks();
    }

    /**
     * Clamped, because {@code ticksSinceDeath} is {@code -1} between a mid-tick death and the next
     * {@code doTick}.
     */
    public int fadeAlpha(int ticksSinceDeath) {
        int alpha = 255 - (ticksSinceDeath * (255 / (this.fadeDurationTicks() + 1)));
        return Math.min(255, Math.max(alpha, 0));
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
        if (this.inactive) {
            if (this.delay > 0) {
                this.delay--;
                if (this.delay == 0) {
                    this.inactive = false;
                    this.validTarget = true;
                    this.doTick(gameTime);
                }
            }
            return;
        }
        if (this.dead) {
            // Gated on the death capture, not on death: a tower can kill this mob after its own
            // doTick already ran this tick.
            if (this.ticksSinceDeath(gameTime) == 0) {
                this.evaluateAbilities(gameTime);
            }
            return;
        }
        // Read before tick(): an effect in its last tick must still slow this tick's movement.
        float speedMultiplier = this.activeEffects.speedMultiplier();
        // Applied here, not through tick()'s sink: a heal is not a negative damagePerTick.
        this.health = Math.min(this.healthMax, this.health + this.activeEffects.healPerTick());
        this.activeEffects.tick();
        // After tick() so an expiry is seen the tick it happens; before the dead-return so a lethal
        // damage-over-time tick still records the loss.
        this.effectTransitions.observe(this.activeEffects.activeKinds(), gameTime);
        if (this.dead) {
            return;
        }
        if (this.motion.advance(this.speed * speedMultiplier)) {
            this.leak();
            return;
        }
        this.validTarget = this.motion.isOnBoard();
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
        // A frozen mob cannot cast.
        if (!this.activeEffectKinds().contains(EffectKind.FREEZE)) {
            this.evaluateAbilities(gameTime);
        }
    }

    /**
     * Costs a life and the score this mob's bounty would have paid, then kills it through the
     * normal fade path. It does not loop back, so no tower gets a second chance at its bounty.
     */
    private void leak() {
        this.die();
        this.gameWorld.economy().apply(EconomyDelta.leak(this.price));
        this.gameWorld.enemies().reportDeath();
    }

    private void die() {
        this.validTarget = false;
        this.dead = true;
        this.moments.markPending(Moment.DEATH);
    }

    private void evaluateAbilities(int gameTime) {
        List<Ability> abilities = this.definition.abilities();
        for (int i = 0; i < abilities.size(); i++) {
            Ability ability = abilities.get(i);
            AbilityState state = this.abilityStates.get(i);
            AbilityContext context = new MobAbilityContext(gameTime);
            if (AbilityEvaluator.shouldFire(ability.trigger(), state, context)) {
                AbilityEvaluator.execute(ability.action(), context);
            }
        }
    }

    public String getInfoString() {
        return this.definition.displayName() + "\n\n" + this.definition.description() + "\n\nRank: "
                + titleCase(this.rank) + "   Health: " + this.definition.baseHealth() + "   Bounty: "
                + this.definition.price();
    }

    private static String titleCase(Rank rank) {
        String name = rank.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    /** Resolves ability data against this mob's world, position and definition. */
    private final class MobAbilityContext implements AbilityContext {

        private final int gameTime;

        MobAbilityContext(int gameTime) {
            this.gameTime = gameTime;
        }

        @Override
        public float healthFraction() {
            return DefinedEnemyMob.this.getHealthFraction();
        }

        @Override
        public int ticksSinceSpawn() {
            return DefinedEnemyMob.this.ticksSinceSpawn;
        }

        @Override
        public int ticksSinceLastHit() {
            return DefinedEnemyMob.this.ticksSinceLastHit;
        }

        @Override
        public boolean justDied() {
            return DefinedEnemyMob.this.isDead() && DefinedEnemyMob.this.ticksSinceDeath(this.gameTime) == 0;
        }

        @Override
        public boolean justTookCriticalHit() {
            return DefinedEnemyMob.this.ticksSinceCriticalHit(this.gameTime) == 0;
        }

        @Override
        public boolean justTookDamage() {
            return DefinedEnemyMob.this.ticksSinceDamageTaken(this.gameTime) == 0;
        }

        @Override
        public void applyEffect(EffectTemplate template, EffectTarget target) {
            switch (target) {
                case SelfTarget ignored -> DefinedEnemyMob.this.applyEffect(template.toEffect(this::creditNoOne));
                case RadiusTarget radiusTarget -> this.applyToOthersInRadius(template, radiusTarget.radius());
            }
            float castRadius = switch (target) {
                case SelfTarget ignored -> 0f;
                case RadiusTarget radiusTarget -> radiusTarget.radius();
            };
            DefinedEnemyMob.this.recordAbilityCast(template.kind(), castRadius, this.gameTime);
        }

        private void applyToOthersInRadius(EffectTemplate template, float radius) {
            float radius2 = radius * radius;
            double selfX = DefinedEnemyMob.this.getX();
            double selfY = DefinedEnemyMob.this.getY();
            for (EnemyMob other : DefinedEnemyMob.this.gameWorld.enemies().getEnemies()) {
                if (other == DefinedEnemyMob.this || !other.validTarget()) {
                    continue;
                }
                double dx = other.getX() - selfX;
                double dy = other.getY() - selfY;
                if (dx * dx + dy * dy <= radius2) {
                    other.applyEffect(template.toEffect(this::creditNoOne));
                }
            }
        }

        /** Ability effects deal no direct damage, so this sink is never invoked. */
        private void creditNoOne(Damage damage) {
        }

        @Override
        public void spawnEnemies(String definitionId, AbilitySpawnShape shape, boolean consumesSelf) {
            GameWorld world = DefinedEnemyMob.this.gameWorld;
            EnemyDefinition baseDefinition = world.getEnemyCatalog().get(definitionId, DefinedEnemyMob.this.rank);
            EnemyDefinition shapedDefinition = shape.traitOverride()
                    .map(trait -> baseDefinition.withAdditionalTraits(List.of(trait)))
                    .orElse(baseDefinition);
            int health = Math.round(shapedDefinition.baseHealth() * shape.healthMultiplier());
            int price = Math.round(shapedDefinition.price() * shape.bountyMultiplier());
            for (int i = 0; i < shape.members(); i++) {
                double slotPosition = i * shape.delaySpacingSlots();
                SpawnParameters spawnParameters = SpawnParameters.of(slotPosition, shapedDefinition.baseSpeed(),
                        health, price, shape.sizeMultiplier(), 1f, new Vec2(0, 0), DefinedEnemyMob.this.pathIndex);
                DefinedEnemyMob spawned = new DefinedEnemyMob(shapedDefinition, world, spawnParameters, DefinedEnemyMob.this.rank);
                spawned.spawnAtSamePositionAs(DefinedEnemyMob.this);
                spawned.recordAbilitySpawn(this.gameTime);
                if (consumesSelf) {
                    world.enemies().replace(DefinedEnemyMob.this, spawned);
                } else {
                    world.enemies().add(spawned);
                }
            }
        }
    }
}
