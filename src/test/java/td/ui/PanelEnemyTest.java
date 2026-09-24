package td.ui;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class PanelEnemyTest {

    private final GameWorld world = WorldFixtures.newWorld();

    private String hoverText(String id, Rank rank) {
        EnemyDefinition definition = EnemyCatalog.builtIn().ranked(id).definitionFor(rank);
        EnemyMob mob = PanelEnemy.previewMob(definition, rank, this.world);

        return mob.accept(new EnemyInfoText());
    }

    @Test
    void aPreviewedEnemyShowsTheHealthAndBountyItWillSpawnWith() {
        EnemyDefinition definition = EnemyCatalog.builtIn().ranked("s").definitionFor(Rank.SOLDIER);

        String text = this.hoverText("s", Rank.SOLDIER);

        assertThat(definition.baseHealth()).isPositive();
        assertThat(text).contains("Health: " + definition.baseHealth()).contains("Bounty: " + definition.price());
    }

    @Test
    void aPreviewedEnemyThatSpeedsUpWhenHurtShowsItsUnhurtSpeed() {
        String text = this.hoverText("t", Rank.ELITE);

        assertThat(text).contains("Speed ").doesNotContain("NaN");
    }
}
