package td.enemy;

import td.damage.Damage;
import td.util.GameWorld;

/**
 * The single concrete {@link EnemyMob} implementation for every data-driven enemy - behavior
 * comes entirely from its {@link EnemyDefinition}'s {@link Trait}s, not from which Java class
 * was instantiated. {@link EnemyMobEmpty} is the one deliberate exception: a wave-timing spacer
 * that never ticks, is never a valid target, and is never drawn stays its own tiny class rather
 * than being forced through a trait/ability model it has no real use for.
 */
public final class DefinedEnemyMob extends AbstractEnemyMob {

    private final EnemyDefinition definition;
    private float bodyScale;
    private double facingRadians;

    public DefinedEnemyMob(EnemyDefinition definition, GameWorld gameWorld, int delay, int health, int price, int level) {
        super();
        this.definition = definition;
        this.type = definition.mobType();
        this.speed = definition.baseSpeed();
        this.doInit(gameWorld, delay, health, price, level);
    }

    @Override
    protected void doInit(GameWorld gameWorld, int delay, int health, int price, int level) {
        super.doInit(gameWorld, delay, Math.round(health / this.definition.healthDivisor()), price, level);
        this.bodyScale = bodyScaleFor(this.definition.archetype(), this.gameWorld.getBoard().scale(), this.level);
    }

    private static float bodyScaleFor(BodyArchetype archetype, int scale, int level) {
        return switch (archetype) {
            case CIRCLE -> scale / 6f;
            case SQUARE, TRIANGLE, GHOST -> scale / (float) ((level < 6) ? (7 - level) : 2);
        };
    }

    public float getBodyScale() {
        return this.bodyScale;
    }

    public BodyArchetype archetype() {
        return this.definition.archetype();
    }

    public double getFacingRadians() {
        return switch (this.definition.movement()) {
            case FixedMovement ignored -> 0.0;
            case PulseMovement ignored -> 0.0;
            case PathDirectionalMovement ignored -> this.getPathFacingRadians();
            case RotorMovement ignored -> this.facingRadians;
        };
    }

    @Override
    public void doTick(int gameTime) {
        super.doTick(gameTime);
        if (this.isInactive() || this.isDead()) {
            return;
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
        Damage landed = super.doDamage(damage);
        TraitContext context = this.traitContext();
        float factor = 1f;
        for (Trait trait : this.definition.traits()) {
            factor *= trait.speedFactor(context);
        }
        this.speed = this.definition.baseSpeed() * factor;
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
        return new TraitContext(this.level, this.getHealthFraction());
    }

    public <R> R accept(EnemyMobVisitor<R> visitor) {
        return visitor.visitDefined(this);
    }

    public String getInfoString() {
        return this.definition.displayName() + "\n\n" + this.definition.description();
    }
}
