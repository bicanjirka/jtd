package td.ui;

import td.GameEngine;
import td.cell.Cell;
import td.enemy.EnemyMob;
import td.tower.Tower;
import td.ui.render.CellDraw;
import td.ui.render.PathMarkerDraw;
import td.ui.render.RenderFrame;
import td.util.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes one frame of the game board: cell highlights, enemies, and towers,
 * as an AWT-free {@link RenderFrame}. The one place that owns the per-object
 * builder dispatch, so a backend never needs to know how any domain type is
 * described. Background image blitting and the actual pixel drawing are the
 * backend's job (e.g. {@link Java2DFrameRenderer}), not this class's - this
 * class has no {@code java.awt} import at all.
 */
public final class BoardRenderer {

    private final GameEngine engine;
    private final Context context;

    public BoardRenderer(GameEngine engine, Context context) {
        this.engine = engine;
        this.context = context;
    }

    public RenderFrame buildFrame(int gameTime, double interpolationAlpha, double animationSeconds) {
        List<CellDraw> cells = new ArrayList<>();
        Cell[][] cellGrid = this.engine.getCellGrid();
        if (cellGrid != null) {
            for (Cell[] column : cellGrid) {
                for (Cell cell : column) {
                    CellDraw draw = CellFrameBuilder.build(cell);
                    if (draw != null) {
                        cells.add(draw);
                    }
                }
            }
        }

        EnemyFrameBuilder enemyFrameBuilder = new EnemyFrameBuilder(gameTime, interpolationAlpha);
        for (EnemyMob enemy : this.context.getEnemies()) {
            enemy.accept(enemyFrameBuilder);
        }

        TowerSpriteFrameBuilder spriteFrameBuilder = new TowerSpriteFrameBuilder();
        TowerEffectFrameBuilder effectFrameBuilder = new TowerEffectFrameBuilder();
        for (Tower tower : this.engine.getTowers()) {
            tower.accept(spriteFrameBuilder);
            tower.accept(effectFrameBuilder);
        }

        List<PathMarkerDraw> pathMarkers = PathMarkerFrameBuilder.build(this.context.getPath(), this.context.scale, animationSeconds);

        return new RenderFrame(this.context.scale, this.context.maxX, this.context.maxY,
                cells, enemyFrameBuilder.build(), spriteFrameBuilder.build(), effectFrameBuilder.build(), pathMarkers);
    }
}
