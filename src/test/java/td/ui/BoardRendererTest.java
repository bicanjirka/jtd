package td.ui;

import org.junit.jupiter.api.Test;
import td.GameEngine;
import td.cell.Cell;
import td.damage.Damage;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.tower.TowerOne;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.RenderFrame;
import td.ui.render.TowerSpriteDraw;
import td.util.Context;
import td.util.RecordingGameHost;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts on BoardRenderer.buildFrame()'s output directly - no Graphics2D, no
 * window - proving the frame model is genuinely headless-testable, which the
 * old paint()-based renderer never was.
 */
class BoardRendererTest {

    private static GameEngine newEngine() {
        GameEngine engine = new GameEngine(new RecordingGameHost());
        engine.loadLevel(5, 5, new int[]{0, 4}, new int[]{2, 2}, List.of(), 100);
        return engine;
    }

    @Test
    void aSelectedTowerYieldsASpriteDrawWithItsImageKeyAndSelectionFlag() {
        GameEngine engine = newEngine();
        Context context = engine.getContext();
        TowerOne tower = new TowerOne(context, 1, 1);
        tower.setSelected(true);
        context.addTower(tower);

        RenderFrame frame = new BoardRenderer(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites()).hasSize(1);
        TowerSpriteDraw sprite = frame.towerSprites().get(0);
        assertThat(sprite.imageKey()).isEqualTo("tower1");
        assertThat(sprite.selected()).isTrue();
    }

    @Test
    void anUnselectedTowerYieldsASpriteDrawWithSelectedFalse() {
        GameEngine engine = newEngine();
        Context context = engine.getContext();
        context.addTower(new TowerOne(context, 1, 1));

        RenderFrame frame = new BoardRenderer(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites().get(0).selected()).isFalse();
    }

    @Test
    void aDeadEnemyYieldsAFadeDrawAndNoBodyDraw() {
        GameEngine engine = newEngine();
        Context context = engine.getContext();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        enemy.doDamage(Damage.of(5000));
        enemy.doTick(1); // captures deathTick, matching how AbstractEnemyMob really ticks

        context.setEnemies(new EnemyMob[]{enemy});

        RenderFrame frame = new BoardRenderer(engine, context).buildFrame(1, 0.0, 0.0);

        assertThat(frame.enemies()).hasSize(1).first().isInstanceOf(EnemyFadeDraw.class);
    }

    @Test
    void anEmptyEnemyYieldsNoDraw() {
        GameEngine engine = newEngine();
        Context context = engine.getContext();
        context.setEnemies(new EnemyMob[]{EnemyFactory.getEnemy("e", context, 0, 50, 3, 1)});

        RenderFrame frame = new BoardRenderer(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.enemies()).isEmpty();
    }

    @Test
    void onlyHighlightedCellsProduceADraw() {
        GameEngine engine = newEngine();
        Context context = engine.getContext();
        // a freshly loaded level has highlightType.none everywhere

        RenderFrame frame = new BoardRenderer(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.cells()).isEmpty();
    }

    @Test
    void aSelectedCellProducesExactlyOneCellDraw() {
        GameEngine engine = newEngine();
        Context context = engine.getContext();
        engine.getCellGrid()[0][0].setHighlight(Cell.highlightType.select);

        RenderFrame frame = new BoardRenderer(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.cells()).hasSize(1);
        assertThat(frame.cells().get(0).highlight()).isEqualTo(Cell.highlightType.select);
    }
}
