package td.enemy;

import td.damage.Damage;
import td.effect.EffectTemplate;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.List;
import java.util.Optional;

/**
 * The single concrete {@link EnemyMob} implementation for every data-driven enemy - behavior
 * comes entirely from its {@link EnemyDefinition}'s {@link Trait}s and {@link Ability}s, not
 * from which Java class was instantiated.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
// body scale, facing and the two tick counters, all advanced by doTick
public final class DefinedEnemyMob extends AbstractEnemyMob {

    private final EnemyDefinition definition;
    private final List<AbilityState> abilityStates;
    // The spawn's full speed multiplier - its SpawnShape's own factor (1 for a normal spawn,
    // 0.5 for a boss) already composed with its path's and wave's speedMultiplier by
    // td.wave.Wave, so a fast path or a called-out fast round needs no separate mechanism here.
    // Folded into every doDamage() speed recomputation alongside the traits' own speedFactor,
    // rather than applied once at construction, which the first hit would silently wipe.
    private final float shapeSpeedMultiplier;
    private float bodyScale;
    private double facingRadians;
    private int ticksSinceSpawn;
    // Starts at 0, not "infinite" - a TimeSinceLastHitTrigger's idle window counts from spawn,
    // the same as from an actual hit, so a fresh spawn doesn't trivially satisfy any threshold
    // immediately.
    private int ticksSinceLastHit;

    public DefinedEnemyMob(EnemyDefinition definition, GameWorld gameWorld, SpawnParameters spawnParameters, Rank rank) {
        // The wave's base health is this definition's to scale: a tougher archetype divides it
        // down. Done in the super call rather than a second init step, so bodyScale below is
        // the only thing left to compute and nothing observes a half-built mob.
        super(gameWorld, definition.mobType(), definition.baseSpeed() * spawnParameters.speedMultiplier(),
                withDividedHealth(spawnParameters, definition.healthDivisor()), rank);
        this.definition = definition;
        this.shapeSpeedMultiplier = spawnParameters.speedMultiplier();
        this.abilityStates = definition.abilities().stream().map(a -> AbilityState.forTrigger(a.trigger())).toList();
        this.bodyScale = bodyScaleFor(definition.archetype(), gameWorld.getBoard().scale(), rank) * spawnParameters.sizeMultiplier();
    }

    /**
     * A shape's health multiplier composes before {@code healthDivisor}, not after: the caller
     * (a {@code SpawnShape}, via {@link td.wave.Wave}) has already scaled the wave's base health
     * by the time it builds {@code spawnParameters}, and this only applies the definition's own
     * toughness division on top - exactly what happened here before shapes existed.
     */
    private static SpawnParameters withDividedHealth(SpawnParameters spawnParameters, float healthDivisor) {
        return new SpawnParameters(spawnParameters.delayTicks(), Math.round(spawnParameters.health() / healthDivisor),
                spawnParameters.price(), spawnParameters.sizeMultiplier(), spawnParameters.speedMultiplier(),
                spawnParameters.localOffset(), spawnParameters.pathIndex());
    }

    private static float bodyScaleFor(BodyArchetype archetype, int scale, Rank rank) {
        return switch (archetype) {
            case CIRCLE -> scale / 6f;
            // The ladder is a closed, five-tier enum, so this needs no cap the way a
            // once-unbounded numeric level did - BOSS's ordinal (4) is the largest this ever sees.
            case SQUARE, TRIANGLE, GHOST -> scale / (float) (7 - rank.ordinal());
            // Fixed, not rank-scaled like SQUARE/TRIANGLE/GHOST above: the Warden is meant to
            // read as visibly the biggest thing on the board regardless of which wave slot
            // spawned it, not merely tied with an ordinary Square at its own size cap.
            case WARDEN -> scale / 1.5f;
            case WARDEN_EGG -> scale / 3f;
            case MENDER -> scale / (float) (7 - rank.ordinal());
        };
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

    public double getFacingRadians() {
        return switch (this.definition.movement()) {
            case FixedMovement ignored -> 0.0;
            case PulseMovement ignored -> 0.0;
            case PathDirectionalMovement ignored -> this.getPathFacingRadians();
            case RotorMovement ignored -> this.facingRadians;
        };
    }

    /**
     * Places this (freshly constructed) mob at the same point along the path another mob was
     * at, rather than the path's start every mob otherwise spawns at - what lets an ability's
     * spawn (the Warden's egg, a reinforcement) appear where the spawning mob actually was.
     */
    void spawnAtSamePositionAs(AbstractEnemyMob other) {
        this.jumpToDistance(other.getDistanceIntoLap());
    }

    @Override
    public void doTick(int gameTime) {
        super.doTick(gameTime);
        if (this.isInactive()) {
            return;
        }
        if (this.isDead()) {
            // Evaluate abilities exactly once - the tick deathTick is captured (checked via
            // ticksSinceDeath, not a "was already dead" flag: a tower can kill this mob during
            // its own doTick phase, after this mob's own doTick already ran for that tick - see
            // AbstractEnemyMob's death-timing invariant - so "dead" and "deathTick captured"
            // are not necessarily the same tick, and only the latter must gate firing once.
            if (this.ticksSinceDeath(gameTime) == 0) {
                this.evaluateAbilities(gameTime);
            }
            return;
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
        this.evaluateAbilities(gameTime);
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

    @Override
    protected Damage absorb(Damage incoming) {
        Damage result = incoming;
        TraitContext context = this.traitContext();
        for (Trait trait : this.definition.traits()) {
            result = trait.onHit(result, context);
        }
        return result;
    }

    @Override
    public Damage doDamage(Damage damage) {
        this.ticksSinceLastHit = 0;
        Damage landed = super.doDamage(damage);
        TraitContext context = this.traitContext();
        float factor = 1f;
        for (Trait trait : this.definition.traits()) {
            factor *= trait.speedFactor(context);
        }
        this.setSpeed(this.definition.baseSpeed() * this.shapeSpeedMultiplier * factor);
        return landed;
    }

    @Override
    public boolean validTarget() {
        if (!super.validTarget()) {
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

    private TraitContext traitContext() {
        return new TraitContext(this.getHealthFraction());
    }

    public <R> R accept(EnemyMobVisitor<R> visitor) {
        return visitor.visitDefined(this);
    }

    /**
     * The rank/health/bounty line makes up for {@code td.ui.PathWaveRow} no longer showing a
     * wave-wide hp/price/rank label - those numbers are per-enemy now, so hovering the mob that
     * actually has them is where they belong.
     */
    public String getInfoString() {
        return this.definition.displayName() + "\n\n" + this.definition.description() + "\n\nRank: "
                + titleCase(this.rank) + "   Health: " + this.definition.baseHealth() + "   Bounty: "
                + this.definition.price();
    }

    private static String titleCase(Rank rank) {
        String name = rank.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    /**
     * The runtime {@link AbilityContext} a live mob's own abilities execute against - resolves
     * {@link Ability}/{@link AbilityAction} data against this mob's actual {@link GameWorld},
     * position and {@link EnemyDefinition}, which {@link AbilityEvaluator} itself never needs
     * to see directly.
     */
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

        /**
         * Ability-produced effects deal no direct damage in v1 (shield/invisibility/heal only) -
         * this sink is never actually invoked.
         */
        private void creditNoOne(Damage damage) {
        }

        @Override
        public void spawnEnemies(String definitionId, int count, boolean consumesSelf) {
            GameWorld world = DefinedEnemyMob.this.gameWorld;
            EnemyDefinition spawnedDefinition = world.getEnemyCatalog().get(definitionId, DefinedEnemyMob.this.rank);
            for (int i = 0; i < count; i++) {
                SpawnParameters spawnParameters = SpawnParameters.atSlot(0, spawnedDefinition.baseSpeed(),
                        spawnedDefinition.baseHealth(), spawnedDefinition.price(), DefinedEnemyMob.this.getPathIndex());
                DefinedEnemyMob spawned = new DefinedEnemyMob(spawnedDefinition, world, spawnParameters, DefinedEnemyMob.this.rank);
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
