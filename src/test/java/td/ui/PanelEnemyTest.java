package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.enemy.TraitTerm;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.ui.render.InfoSheet;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Row;
import td.util.GameWorld;

import java.awt.Point;
import java.awt.Rectangle;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class PanelEnemyTest {

    private final GameWorld world = WorldFixtures.newWorld();

    private InfoSheet hoverSheet(String id, Rank rank) {
        EnemyDefinition definition = EnemyCatalog.builtIn().ranked(id).definitionFor(rank);
        EnemyMob mob = PanelEnemy.previewMob(definition, rank, this.world);

        return mob.accept(new EnemyInfoText());
    }

    /** As a wave row fills it: the enemies, then a layout of the given width. */
    private static PanelEnemy stripOf(int width, EnemyDefinition... definitions) {
        PanelEnemy strip = new PanelEnemy();
        for (EnemyDefinition definition : definitions) {
            strip.addEnemy(definition, 3, Rank.SOLDIER);
        }
        strip.setSize(width, PanelEnemy.HEIGHT);
        strip.recalculateSize();
        return strip;
    }

    @Test
    void theSpotUnderThePointerIsItsEnemysSlotAndOpensThatEnemysPreview() {
        EnemyDefinition second = EnemyFixtures.simpleDefinition("second");
        PanelEnemy strip = stripOf(200, EnemyFixtures.simpleDefinition("first"), second);

        TooltipSpot spot = strip.spotAt(new Point(40, 20)).orElseThrow();

        assertThat(spot.bounds()).isEqualTo(new Rectangle(32, 0, 32, PanelEnemy.HEIGHT));
        assertThat(spot.tooltip().sheet())
                .isEqualTo(PanelEnemy.previewMob(second, Rank.SOLDIER, this.world).accept(new EnemyInfoText()));
    }

    @Test
    void pastTheLastEnemyThereIsNoSpot() {
        PanelEnemy strip = stripOf(200, EnemyFixtures.simpleDefinition("first"), EnemyFixtures.simpleDefinition("second"));

        assertThat(strip.spotAt(new Point(70, 20))).isEmpty();
    }

    @Test
    void enemiesThatDoNotFitShareTheWidthInEqualSlots() {
        PanelEnemy strip = stripOf(60, EnemyFixtures.simpleDefinition("first"), EnemyFixtures.simpleDefinition("second"),
                EnemyFixtures.simpleDefinition("third"));

        TooltipSpot spot = strip.spotAt(new Point(45, 20)).orElseThrow();

        assertThat(spot.bounds()).isEqualTo(new Rectangle(40, 0, 20, PanelEnemy.HEIGHT));
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
