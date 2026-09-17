package td.tower;

import org.junit.jupiter.api.Test;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class TowerFactoryTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void createsTowerOneForTypeFirst() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.first, context, 0, 0);

        assertThat(t).isInstanceOf(TowerOne.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.first);
        assertThat(t.getRange()).isEqualTo(TowerOne.RANGE);
    }

    @Test
    void createsTowerTwoForTypeSecond() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.second, context, 0, 0);

        assertThat(t).isInstanceOf(TowerTwo.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.second);
    }

    @Test
    void createsTowerThreeForTypeThird() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.third, context, 0, 0);

        assertThat(t).isInstanceOf(TowerThree.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.third);
    }

    @Test
    void createsTowerFourForTypeFourth() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.fourth, context, 0, 0);

        assertThat(t).isInstanceOf(TowerFour.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.fourth);
    }

    @Test
    void createsTowerAuraForTypeAura() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.aura, context, 0, 0);

        assertThat(t).isInstanceOf(TowerAura.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.aura);
    }

    @Test
    void createsTowerMortarForTypeMortar() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.mortar, context, 0, 0);

        assertThat(t).isInstanceOf(TowerMortar.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.mortar);
    }

    @Test
    void createsTowerSeekerForTypeSeeker() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.seeker, context, 0, 0);

        assertThat(t).isInstanceOf(TowerSeeker.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.seeker);
    }

    @Test
    void createsTowerCinderForTypeCinder() {
        Tower t = TowerFactory.createTower(TowerFactory.Type.cinder, context, 0, 0);

        assertThat(t).isInstanceOf(TowerCinder.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.Type.cinder);
    }

    @Test
    void enumPriceMatchesEachTowersStaticPrice() {
        assertThat(TowerFactory.Type.first.price).isEqualTo(TowerOne.PRICE);
        assertThat(TowerFactory.Type.second.price).isEqualTo(TowerTwo.PRICE);
        assertThat(TowerFactory.Type.third.price).isEqualTo(TowerThree.PRICE);
        assertThat(TowerFactory.Type.fourth.price).isEqualTo(TowerFour.PRICE);
        assertThat(TowerFactory.Type.aura.price).isEqualTo(TowerAura.PRICE);
        assertThat(TowerFactory.Type.mortar.price).isEqualTo(TowerMortar.PRICE);
        assertThat(TowerFactory.Type.seeker.price).isEqualTo(TowerSeeker.PRICE);
        assertThat(TowerFactory.Type.cinder.price).isEqualTo(TowerCinder.PRICE);
    }
}
