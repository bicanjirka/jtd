package td.tower;

import org.junit.jupiter.api.Test;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class TowerFactoryTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void createsASniperTowerForTypeSniper() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.SNIPER, context, 0, 0);

        assertThat(t).isInstanceOf(SniperTower.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.SNIPER);
        assertThat(t.getRange()).isEqualTo(SniperTower.RANGE);
    }

    @Test
    void createsASplashTowerForTypeSplash() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.SPLASH, context, 0, 0);

        assertThat(t).isInstanceOf(SplashTower.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.SPLASH);
    }

    @Test
    void createsASonarTowerForTypeSonar() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.SONAR, context, 0, 0);

        assertThat(t).isInstanceOf(SonarTower.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.SONAR);
    }

    @Test
    void createsAPulseTowerForTypePulse() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.PULSE, context, 0, 0);

        assertThat(t).isInstanceOf(PulseTower.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.PULSE);
    }

    @Test
    void createsAnAuraTowerForTypeAura() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.AURA, context, 0, 0);

        assertThat(t).isInstanceOf(AuraTower.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.AURA);
    }

    @Test
    void createsAMortarTowerForTypeMortar() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.MORTAR, context, 0, 0);

        assertThat(t).isInstanceOf(MortarTower.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.MORTAR);
    }

    @Test
    void createsASeekerTowerForTypeSeeker() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.SEEKER, context, 0, 0);

        assertThat(t).isInstanceOf(SeekerTower.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.SEEKER);
    }

    @Test
    void createsACinderTowerForTypeCinder() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.CINDER, context, 0, 0);

        assertThat(t).isInstanceOf(CinderTower.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.CINDER);
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
