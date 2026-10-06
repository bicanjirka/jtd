package td.tower;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TowerExperienceTest {

    @Test
    void ranksOpenAtFiftyOneHundredFiftyAndThreeHundredXp() {
        assertThat(TowerRank.of(49)).isEqualTo(TowerRank.RECRUIT);
        assertThat(TowerRank.of(50)).isEqualTo(TowerRank.SEASONED);
        assertThat(TowerRank.of(150)).isEqualTo(TowerRank.EXPERT);
        assertThat(TowerRank.of(300)).isEqualTo(TowerRank.HERO);
    }

    @Test
    void xpAddsUpAndOnlyCrossingATierStampsARankUp() {
        TowerExperience experience = new TowerExperience();

        experience.earn(30, 10);
        int beforeRankUp = experience.ticksSinceRankUp(12);
        experience.earn(30, 20);

        assertThat(beforeRankUp).isEqualTo(-1);
        assertThat(experience.xp()).isEqualTo(60);
        assertThat(experience.rank()).isEqualTo(TowerRank.SEASONED);
        assertThat(experience.ticksSinceRankUp(25)).isEqualTo(5);
    }
}
