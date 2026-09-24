package td.enemy;

import td.effect.EffectTemplate;

public record ApplyEffectAction(EffectTemplate template, EffectTarget target) implements AbilityAction {
}
