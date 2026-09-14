package td.enemy;

/** Who an {@link ApplyEffectAction} affects when it fires. */
public sealed interface EffectTarget permits SelfTarget, RadiusTarget {
}
