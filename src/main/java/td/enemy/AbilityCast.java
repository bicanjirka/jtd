package td.enemy;

import td.effect.EffectKind;

/**
 * The most recent ability-applied effect a mob cast, and when - recorded on the <em>caster</em>
 * regardless of who the effect actually landed on. {@code radius} is {@code 0} for a
 * {@link SelfTarget} cast. This exists because a periodic re-application onto allies already
 * under its effect (the Ghost Elite's shroud, refreshed every interval) triggers no gain/loss
 * transition on any of them - without a moment recorded on the caster itself, such a re-cast
 * would be entirely invisible. See {@code td.ui.EnemyFrameBuilder}.
 */
public record AbilityCast(EffectKind kind, float radius, int tick) {
}
