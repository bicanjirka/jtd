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
 * Describes one frame of the game board: cell highlights, enemies, and towers,
 * as an AWT-free {@link RenderFrame}. The one place that owns the per-object
 * builder dispatch, so a backend never needs to know how any domain type is
 * described. Background image blitting and the actual pixel drawing are the
 * backend's job (e.g. {@link Java2DFrameRenderer}), not this class's - this
 * class has no {@code java.awt} import at all.
 * <p>
 * It takes the {@link GameWorld} rather than a handful of narrower slices. Drawing the board
 * means drawing all of it - cells, enemies, towers, projectiles, the board geometry and every
 * path - so naming six collaborators at the call site would say less than naming the one thing
 * that is "everything on the board". What it deliberately does <em>not</em> take is
 * {@code GameEngine}: a renderer has no business next to input handling and level loading.
 * <p>
 * The level is read fresh on every frame through {@link GameWorld#level()}, not captured at
 * construction, because it is replaced wholesale when a level loads - and read <em>once</em>
 * per frame, so a frame cannot mix the board geometry of one level with the cells of another.
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

        TowerSpriteFrameBuilder spriteFrameBuilder = new TowerSpriteFrameBuilder(interpolationAlpha, animationSeconds);
        TowerEffectFrameBuilder effectFrameBuilder = new TowerEffectFrameBuilder(gameTime, interpolationAlpha, animationSeconds);
        for (Tower tower : this.world.towers().all()) {
            tower.accept(spriteFrameBuilder);
            tower.accept(effectFrameBuilder);
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
