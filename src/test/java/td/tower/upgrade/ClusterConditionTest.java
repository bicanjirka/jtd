package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class ClusterConditionTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    private Tower towerAt(int cellX, int cellY) {
        Tower t = TowerFactory.createTower(TowerFactory.Type.first, this.context, cellX, cellY);
        this.context.towers().add(t);
        return t;
    }

    @Test
    void notSatisfiedWithNoNeighboursAndDoesNotCountItself() {
        Tower subject = this.towerAt(5, 5);

        assertThat(new ClusterCondition(1).isSatisfied(subject, this.context)).isFalse();
    }

    @Test
    void satisfiedByATowerInAnyOfTheEightSurroundingCells() {
        Tower subject = this.towerAt(5, 5);
        this.towerAt(6, 6);

        assertThat(new ClusterCondition(1).isSatisfied(subject, this.context)).isTrue();
    }

    @Test
    void doesNotCountATowerTwoCellsAway() {
        Tower subject = this.towerAt(5, 5);
        this.towerAt(7, 5);

        assertThat(new ClusterCondition(1).isSatisfied(subject, this.context)).isFalse();
    }

    @Test
    void requiresTheThresholdCountOfNeighbours() {
        Tower subject = this.towerAt(5, 5);
        this.towerAt(4, 4);
        this.towerAt(6, 6);

        assertThat(new ClusterCondition(2).isSatisfied(subject, this.context)).isTrue();
        assertThat(new ClusterCondition(3).isSatisfied(subject, this.context)).isFalse();
    }
}
