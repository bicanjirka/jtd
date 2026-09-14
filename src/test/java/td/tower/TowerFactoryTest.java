package td.tower;

import org.junit.jupiter.api.Test;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class TowerFactoryTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void createsTowerOneForTypeFirst() {
        Tower t = TowerFactory.createTower(TowerFactory.type.first, context, 0, 0);

        assertThat(t).isInstanceOf(TowerOne.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.type.first);
        assertThat(t.getRange()).isEqualTo(TowerOne.range);
    }

    @Test
    void createsTowerTwoForTypeSecond() {
        Tower t = TowerFactory.createTower(TowerFactory.type.second, context, 0, 0);

        assertThat(t).isInstanceOf(TowerTwo.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.type.second);
    }

    @Test
    void createsTowerThreeForTypeThird() {
        Tower t = TowerFactory.createTower(TowerFactory.type.third, context, 0, 0);

        assertThat(t).isInstanceOf(TowerThree.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.type.third);
    }

    @Test
    void createsTowerFourForTypeFourth() {
        Tower t = TowerFactory.createTower(TowerFactory.type.fourth, context, 0, 0);

        assertThat(t).isInstanceOf(TowerFour.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.type.fourth);
    }

    @Test
    void createsTowerAuraForTypeAura() {
        Tower t = TowerFactory.createTower(TowerFactory.type.aura, context, 0, 0);

        assertThat(t).isInstanceOf(TowerAura.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.type.aura);
    }

    @Test
    void createsTowerMortarForTypeMortar() {
        Tower t = TowerFactory.createTower(TowerFactory.type.mortar, context, 0, 0);

        assertThat(t).isInstanceOf(TowerMortar.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.type.mortar);
    }

    @Test
    void createsTowerSeekerForTypeSeeker() {
        Tower t = TowerFactory.createTower(TowerFactory.type.seeker, context, 0, 0);

        assertThat(t).isInstanceOf(TowerSeeker.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.type.seeker);
    }

    @Test
    void createsTowerCinderForTypeCinder() {
        Tower t = TowerFactory.createTower(TowerFactory.type.cinder, context, 0, 0);

        assertThat(t).isInstanceOf(TowerCinder.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.type.cinder);
    }

    @Test
    void enumPriceMatchesEachTowersStaticPrice() {
        assertThat(TowerFactory.type.first.price).isEqualTo(TowerOne.price);
        assertThat(TowerFactory.type.second.price).isEqualTo(TowerTwo.price);
        assertThat(TowerFactory.type.third.price).isEqualTo(TowerThree.price);
        assertThat(TowerFactory.type.fourth.price).isEqualTo(TowerFour.price);
        assertThat(TowerFactory.type.aura.price).isEqualTo(TowerAura.price);
        assertThat(TowerFactory.type.mortar.price).isEqualTo(TowerMortar.price);
        assertThat(TowerFactory.type.seeker.price).isEqualTo(TowerSeeker.price);
        assertThat(TowerFactory.type.cinder.price).isEqualTo(TowerCinder.price);
    }
}
