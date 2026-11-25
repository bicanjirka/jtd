package td.ui;

import org.junit.jupiter.api.Test;
import td.GameEngine;
import td.cell.CellHighlight;
import td.fixtures.LevelFixtures;
import td.tower.SniperTower;
import td.ui.render.RenderFrame;
import td.ui.render.SheetLine;
import td.ui.render.TowerSheet;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BoardRendererTest {

    private static GameEngine newEngine() {
        GameEngine engine = new GameEngine(new RecordingGameHost());
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        return engine;
    }

    private static BoardRenderer rendererFor(GameWorld context) {
        return rendererFor(context, CellHighlight.none());
    }

    private static BoardRenderer rendererFor(GameWorld context, CellHighlight highlight) {
        return new BoardRenderer(context, () -> highlight);
    }

    @Test
    void aSelectedTowersSheetIsBuiltWithTheFrame() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        SniperTower tower = new SniperTower(context, 1, 2);
        tower.setSelected(true);
        context.towers().add(tower);

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        TowerSheet sheet = frame.towerSheet().orElseThrow();
        assertThat(sheet.x()).isEqualTo(tower.getX());
        assertThat(sheet.y()).isEqualTo(tower.getY());
        assertThat(sheet.sheet().lines().getFirst()).isInstanceOf(SheetLine.TowerHeader.class);
    }

    @Test
    void noTowerSheetWithoutASelection() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new SniperTower(context, 1, 1));

        assertThat(rendererFor(context).buildFrame(0, 0.0, 0.0).towerSheet()).isEmpty();
    }

    @Test
    void aTowerThatCannotPayForAnythingShowsNoUpgradeArrow() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.economy().startEconomy(0, 5);
        context.towers().add(new SniperTower(context, 1, 1));

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites().getFirst().upgradeReady()).isFalse();
    }

    @Test
    void aFundedTowerWithAnUpgradeOnOfferShowsTheUpgradeArrow() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.economy().startEconomy(1000, 5);
        context.towers().add(new SniperTower(context, 1, 1));

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites().getFirst().upgradeReady()).isTrue();
    }

    @Test
    void aPlacementHoverIsAffordableOnlyWhileTheCreditsCoverItsPrice() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        int credits = context.economy().getCredits();

        boolean shortOfCash = rendererFor(context, CellHighlight.place(0, 0, 1f, credits + 1))
                .buildFrame(0, 0.0, 0.0).cells().getFirst().affordable();
        boolean covered = rendererFor(context, CellHighlight.place(0, 0, 1f, credits))
                .buildFrame(0, 0.0, 0.0).cells().getFirst().affordable();

        assertThat(shortOfCash).isFalse();
        assertThat(covered).isTrue();
    }

}
