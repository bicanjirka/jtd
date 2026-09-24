package td.enemy;

import td.effect.EffectKind;

/** The effect kind and radius a definition projects onto allies, for drawing a ring. */
public record SupportAura(EffectKind kind, float radius) {
}
