package td.tower;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TowerExperienceTest {

    @Test
    void xpAddsUpAndARestoreReplacesIt() {
        TowerExperience experience = new TowerExperience();

        experience.earn(30);
        experience.earn(30);
        int earned = experience.xp();
        experience.restore(5);

        assertThat(earned).isEqualTo(60);
        assertThat(experience.xp()).isEqualTo(5);
    }
}
