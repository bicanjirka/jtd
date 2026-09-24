package td.ui;

import td.board.BoardGeometry;
import td.enemy.EnemyMob;
import td.projectile.Projectile;
import td.tower.Tower;
import td.ui.render.CellDraw;
import td.ui.render.PathMarkerDraw;
import td.ui.render.RenderFrame;
import td.util.GameWorld;
import td.util.LoadedLevel;
import td.util.PathRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes one frame of the board as an AWT-free {@link RenderFrame}, owning the dispatch to each
 * per-object builder. Drawing pixels is the backend's job.
 * <p>
 * Takes the whole {@link GameWorld}, since it draws everything on the board, but never the engine.
 * Reads {@link GameWorld#level()} once per frame, so a frame cannot mix two levels.
 */
public final class BoardRenderer {

    private final GameWorld world;

    public BoardRenderer(GameWorld world) {
        this.world = world;
    }

    public RenderFrame buildFrame(int gameTime, double interpolationAlpha, double animationSeconds) {
        LoadedLevel level = this.world.level();

        List<CellDraw> cells = new ArrayList<>();
        level.cells().forEach(cell -> {
            CellDraw draw = CellFrameBuilder.build(cell);
            if (draw != null) {
                cells.add(draw);
            }
        });

        EnemyFrameBuilder enemyFrameBuilder = new EnemyFrameBuilder(gameTime, interpolationAlpha);
        for (EnemyMob enemy : this.world.enemies().getEnemies()) {
            enemy.accept(enemyFrameBuilder);
        }

        TowerSpriteFrameBuilder spriteFrameBuilder = new TowerSpriteFrameBuilder(this.world, interpolationAlpha, animationSeconds);
        TowerEffectFrameBuilder effectFrameBuilder = new TowerEffectFrameBuilder(gameTime, interpolationAlpha, animationSeconds);
        for (Tower tower : this.world.towers().all()) {
            tower.accept(spriteFrameBuilder);
            tower.accept(effectFrameBuilder);
            effectFrameBuilder.addStatus(tower, level.board().scale());
        }

        ProjectileFrameBuilder projectileFrameBuilder = new ProjectileFrameBuilder(interpolationAlpha);
        for (Projectile projectile : this.world.projectiles().getProjectiles()) {
            projectile.accept(projectileFrameBuilder);
        }

        BoardGeometry board = level.board();
        List<PathMarkerDraw> pathMarkers = new ArrayList<>();
        for (PathRuntime pathRuntime : level.paths()) {
            pathMarkers.addAll(PathMarkerFrameBuilder.build(pathRuntime.path(), pathRuntime.color(), board.scale(), animationSeconds));
        }

        return new RenderFrame(board.scale(), board.maxX(), board.maxY(),
                cells, enemyFrameBuilder.build(), enemyFrameBuilder.buildMarkers(), enemyFrameBuilder.buildCritSparks(),
                spriteFrameBuilder.build(), spriteFrameBuilder.buildHeads(),
                effectFrameBuilder.build(), projectileFrameBuilder.build(), pathMarkers,
                enemyFrameBuilder.buildOverlays());
    }
}
