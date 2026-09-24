package td.stat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StatSheetTest {

    @Test
    void anUnmodifiedStatResolvesToItsBase() {
        StatSheet sheet = new StatSheet(BaseStats.defaults().with(EnemyStat.ARMOR, 25f), accumulator -> {
        });

        assertThat(sheet.value(EnemyStat.ARMOR)).isEqualTo(25f);
        assertThat(sheet.value(EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isEqualTo(1f);
    }

    @Test
    void aResolvedValueIsClampedToItsStatsRange() {
        StatSheet sheet = new StatSheet(BaseStats.defaults(), accumulator -> {
            accumulator.multiply(EnemyStat.PHYSICAL_DAMAGE_TAKEN, 0f);
            accumulator.add(EnemyStat.RESILIENCE, StatModifier.flat(500f));
        });

        assertThat(sheet.value(EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isEqualTo(0.1f);
        assertThat(sheet.value(EnemyStat.RESILIENCE)).isEqualTo(100f);
    }

    @Test
    void spiritScalesRestorativeContributions() {
        StatSheet sheet = new StatSheet(BaseStats.defaults().with(EnemyStat.SPIRIT, 50f), accumulator -> {
            accumulator.restoreFlat(EnemyStat.REGENERATION, 10f);
            accumulator.restoreReduction(EnemyStat.MAGIC_DAMAGE_TAKEN, 0.4f);
        });

        assertThat(sheet.value(EnemyStat.REGENERATION)).isCloseTo(15f, within(0.001f));
        assertThat(sheet.value(EnemyStat.MAGIC_DAMAGE_TAKEN)).isCloseTo(0.4f, within(0.001f));
    }

    @Test
    void valuesStayCachedUntilInvalidated() {
        float[] armor = {10f};
        StatSheet sheet = new StatSheet(BaseStats.defaults(),
                accumulator -> accumulator.add(EnemyStat.ARMOR, StatModifier.flat(armor[0])));
        assertThat(sheet.value(EnemyStat.ARMOR)).isEqualTo(10f);

        armor[0] = 30f;

        assertThat(sheet.value(EnemyStat.ARMOR)).isEqualTo(10f);
        sheet.invalidate();
        assertThat(sheet.value(EnemyStat.ARMOR)).isEqualTo(30f);
    }
}
