package td.enemy;

import td.effect.EffectKind;

/**
 * The most recent ability effect a mob cast, recorded on the caster. {@code radius} is {@code 0}
 * for a self cast. Needed because refreshing an effect on allies that already have it causes no
 * gain or loss transition, so the cast would otherwise be invisible.
 */
public record AbilityCast(EffectKind kind, float radius, int tick) {
}
