package td.ui;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.WorldFixtures;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Row;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class PanelEnemyTest {

    private final GameWorld world = WorldFixtures.newWorld();

    private InfoSheet hoverSheet(String id, Rank rank) {
        EnemyDefinition definition = EnemyCatalog.builtIn().ranked(id).definitionFor(rank);
        EnemyMob mob = PanelEnemy.previewMob(definition, rank, this.world);

        return mob.accept(new EnemyInfoText());
    }

    @Test
    void aPreviewedEnemyShowsTheHealthAndBountyItWillSpawnWith() {
        EnemyDefinition definition = EnemyCatalog.builtIn().ranked("s").definitionFor(Rank.SOLDIER);

        InfoSheet sheet = this.hoverSheet("s", Rank.SOLDIER);

        assertThat(definition.baseHealth()).isPositive();
        assertThat(sheet.lines()).contains(new SheetLine.HealthBar(definition.baseHealth(), definition.baseHealth(),
                "$" + definition.price(), false));
    }

    @Test
    void aPreviewedEnemyThatSpeedsUpWhenHurtShowsItsUnhurtSpeedAndHowFastItCanGet() {
        InfoSheet sheet = this.hoverSheet("t", Rank.ELITE);

        assertThat(sheet.lines().stream().filter(Row.class::isInstance).map(Row.class::cast)
                .filter(row -> row.label().equals("Speed")).map(Row::value))
                .singleElement().asString().doesNotContain("NaN");
        assertThat(sheet.lines()).contains(Row.trait(Palette.TRAIT_MARKER_HURT_SPEED, "Hurt speed", "up to x1.8"));
    }
}
