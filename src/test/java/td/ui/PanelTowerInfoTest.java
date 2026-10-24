package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.term.TermText;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.InfoSheet;
import td.ui.render.SheetLine;
import td.ui.render.TowerSheet;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class PanelTowerInfoTest {

    private static final InfoSheet HOVERED = sheet("Hovered");

    private final GameWorld world = WorldFixtures.newWorld();
    private final FakeTower tower = FakeTower.offering(this.world, 2, 3, UpgradeTree.none());
    private final PanelTowerInfo info = new PanelTowerInfo();

    private static InfoSheet sheet(String text) {
        return new InfoSheet(List.of(SheetLine.Sentence.of(TermText.of(text))));
    }

    @Test
    void aHoveredSheetGivesThePaneBackToTheSelectedTowerOnceTheHoverEnds() {
        this.info.setTower(this.tower);
        this.info.showHoveredSheet(HOVERED);

        this.info.hoverEnded();

        assertThat(this.tower.isSelected()).isTrue();
        assertThat(this.info.shownSheet()).isEqualTo(TowerSheetText.status(this.tower.inspect()));
    }

    @Test
    void theSelectedTowersLiveSheetWaitsWhileAnotherSheetIsHovered() {
        this.info.setTower(this.tower);
        this.info.showHoveredSheet(HOVERED);

        this.info.showTowerSheet(new TowerSheet(this.tower.getX(), this.tower.getY(), sheet("Live")));

        assertThat(this.info.shownSheet()).isEqualTo(HOVERED);
    }

    @Test
    void withNothingSelectedAHoveredSheetStaysOnceTheHoverEnds() {
        this.info.showHoveredSheet(HOVERED);

        this.info.hoverEnded();

        assertThat(this.info.shownSheet()).isEqualTo(HOVERED);
    }
}
