package td;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TowerDefenseTest {

    @Test
    void theWelcomeTextKeepsOnlyTheKeysNoButtonShows() {
        assertThat(TowerDefense.welcomeText()).contains("esc - cancel placing").contains("ctrl+shift+d - dev panel");
    }
}
