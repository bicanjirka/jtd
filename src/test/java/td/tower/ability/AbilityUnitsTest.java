package td.tower.ability;

import org.junit.jupiter.api.Test;
import td.damage.DamageUnits;

import static org.assertj.core.api.Assertions.assertThat;

class AbilityUnitsTest {

    @Test
    void smallHitsAddUpToAWholeUnitWithNothingRoundedAway() {
        int credited = DamageUnits.ofPoints(14f);
        AbilityUnits earned = AbilityUnits.none();

        for (int hit = 0; hit < 50; hit++) {
            earned = earned.plus(AbilityUnits.forDamage(credited, 700f));
        }

        assertThat(earned).isEqualTo(AbilityUnits.whole(1));
    }

    @Test
    void takingMoreThanIsThereLeavesNone() {
        AbilityUnits half = AbilityUnits.of(0.5f);

        assertThat(half.minus(AbilityUnits.whole(1))).isEqualTo(AbilityUnits.none());
    }
}
