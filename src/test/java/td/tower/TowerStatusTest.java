package td.tower;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TowerStatusTest {

    @Test
    void aOneShotStatusSaysWhatItDoesAndThatTheShotSpendsIt() {
        TowerStatus status = TowerStatus.nextShots(TowerTerm.DEADEYE_ROUND, 1, TowerModifiers.critMultiplier(1.5f));

        assertThat(status.describe().plain()).endsWith("; spent by the next shot").contains("150%");
    }

    @Test
    void aTimedStatusSaysHowLongItLasts() {
        TowerStatus status = TowerStatus.timed(TowerTerm.HASTE, TowerModifiers.attackSpeed(1f), 100);

        assertThat(status.describe().plain()).contains("100%").endsWith("; for 5 s");
    }
}
