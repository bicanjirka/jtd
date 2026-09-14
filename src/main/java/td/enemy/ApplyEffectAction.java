package td.enemy;

import td.effect.EffectTemplate;

/** Applies {@code template} to {@code target} - the Warden's periodic self-shield and health-threshold ally-shield abilities. */
public record ApplyEffectAction(EffectTemplate template, EffectTarget target) implements AbilityAction {
}
