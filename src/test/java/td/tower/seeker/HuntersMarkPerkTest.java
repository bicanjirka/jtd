package td.tower.seeker;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeEnemyMob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HuntersMarkPerkTest {

    @Test
    void eachConsecutiveHitOnTheSameEnemyLandsAFifthHarderUpToTwiceAsHard() {
        HuntersMarkPerk perk = new HuntersMarkPerk();
        FakeEnemyMob target = FakeEnemyMob.at(0, 0);

        float[] factors = new float[8];
        for (int i = 0; i < factors.length; i++) {
            factors[i] = perk.damageFactor(target);
        }

        assertThat(factors).containsExactly(1f, 1.2f, 1.4f, 1.6f, 1.8f, 2f, 2f, 2f);
    }

    @Test
    void aHitOnAnotherEnemyStartsTheStreakOver() {
        HuntersMarkPerk perk = new HuntersMarkPerk();
        FakeEnemyMob first = FakeEnemyMob.at(0, 0);
        FakeEnemyMob second = FakeEnemyMob.at(10, 0);
        perk.damageFactor(first);
        perk.damageFactor(first);

        float other = perk.damageFactor(second);
        float back = perk.damageFactor(first);

        assertThat(other).isEqualTo(1f);
        assertThat(back).isCloseTo(1f, within(0.001f));
    }
}
