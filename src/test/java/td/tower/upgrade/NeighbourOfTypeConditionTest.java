package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.AuraTower;
import td.tower.SniperTower;
import td.tower.TowerFactory;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class NeighbourOfTypeConditionTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void satisfiedByAnotherTowerOfTheTypeInASurroundingCell() {
        AuraTower aura = new AuraTower(this.context, 5, 5);
        this.context.towers().add(aura);
        this.context.towers().add(new AuraTower(this.context, 6, 4));

        assertThat(new NeighbourOfTypeCondition(TowerFactory.Type.AURA).isSatisfied(aura, this.context)).isTrue();
    }

    @Test
    void notSatisfiedByItselfAnotherTypeOrATowerTwoCellsAway() {
        AuraTower aura = new AuraTower(this.context, 5, 5);
        this.context.towers().add(aura);
        this.context.towers().add(new SniperTower(this.context, 5, 6));
        this.context.towers().add(new AuraTower(this.context, 7, 5));

        assertThat(new NeighbourOfTypeCondition(TowerFactory.Type.AURA).isSatisfied(aura, this.context)).isFalse();
    }

    @Test
    void describesItselfByTheTypeItNeeds() {
        assertThat(new NeighbourOfTypeCondition(TowerFactory.Type.AURA).describe())
                .startsWith("next to another ");
    }
}
