package td.tower;

import org.junit.jupiter.api.Test;
import td.stat.DisruptionPenalty;
import td.stat.StatModifiers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TowerStatsTest {

    private static final TowerBaseStats BASE = new TowerBaseStats(1, 2f, 20);

    private static TowerStats of(StatModifiers<TowerStat> own, DisruptionPenalty disruption) {
        return TowerStats.of(BASE, own, StatModifiers.none(), disruption, 10);
    }

    @Test
    void attackSpeedBonusesAdd() {
        TowerStats stats = of(TowerModifiers.attackSpeed(0.3f).plus(TowerModifiers.attackSpeed(0.25f)),
                DisruptionPenalty.none());

        assertThat(stats.attackRate()).isCloseTo(1.55, within(1e-6));
    }

    @Test
    void attackSpeedStaysBetweenAQuarterAndTenTimesTheBaseRate() {
        assertThat(of(TowerModifiers.attackSpeed(-2f), DisruptionPenalty.none()).attackRate())
                .isCloseTo(0.25, within(1e-6));
        assertThat(of(TowerModifiers.attackSpeed(20f), DisruptionPenalty.none()).attackRate())
                .isCloseTo(10.0, within(1e-6));
    }

    @Test
    void disruptionLowersRangeByCellsAndAttackSpeedByWhatTheAurasLeave() {
        TowerStats stats = of(TowerModifiers.disruptionTaken(0.5f), new DisruptionPenalty(0.4f, 0.6f));

        assertThat(stats.range()).isCloseTo(2f - 0.3f, within(1e-5f));
        assertThat(stats.attackRate()).isCloseTo(0.8, within(1e-6));
    }

    @Test
    void disruptionTakenMultipliesAcrossAuras() {
        TowerStats stats = of(TowerModifiers.disruptionTaken(0.5f).plus(TowerModifiers.disruptionTaken(0.5f)),
                DisruptionPenalty.none());

        assertThat(stats.value(TowerStat.DISRUPTION_TAKEN)).isEqualTo(0.25f);
    }

    @Test
    void twoWitheringSourcesKeepTheShorterInterval() {
        TowerStats stats = of(TowerModifiers.witherEvery(200).plus(TowerModifiers.witherEvery(100)),
                DisruptionPenalty.none());

        assertThat(stats.witherTicks()).isEqualTo(100);
    }

    @Test
    void rangeNeverDropsBelowHalfACell() {
        TowerStats stats = of(TowerModifiers.range(-5f), new DisruptionPenalty(0f, 3f));

        assertThat(stats.range()).isEqualTo(0.5f);
    }

    @Test
    void anAuraBuffAndADisruptionPenaltyAddUp() {
        TowerStats stats = TowerStats.of(BASE, StatModifiers.none(), TowerModifiers.rangePercent(0.5f),
                new DisruptionPenalty(0.25f, 0.2f), 10);

        assertThat(stats.range()).isCloseTo((2f - 0.2f) * 1.5f, within(1e-5f));
        assertThat(stats.attackRate()).isCloseTo(0.75, within(1e-6));
    }

    @Test
    void reachForXpKeepsTheBuffedRangeAndIgnoresTheDisruption() {
        TowerStats stats = TowerStats.of(BASE, StatModifiers.none(), TowerModifiers.rangePercent(0.5f),
                new DisruptionPenalty(0.25f, 0.2f), 10);

        assertThat(stats.reachReal()).isCloseTo(2f * 1.5f * 10, within(1e-4f));
    }
}
