package td.tower;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class TowerFactoryTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void everyTypeBuildsATowerOfThatType() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            assertThat(TowerFactory.createTower(type, this.context, 0, 0).getType()).as(type.name()).isEqualTo(type);
        }
    }

    @Test
    void enumPriceMatchesEachTowersStaticPrice() {
        assertThat(TowerFactory.Type.SNIPER.price).isEqualTo(SniperTower.PRICE);
        assertThat(TowerFactory.Type.SPLASH.price).isEqualTo(SplashTower.PRICE);
        assertThat(TowerFactory.Type.SONAR.price).isEqualTo(SonarTower.PRICE);
        assertThat(TowerFactory.Type.PULSE.price).isEqualTo(PulseTower.PRICE);
        assertThat(TowerFactory.Type.AURA.price).isEqualTo(AuraTower.PRICE);
        assertThat(TowerFactory.Type.MORTAR.price).isEqualTo(MortarTower.PRICE);
        assertThat(TowerFactory.Type.SEEKER.price).isEqualTo(SeekerTower.PRICE);
        assertThat(TowerFactory.Type.CINDER.price).isEqualTo(CinderTower.PRICE);
    }
}
