package td.enemy;

import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.EffectTemplate;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.Vec2;

import java.util.List;
import java.util.Optional;

/**
 * The one concrete {@link EnemyMob}: behaviour comes entirely from its {@link EnemyDefinition}'s
 * traits and abilities.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class DefinedEnemyMob extends AbstractEnemyMob {

    private final EnemyDefinition definition;
    private final List<AbilityState> abilityStates;
    // Folded into every speed recomputation; applied once at construction, the first hit
    // would wipe it.
    private final float shapeSpeedMultiplier;
    private float bodyScale;
    private double facingRadians;
    private int ticksSinceSpawn;
    // Counts from spawn, so a fresh mob does not trivially satisfy an idle threshold.
    private int ticksSinceLastHit;

    public DefinedEnemyMob(EnemyDefinition definition, GameWorld gameWorld, SpawnParameters spawnParameters, Rank rank) {
        super(gameWorld, definition.mobType(), definition.baseSpeed() * spawnParameters.speedMultiplier(),
                withDividedHealth(spawnParameters, definition.healthDivisor()), rank);
        this.definition = definition;
        this.shapeSpeedMultiplier = spawnParameters.speedMultiplier();
        this.abilityStates = definition.abilities().stream().map(a -> AbilityState.forTrigger(a.trigger())).toList();
        this.bodyScale = bodyScaleFor(definition.archetype(), gameWorld.getBoard().scale(), rank) * spawnParameters.sizeMultiplier();
    }

    /** Divides after the spawn shape's multiplier, which the wave already applied to the health. */
    private static SpawnParameters withDividedHealth(SpawnParameters spawnParameters, float healthDivisor) {
        return new SpawnParameters(spawnParameters.delayTicks(), Math.round(spawnParameters.health() / healthDivisor),
                spawnParameters.price(), spawnParameters.sizeMultiplier(), spawnParameters.speedMultiplier(),
                spawnParameters.localOffset(), spawnParameters.pathIndex());
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

    public double getFacingRadians() {
        return switch (this.definition.movement()) {
            case FixedMovement ignored -> 0.0;
            case PulseMovement ignored -> 0.0;
            case PathDirectionalMovement ignored -> this.getPathFacingRadians();
            case RotorMovement ignored -> this.facingRadians;
        };
    }

    /** Places this freshly built mob where {@code other} is on the path. */
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
            // Gated on deathTick capture, not on death: a tower can kill this mob after its own doTick
            // already ran this tick.
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
        // A frozen mob cannot cast.
        if (!this.isIncapacitated()) {
            this.evaluateAbilities(gameTime);
        }
    }

    /** Unable to cast, currently only while frozen. */
    private boolean isIncapacitated() {
        return this.activeEffectKinds().contains(EffectKind.FREEZE);
    }

    /** Rejects the effect if any trait blocks its kind. */
    @Override
    public void applyEffect(Effect effect) {
        for (Trait trait : this.definition.traits()) {
            if (trait.blocksEffect(effect.kind())) {
                return;
            }
        }
        super.applyEffect(effect);
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
                        health, price, shape.sizeMultiplier(), 1f, new Vec2(0, 0), DefinedEnemyMob.this.getPathIndex());
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
