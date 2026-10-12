package td.stat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseStatsTest {

    @Test
    void onlyAuthoredStatsAreSet() {
        BaseStats<EnemyStat> base = BaseStats.defaults(EnemyStat.class).with(EnemyStat.ARMOR, 0f);

        assertThat(base.isSet(EnemyStat.ARMOR)).isTrue();
        assertThat(base.isSet(EnemyStat.SPIRIT)).isFalse();
        assertThat(base.value(EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isEqualTo(1f);
    }
}
