package td.stat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResolvedStatsTest {

    @Test
    void resolvesLikeTheEnemySheetForTheSameModifiers() {
        BaseStats<EnemyStat> base = BaseStats.defaults(EnemyStat.class).with(EnemyStat.ARMOR, 20f)
                .with(EnemyStat.MOVE_SPEED, 2f);
        StatModifiers<EnemyStat> bundle = StatModifiers.of(EnemyStat.ARMOR, StatModifier.flat(10f))
                .and(EnemyStat.ARMOR, StatModifier.percent(0.5f))
                .and(EnemyStat.MOVE_SPEED, StatModifier.times(0.75f))
                .and(EnemyStat.STEALTH, StatModifier.setTo(1f));

        ResolvedStats<EnemyStat> resolved = ResolvedStats.resolve(base, bundle, EnemyStat.class);
        StatSheet sheet = new StatSheet(base, bundle::applyTo);

        for (EnemyStat stat : EnemyStat.values()) {
            assertThat(resolved.value(stat)).as(stat.name()).isEqualTo(sheet.value(stat));
        }
    }

    @Test
    void aSetValueWinsAndIsClamped() {
        StatModifiers<EnemyStat> bundle = StatModifiers.of(EnemyStat.RESILIENCE, StatModifier.flat(10f))
                .and(EnemyStat.RESILIENCE, StatModifier.setTo(500f));

        ResolvedStats<EnemyStat> resolved = ResolvedStats.resolve(BaseStats.defaults(EnemyStat.class), bundle,
                EnemyStat.class);

        assertThat(resolved.value(EnemyStat.RESILIENCE)).isEqualTo(100f);
    }

    @Test
    void aStatNoModifierTouchesKeepsItsDefault() {
        ResolvedStats<EnemyStat> resolved = ResolvedStats.resolve(BaseStats.defaults(EnemyStat.class),
                StatModifiers.of(EnemyStat.ARMOR, StatModifier.flat(5f)), EnemyStat.class);

        assertThat(resolved.value(EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isEqualTo(1f);
    }
}
