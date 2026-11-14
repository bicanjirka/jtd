package td.ui;

import org.junit.jupiter.api.Test;
import td.GameEngine;
import td.cell.CellHighlight;
import td.damage.Damage;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.EnemyFixtures;
import td.fixtures.LevelFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.projectile.CannonballProjectile;
import td.tower.CinderTower;
import td.tower.SniperTower;
import td.tower.TowerFactory;
import td.tower.TowerTerm;
import td.tower.upgrade.UpgradeNode;
import td.ui.render.CellGridDraw;
import td.ui.render.ConeDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.ProjectileDraw;
import td.ui.render.RenderFrame;
import td.ui.render.SheetLine;
import td.ui.render.TowerLook;
import td.ui.render.TowerSheet;
import td.ui.render.TowerSpriteDraw;
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
    void aSelectedTowerYieldsASpriteDrawWithItsBodyPaletteAndSelectionFlag() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        SniperTower tower = new SniperTower(context, 1, 1);
        tower.setSelected(true);
        context.towers().add(tower);

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites()).hasSize(1);
        TowerSpriteDraw sprite = frame.towerSprites().getFirst();
        assertThat(sprite.look()).isEqualTo(TowerLook.SNIPER);
        assertThat(sprite.selected()).isTrue();
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
    void theCellGridIsDrawnOnlyWhileShown() {
        GameEngine engine = newEngine();
        BoardRenderer renderer = rendererFor(engine.getGameWorld());

        RenderFrame hidden = renderer.buildFrame(0, 0.0, 0.0);
        renderer.setCellGridShown(true);
        RenderFrame shown = renderer.buildFrame(0, 0.0, 0.0);

        assertThat(hidden.cellGrid()).isEmpty();
        assertThat(shown.cellGrid()).isPresent();
    }

    @Test
    void theCellGridShadesThePathsCellsButNotATowersCell() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.economy().startEconomy(1000, 5);
        BoardRenderer renderer = rendererFor(context);
        renderer.setCellGridShown(true);
        int pathCells = renderer.buildFrame(0, 0.0, 0.0).cellGrid().orElseThrow().blockedCells().size();
        engine.startPlacing(TowerFactory.Type.SNIPER, 0f);
        engine.mouseClicked(BoardFixtures.cellCenter(1), BoardFixtures.cellCenter(1));

        List<CellGridDraw.BlockedCell> blocked = renderer.buildFrame(0, 0.0, 0.0).cellGrid().orElseThrow().blockedCells();

        assertThat(engine.cells().at(1, 1).hasTower()).isTrue();
        assertThat(pathCells).isPositive();
        assertThat(blocked).hasSize(pathCells);
    }

    @Test
    void aTowerStandsOnTheFoundationOfTheHighestTierItHasOpen() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        SniperTower tower = new SniperTower(context, 1, 1);
        context.towers().add(tower);
        UpgradeNode upper = tower.upgradeTree().nodes().stream().filter(node -> node.tier() > 0).findFirst().orElseThrow();
        UpgradePaths.buy(tower, context, upper.displayName());
        int open = tower.upgradeTree().highestOpenTier(tower, context);

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(open).isPositive();
        assertThat(frame.towerSprites().getFirst().tier()).isEqualTo(open);
    }

    @Test
    void aTowerWhoseTierRoseAMomentAgoIsDrawnRisingFromItsOldTierUntilTheRiseEnds() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        SniperTower tower = new SniperTower(context, 1, 1);
        context.towers().add(tower);
        tower.beginTick(100);
        UpgradeNode upper = tower.upgradeTree().nodes().stream().filter(node -> node.tier() > 0).findFirst().orElseThrow();
        UpgradePaths.buy(tower, context, upper.displayName());
        tower.beginTick(101);

        TowerSpriteDraw rising = rendererFor(context).buildFrame(102, 0.0, 0.0).towerSprites().getFirst();
        TowerSpriteDraw settled = rendererFor(context).buildFrame(200, 0.0, 0.0).towerSprites().getFirst();

        assertThat(rising.risenFrom()).isZero();
        assertThat(rising.riseProgress()).isBetween(0f, 1f);
        assertThat(settled.riseProgress()).isEqualTo(-1f);
    }

    @Test
    void towersAreDescribedBackToFrontWhateverOrderTheyWereBuiltIn() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        SniperTower south = new SniperTower(context, 1, 3);
        SniperTower north = new SniperTower(context, 2, 1);
        SniperTower middle = new SniperTower(context, 1, 2);
        context.towers().add(south);
        context.towers().add(north);
        context.towers().add(middle);

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites()).extracting(TowerSpriteDraw::boardY)
                .containsExactly(north.getBoardY(), middle.getBoardY(), south.getBoardY());
    }

    @Test
    void anUnselectedTowerYieldsASpriteDrawWithSelectedFalse() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new SniperTower(context, 1, 1));

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites().getFirst().selected()).isFalse();
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
    void aDeadEnemyYieldsAFadeDrawAndNoBodyDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.doDamage(Damage.physical(5000));
        enemy.doTick(1); // captures deathTick, matching how a mob really ticks

        context.enemies().setEnemies(new EnemyMob[]{enemy});

        RenderFrame frame = rendererFor(context).buildFrame(1, 0.0, 0.0);

        assertThat(frame.enemies()).hasSize(1).first().isInstanceOf(EnemyFadeDraw.class);
    }

    @Test
    void noHighlightProducesNoCellDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.cells()).isEmpty();
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

    @Test
    void aSelectedCellProducesExactlyOneCellDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();

        RenderFrame frame = rendererFor(context, CellHighlight.select(0, 0)).buildFrame(0, 0.0, 0.0);

        assertThat(frame.cells()).hasSize(1);
        assertThat(frame.cells().getFirst().highlight()).isEqualTo(CellHighlight.Kind.SELECT);
    }

    @Test
    void aHighlightOffAReplacedSmallerGridProducesNoCellDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        int outside = engine.cells().width();

        RenderFrame frame = rendererFor(context, CellHighlight.select(outside, 0)).buildFrame(0, 0.0, 0.0);

        assertThat(frame.cells()).isEmpty();
    }

    @Test
    void aCinderTowerWithNoInFlightWaveYieldsNoConeDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        CinderTower tower = new CinderTower(context, 1, 1);
        WorldFixtures.passAWave(context);
        context.towers().add(tower);

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerEffects()).isEmpty();
    }

    @Test
    void aCinderTowerThatJustFiredAWaveYieldsAConeDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        CinderTower tower = new CinderTower(context, 1, 1);
        WorldFixtures.passAWave(context);
        context.towers().add(tower);
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        context.enemies().setEnemies(new EnemyMob[]{enemy});

        int firedAt = 1;
        while (tower.getInFlightWaves().isEmpty() && firedAt < 60) {
            tower.doTick(firedAt++); // turns toward the enemy, then fires a wave
        }

        RenderFrame frame = rendererFor(context).buildFrame(firedAt - 1, 0.0, 0.0);

        assertThat(frame.towerEffects()).hasSize(1);
        assertThat(frame.towerEffects().getFirst()).isInstanceOf(ConeDraw.class);
    }

    @Test
    void anInFlightProjectileYieldsAProjectileDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.projectiles().add(new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        }));

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.projectiles()).hasSize(1);
        assertThat(frame.projectiles().getFirst()).isInstanceOf(ProjectileDraw.class);
    }

    @Test
    void aSelectedEnemysSheetCarriesWhatTowersRememberOfIt() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 100, 5);
        context.enemies().add(enemy);
        int scale = context.getBoard().scale();
        CinderTower cinder = new CinderTower(context, (int) (enemy.getX() / scale) + 1, (int) (enemy.getY() / scale));
        context.towers().add(cinder);
        context.economy().startEconomy(1000, 5);
        cinder.buyUpgrade(UpgradePaths.named(cinder, "Attune"));
        for (int t = 1; t <= 400 && cinder.marksOn(enemy).isEmpty(); t++) {
            cinder.doTick(t);
        }
        engine.requestEnemySelectionAt((int) enemy.getX(), (int) enemy.getY());

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0, engine.inspectSelectedEnemy());

        assertThat(cinder.marksOn(enemy)).isNotEmpty();
        assertThat(frame.enemyInspection().orElseThrow().lines()).anySatisfy(line -> assertThat(line)
                .isInstanceOfSatisfying(SheetLine.IconRow.class, row -> {
                    assertThat(row.label()).isEqualTo("Debuffs");
                    assertThat(row.icons()).extracting(icon -> icon.face().pictogram())
                            .contains(TowerTerm.STOKE.key());
                }));
    }
}
