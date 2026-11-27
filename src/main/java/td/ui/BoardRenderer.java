package td.ui;

import td.board.BoardGeometry;
import td.cell.CellHighlight;
import td.enemy.EnemyInspection;
import td.enemy.EnemyMob;
import td.projectile.Projectile;
import td.stat.StatWording;
import td.tower.AuraTower;
import td.tower.MortarTower;
import td.tower.Tower;
import td.tower.TowerInspection;
import td.ui.render.BoardDraw;
import td.ui.render.CannonballDraw;
import td.ui.render.CellDraw;
import td.ui.render.CellGridDraw;
import td.ui.render.InfoSheet;
import td.ui.render.MissileDraw;
import td.ui.render.PathMarkerDraw;
import td.ui.render.ProjectileDraw;
import td.ui.render.RenderFrame;
import td.ui.render.SmokeDraw;
import td.ui.render.TowerGroundDraw;
import td.ui.render.TowerSheet;
import td.ui.render.TowerSpriteDraw;
import td.ui.render.UpgradeArrowDraw;
import td.ui.render.ZoneDetailDraw;
import td.ui.render.ZoneDraw;
import td.ui.render.ZoneGroundPieceDraw;
import td.ui.render.ZonePatchDraw;
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
        CellDraw highlighted = CellFrameBuilder.build(this.highlight.get(), level.cells(), this.world.economy());
        if (highlighted != null) {
            cells.add(highlighted);
        }

        Optional<EnemyMob> selected = inspection.isPresent() ? this.world.enemySelection().selectedAlive() : Optional.empty();
        EnemyFrameBuilder enemyFrameBuilder = new EnemyFrameBuilder(gameTime, interpolationAlpha, selected, animationSeconds);
        for (EnemyMob enemy : this.world.enemies().getEnemies()) {
            enemy.accept(enemyFrameBuilder);
        }

        TowerSpriteFrameBuilder spriteFrameBuilder = new TowerSpriteFrameBuilder(this.world, gameTime, interpolationAlpha,
                animationSeconds);
        TowerEffectFrameBuilder effectFrameBuilder = new TowerEffectFrameBuilder(this.world, gameTime,
                interpolationAlpha, animationSeconds);
        for (Tower tower : this.world.towers().all()) {
            tower.accept(spriteFrameBuilder);
            effectFrameBuilder.add(tower);
        }

        ProjectileFrameBuilder projectileFrameBuilder = new ProjectileFrameBuilder(gameTime, interpolationAlpha,
                level.board().scale(), effectFrameBuilder.nestLifts());
        for (Projectile projectile : this.world.projectiles().getProjectiles()) {
            projectile.accept(projectileFrameBuilder);
        }

        BoardGeometry board = level.board();
        List<PathMarkerDraw> pathMarkers = new ArrayList<>();
        for (PathRuntime pathRuntime : level.paths()) {
            pathMarkers.addAll(PathMarkerFrameBuilder.build(pathRuntime.path(), pathRuntime.color(), board.scale(), animationSeconds));
        }

        Optional<InfoSheet> enemySheet = inspection
                .map(found -> EnemyStatText.live(found, new StatWording(board.scale())));

        Optional<TowerSheet> towerSheet = this.world.towers().all().stream().filter(Tower::isSelected).findFirst()
                .map(tower -> new TowerSheet(tower.getX(), tower.getY(), TowerSheetText.status(liveInspection(tower))));

        List<ZonePatchDraw> zones = ZoneFrameBuilder.build(this.world.zones(), this.world.towers().all(), animationSeconds);
        List<BoardDraw> scene = scene(board.scale(), spriteFrameBuilder.build(), enemyFrameBuilder.placed(), zones,
                effectFrameBuilder.build(), projectileFrameBuilder.build());
        return new RenderFrame(board.scale(), board.maxX(), board.maxY(), cells, zones, pathMarkers, scene,
                enemySheet, towerSheet, this.cellGrid(level), level.ground(), effectFrameBuilder.boardFlash());
    }

    /**
     * Everything over the ground in paint order. Within a height and a foot, what is added first is painted first: a
     * tower's ground before an enemy's ring, an enemy before what a tower does to it, a tower before what moves at it.
     */
    private static List<BoardDraw> scene(int scale, List<TowerSpriteDraw> sprites, List<PlacedDraw> enemies,
            List<ZonePatchDraw> zones, List<PlacedDraw> effects, List<ProjectileDraw> projectiles) {
        List<PlacedDraw> placed = new ArrayList<>(sprites.size() * 3 + enemies.size() + effects.size()
                + projectiles.size());
        for (TowerSpriteDraw sprite : sprites) {
            placed.add(PlacedDraw.of(new TowerGroundDraw(sprite), sprite.centerY()));
        }
        placed.addAll(enemies);
        for (TowerSpriteDraw sprite : sprites) {
            placed.add(PlacedDraw.of(sprite, sprite.centerY()));
            if (sprite.upgradeReady()) {
                placed.add(PlacedDraw.of(new UpgradeArrowDraw(sprite), sprite.centerY()));
            }
        }
        for (ZonePatchDraw patch : zones) {
            for (ZoneDraw zone : patch.discs()) {
                if (ZoneFrameBuilder.stands(patch.palette())) {
                    placed.add(PlacedDraw.of(new ZoneDetailDraw(patch.palette(), zone), zone.centerY()));
                }
                int count = GroundArt.pieces(patch.palette(), zone, scale);
                for (int i = 0; i < count; i++) {
                    ZoneGroundPieceDraw piece = new ZoneGroundPieceDraw(patch.palette(), zone, i, count);
                    placed.add(PlacedDraw.of(piece, (float) GroundArt.pieceAt(piece)[1]));
                }
            }
        }
        placed.addAll(effects);
        for (ProjectileDraw projectile : projectiles) {
            placed.add(PlacedDraw.of(projectile, footOf(projectile)));
        }
        return PlacedDraw.inOrder(placed);
    }

    /** The screen row of the ground under a shot in flight. */
    private static float footOf(ProjectileDraw projectile) {
        return switch (projectile) {
            case CannonballDraw shell -> shell.y();
            case MissileDraw missile -> missile.y();
            case SmokeDraw smoke -> smoke.y();
        };
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

    /** {@code tower} as it stands this frame: what it carries (and what its light carries), how far its nodes count, its bars, its magazine. */
    static TowerInspection liveInspection(Tower tower) {
        TowerInspection found = tower.inspect().withConditions(tower.conditions()).withProgress(tower.progress())
                .withAbilities(tower.abilities()).withCarries(AuraTower.carriedBy(tower));
        return MortarTower.magazineViewOf(tower).map(found::withMagazine).orElse(found);
    }
}
