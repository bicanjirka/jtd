package td.ui;

import td.board.BoardGeometry;
import td.cell.CellHighlight;
import td.enemy.EnemyInspection;
import td.enemy.EnemyMark;
import td.enemy.EnemyMob;
import td.projectile.Projectile;
import td.tower.Tower;
import td.ui.render.CellDraw;
import td.ui.render.CellGridDraw;
import td.ui.render.InfoSheet;
import td.ui.render.PathMarkerDraw;
import td.ui.render.RenderFrame;
import td.ui.render.TowerSheet;
import td.util.GameWorld;
import td.util.LoadedLevel;
import td.util.PathRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Describes one frame of the board as an AWT-free {@link RenderFrame}, owning the dispatch to each
 * per-object builder. Drawing pixels is the backend's job.
 * <p>
 * Takes the whole {@link GameWorld}, since it draws everything on the board, but never the engine.
 * Reads {@link GameWorld#level()} once per frame, so a frame cannot mix two levels.
 */
public final class BoardRenderer {

    private final GameWorld world;
    private final Supplier<CellHighlight> highlight;

    private volatile boolean cellGridShown;

    public BoardRenderer(GameWorld world, Supplier<CellHighlight> highlight) {
        this.world = world;
        this.highlight = highlight;
    }

    /** Set from the EDT; the next frame built carries the grid, or drops it. */
    public void setCellGridShown(boolean shown) {
        this.cellGridShown = shown;
    }

    public RenderFrame buildFrame(int gameTime, double interpolationAlpha, double animationSeconds) {
        return this.buildFrame(gameTime, interpolationAlpha, animationSeconds, Optional.empty());
    }

    /** A frame that also carries {@code inspection}'s text and rings the selected enemy. */
    public RenderFrame buildFrame(int gameTime, double interpolationAlpha, double animationSeconds,
            Optional<EnemyInspection> inspection) {
        LoadedLevel level = this.world.level();

        List<CellDraw> cells = new ArrayList<>();
        CellDraw highlighted = CellFrameBuilder.build(this.highlight.get(), level.cells(), this.world.economy().getCredits());
        if (highlighted != null) {
            cells.add(highlighted);
        }

        Optional<EnemyMob> selected = inspection.isPresent() ? this.world.enemySelection().selectedAlive() : Optional.empty();
        EnemyFrameBuilder enemyFrameBuilder = new EnemyFrameBuilder(gameTime, interpolationAlpha, selected);
        for (EnemyMob enemy : this.world.enemies().getEnemies()) {
            enemy.accept(enemyFrameBuilder);
        }

        TowerSpriteFrameBuilder spriteFrameBuilder = new TowerSpriteFrameBuilder(this.world, gameTime, interpolationAlpha, animationSeconds);
        TowerEffectFrameBuilder effectFrameBuilder = new TowerEffectFrameBuilder(level.board().scale(), gameTime,
                interpolationAlpha, animationSeconds);
        for (Tower tower : this.world.towers().all()) {
            tower.accept(spriteFrameBuilder);
            tower.accept(effectFrameBuilder);
            effectFrameBuilder.addStatus(tower);
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

        Optional<InfoSheet> enemySheet = inspection
                .map(found -> selected.map(mob -> found.withMarks(this.marksOn(mob))).orElse(found))
                .map(EnemyStatText::live);

        Optional<TowerSheet> towerSheet = this.world.towers().all().stream().filter(Tower::isSelected).findFirst()
                .map(tower -> new TowerSheet(tower.getX(), tower.getY(), TowerSheetText.status(tower.inspect()
                        .withConditions(tower.conditions()).withProgress(tower.progress()))));

        return new RenderFrame(board.scale(), board.maxX(), board.maxY(),
                cells, ZoneFrameBuilder.build(this.world.zones(), animationSeconds), enemyFrameBuilder.build(), enemyFrameBuilder.buildMarkers(), enemyFrameBuilder.buildCritSparks(),
                spriteFrameBuilder.build(), spriteFrameBuilder.buildHeads(),
                effectFrameBuilder.build(), projectileFrameBuilder.build(), pathMarkers,
                enemyFrameBuilder.buildOverlays(), enemySheet, towerSheet, this.cellGrid(level));
    }

    /** What every tower remembers about {@code mob}, for its pane. */
    private List<EnemyMark> marksOn(EnemyMob mob) {
        List<EnemyMark> marks = new ArrayList<>();
        this.world.towers().all().forEach(tower -> marks.addAll(tower.marksOn(mob)));
        return marks;
    }

    /** Blocked means unbuildable with no tower on it: a path covers the cell. */
    private Optional<CellGridDraw> cellGrid(LoadedLevel level) {
        if (!this.cellGridShown) {
            return Optional.empty();
        }
        List<CellGridDraw.BlockedCell> blocked = new ArrayList<>();
        level.cells().forEach(cell -> {
            if (!cell.buildable() && !cell.hasTower()) {
                blocked.add(new CellGridDraw.BlockedCell(cell.getX(), cell.getY()));
            }
        });
        return Optional.of(new CellGridDraw(blocked));
    }
}
