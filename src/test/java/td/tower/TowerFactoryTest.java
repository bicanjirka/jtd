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
}
