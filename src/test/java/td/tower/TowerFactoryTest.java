package td.tower;

import org.junit.jupiter.api.Test;
import td.util.Context;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class TowerFactoryTest {

    private final Context context = new Context(new RecordingGameHost());

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
    void createsTowerUpgradeForTypeUpgrade() {
        Tower t = TowerFactory.createTower(TowerFactory.type.upgrade, context, 0, 0);

        assertThat(t).isInstanceOf(TowerUpgrade.class);
        assertThat(t.getType()).isEqualTo(TowerFactory.type.upgrade);
    }

    @Test
    void enumPriceMatchesEachTowersStaticPrice() {
        assertThat(TowerFactory.type.first.price).isEqualTo(TowerOne.price);
        assertThat(TowerFactory.type.second.price).isEqualTo(TowerTwo.price);
        assertThat(TowerFactory.type.third.price).isEqualTo(TowerThree.price);
        assertThat(TowerFactory.type.fourth.price).isEqualTo(TowerFour.price);
        assertThat(TowerFactory.type.upgrade.price).isEqualTo(TowerUpgrade.price);
    }
}
