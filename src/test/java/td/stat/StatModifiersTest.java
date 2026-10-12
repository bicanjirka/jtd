package td.stat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatModifiersTest {

    @Test
    void plusMergesStatByStat() {
        StatModifiers<EnemyStat> a = StatModifiers.of(EnemyStat.SPIRIT, StatModifier.flat(10f));
        StatModifiers<EnemyStat> b = StatModifiers.of(EnemyStat.ARMOR, StatModifier.percent(0.5f))
                .and(EnemyStat.SPIRIT, StatModifier.times(2f));

        StatModifiers<EnemyStat> combined = a.plus(b);

        assertThat(combined.stats()).containsExactly(EnemyStat.ARMOR, EnemyStat.SPIRIT);
        assertThat(combined.get(EnemyStat.SPIRIT)).isEqualTo(StatModifier.flat(10f).plus(StatModifier.times(2f)));
        assertThat(combined.get(EnemyStat.ARMOR)).isEqualTo(StatModifier.percent(0.5f));
    }

    @Test
    void noneIsTheIdentityOnBothSides() {
        StatModifiers<EnemyStat> bundle = StatModifiers.of(EnemyStat.ARMOR, StatModifier.flat(3f));

        assertThat(bundle.plus(StatModifiers.none())).isEqualTo(bundle);
        assertThat(StatModifiers.<EnemyStat>none().plus(bundle)).isEqualTo(bundle);
        assertThat(StatModifiers.<EnemyStat>none().isNone()).isTrue();
    }
}
