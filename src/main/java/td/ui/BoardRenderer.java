package td.ui;

import td.GameEngine;
import td.board.BoardGeometry;
import td.cell.Cell;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.projectile.Projectile;
import td.projectile.ProjectileRegistry;
import td.tower.Tower;
import td.ui.render.CellDraw;
import td.ui.render.PathMarkerDraw;
import td.ui.render.RenderFrame;
import td.wave.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Describes one frame of the game board: cell highlights, enemies, and towers,
 * as an AWT-free {@link RenderFrame}. The one place that owns the per-object
 * builder dispatch, so a backend never needs to know how any domain type is
 * described. Background image blitting and the actual pixel drawing are the
 * backend's job (e.g. {@link Java2DFrameRenderer}), not this class's - this
 * class has no {@code java.awt} import at all.
 * <p>
 * {@code board}/{@code path} are suppliers rather than fixed values because both are
 * replaced wholesale when a level loads, after this renderer is constructed - a fixed
 * field captured at construction would render a stale board forever.
 */
public final class BoardRenderer {

    private final GameEngine engine;
    private final EnemyRegistry enemies;
    private final ProjectileRegistry projectiles;
    private final Supplier<BoardGeometry> board;
    private final Supplier<Path> path;

    public BoardRenderer(GameEngine engine, EnemyRegistry enemies, ProjectileRegistry projectiles,
                          Supplier<BoardGeometry> board, Supplier<Path> path) {
        this.engine = engine;
        this.enemies = enemies;
        this.projectiles = projectiles;
        this.board = board;
        this.path = path;
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
        for (EnemyMob enemy : this.enemies.getEnemies()) {
            enemy.accept(enemyFrameBuilder);
        }

        TowerSpriteFrameBuilder spriteFrameBuilder = new TowerSpriteFrameBuilder(interpolationAlpha, animationSeconds);
        TowerEffectFrameBuilder effectFrameBuilder = new TowerEffectFrameBuilder(gameTime, interpolationAlpha, animationSeconds);
        for (Tower tower : this.engine.getTowers()) {
            tower.accept(spriteFrameBuilder);
            tower.accept(effectFrameBuilder);
        }

        ProjectileFrameBuilder projectileFrameBuilder = new ProjectileFrameBuilder(interpolationAlpha);
        for (Projectile projectile : this.projectiles.getProjectiles()) {
            projectile.accept(projectileFrameBuilder);
        }

        BoardGeometry board = this.board.get();
        List<PathMarkerDraw> pathMarkers = PathMarkerFrameBuilder.build(this.path.get(), board.scale(), animationSeconds);

        return new RenderFrame(board.scale(), board.maxX(), board.maxY(),
                cells, enemyFrameBuilder.build(), enemyFrameBuilder.buildMarkers(),
                spriteFrameBuilder.build(), spriteFrameBuilder.buildHeads(),
                effectFrameBuilder.build(), projectileFrameBuilder.build(), pathMarkers);
    }
}
