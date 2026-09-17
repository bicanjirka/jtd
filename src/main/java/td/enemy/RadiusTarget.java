package td.enemy;

/**
 * The effect applies to every other valid-target enemy within {@code radius} pixels - the Warden's "call to arms" ability.
 */
public record RadiusTarget(float radius) implements EffectTarget {
}
