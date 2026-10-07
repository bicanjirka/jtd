package td;

import org.junit.jupiter.api.Test;
import td.tower.TowerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class TowerDefenseTest {

    @Test
    void theWelcomeTextListsEveryTowerUnderItsKeyAndShopName() {
        String text = TowerDefense.welcomeText();

        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            assertThat(text).contains(type.placementKey + " - build " + type.displayName().toLowerCase());
        }
        assertThat(text).contains("q - build sniper").contains("w - build burst").contains("t - build beacon");
    }

    @Test
    void theWelcomeTextKeepsTheGeneralShortcuts() {
        assertThat(TowerDefense.welcomeText()).contains("esc - cancel placing").contains("ctrl+shift+d - dev panel");
    }
}
