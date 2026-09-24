package td.enemy;

/** Every other valid enemy within {@code radius} pixels. */
public record RadiusTarget(float radius) implements EffectTarget {
}
