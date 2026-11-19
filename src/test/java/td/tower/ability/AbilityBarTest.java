package td.tower.ability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbilityBarTest {

    @Test
    void aWaveStartsWithOneUnitInTheBar() {
        AbilityBar bar = AbilityBar.atWaveStart(3);

        assertThat(bar.fill()).isEqualTo(AbilityUnits.whole(1));
        assertThat(bar.isReady()).isFalse();
    }

    @Test
    void everyBarHoldsTheSameWhateverItsCostAndLosesWhatComesPastIt() {
        AbilityBar cheap = AbilityBar.atWaveStart(1);
        AbilityBar dear = AbilityBar.atWaveStart(3);

        AbilityBar full = cheap.earning(AbilityUnits.whole(9));

        assertThat(full.capacity()).isEqualTo(dear.capacity());
        assertThat(full.fill()).isEqualTo(AbilityUnits.whole(full.capacity()));
    }

    @Test
    void aCastPaysItsCostAndKeepsTheRest() {
        AbilityBar bar = AbilityBar.atWaveStart(2).earning(AbilityUnits.of(1.5f));

        AbilityBar after = bar.afterCast();

        assertThat(bar.isReady()).isTrue();
        assertThat(after.fill()).isEqualTo(AbilityUnits.of(0.5f));
    }

    @Test
    void aBarWithoutItsCostInCannotCast() {
        AbilityBar bar = AbilityBar.atWaveStart(2);

        assertThatThrownBy(bar::afterCast).isInstanceOf(IllegalStateException.class);
    }
}
