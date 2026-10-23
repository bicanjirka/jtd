package td.ui;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.enemy.TraitTerm;
import td.fixtures.WorldFixtures;
import td.ui.render.InfoSheet;
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
        EnemyDefinition definition = EnemyCatalog.builtIn().ranked("armored").definitionFor(Rank.SOLDIER);

        InfoSheet sheet = this.hoverSheet("armored", Rank.SOLDIER);

        assertThat(definition.baseHealth()).isPositive();
        assertThat(sheet.lines()).contains(new SheetLine.HealthBar(definition.baseHealth(), definition.baseHealth(),
                "$" + definition.price(), false));
    }

    @Test
    void aPreviewedEnemyThatSpeedsUpWhenHurtShowsItsUnhurtSpeedAndHowFastItCanGet() {
        InfoSheet sheet = this.hoverSheet("fast", Rank.ELITE);

        assertThat(sheet.lines().stream().filter(Row.class::isInstance).map(Row.class::cast)
                .filter(row -> row.label().equals("Speed")).map(Row::value))
                .singleElement().asString().doesNotContain("NaN");
        assertThat(sheet.lines()).anySatisfy(line -> assertThat(line).isInstanceOfSatisfying(SheetLine.IconRow.class,
                row -> assertThat(row.icons()).anySatisfy(icon -> {
                    assertThat(icon.face().pictogram()).isEqualTo(TraitTerm.ADRENALINE.key());
                    assertThat(icon.tooltip().lines().getLast()).isInstanceOfSatisfying(SheetLine.Sentence.class,
                            sentence -> assertThat(sentence.text().plain()).contains("1.8"));
                })));
    }
}
