package td.effect;

import org.junit.jupiter.api.Test;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

import static org.assertj.core.api.Assertions.assertThat;

class EffectRuleTest {

    @Test
    void ofIsARefreshingSelfPacedKindWithNoStatsOrTags() {
        EffectRule rule = EffectRule.of(EffectCategory.DEBUFF);

        assertThat(rule.stacking()).isEqualTo(EffectStacking.REFRESH);
        assertThat(rule.stackCap()).isEqualTo(1);
        assertThat(rule.pacing()).isEqualTo(EffectPacing.OWN);
        assertThat(rule.perStack().isNone()).isTrue();
        assertThat(rule.tags()).isEmpty();
        assertThat(rule.resistedBy()).isEmpty();
    }

    @Test
    void perStackAddsUp() {
        EffectRule rule = EffectRule.of(EffectCategory.DEBUFF).perStack(EnemyStat.ARMOR, -5f).perStack(EnemyStat.ARMOR, -3f)
                .perStack(EnemyStat.MOVE_SPEED, StatModifier.times(0.5f));

        assertThat(rule.perStack()).isEqualTo(StatModifiers.of(EnemyStat.ARMOR, StatModifier.flat(-8f))
                .and(EnemyStat.MOVE_SPEED, StatModifier.times(0.5f)));
    }
}
