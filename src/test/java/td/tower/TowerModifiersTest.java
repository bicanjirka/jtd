package td.tower;

import org.junit.jupiter.api.Test;
import td.stat.StatModifier;

import static org.assertj.core.api.Assertions.assertThat;

class TowerModifiersTest {

    @Test
    void percentFactoriesArePercentAndTheRestAreFlat() {
        assertThat(TowerModifiers.damagePercent(0.3f).get(TowerStat.DAMAGE)).isEqualTo(StatModifier.percent(0.3f));
        assertThat(TowerModifiers.rangePercent(0.1f).get(TowerStat.RANGE)).isEqualTo(StatModifier.percent(0.1f));
        assertThat(TowerModifiers.range(0.6f).get(TowerStat.RANGE)).isEqualTo(StatModifier.flat(0.6f));
        assertThat(TowerModifiers.attackSpeed(0.25f).get(TowerStat.ATTACK_SPEED)).isEqualTo(StatModifier.flat(0.25f));
        assertThat(TowerModifiers.bounty(0.5f).get(TowerStat.BOUNTY)).isEqualTo(StatModifier.flat(0.5f));
    }

    @Test
    void witherEverySetsTheInterval() {
        assertThat(TowerModifiers.witherEvery(200).get(TowerStat.WITHER_INTERVAL)).isEqualTo(StatModifier.setTo(200f));
    }
}
