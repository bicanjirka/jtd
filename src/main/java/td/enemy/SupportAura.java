package td.enemy;

import td.effect.EffectKind;

/**
 * What an {@link EnemyDefinition} projects onto nearby allies, and how far - see
 * {@link EnemyDefinition#supportAura()}. Purely descriptive: it names the effect kind and
 * radius for a UI to draw a ring at, not a live, applied effect.
 */
public record SupportAura(EffectKind kind, float radius) {
}
