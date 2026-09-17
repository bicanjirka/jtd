package td.ui;

import org.junit.jupiter.api.Test;
import td.GameEngine;
import td.cell.Cell;
import td.damage.Damage;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.level.LevelDefinition;
import td.projectile.CannonballProjectile;
import td.tower.TowerCinder;
import td.tower.TowerOne;
import td.ui.render.ConeDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.Palette;
import td.ui.render.ProjectileDraw;
import td.ui.render.RenderFrame;
import td.ui.render.TowerSpriteDraw;
import td.util.GameWorld;
import td.util.RecordingGameHost;
import td.wave.Point;

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
        engine.loadLevel(LevelDefinition.unsmoothed("Test Level", "", 5, 5,
                List.of(new Point(0, 2), new Point(4, 2)), List.of(), 100, 5));
        return engine;
    }

    private static BoardRenderer rendererFor(GameEngine engine, GameWorld context) {
        return new BoardRenderer(context);
    }

    @Test
    void aSelectedTowerYieldsASpriteDrawWithItsBodyPaletteAndSelectionFlag() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        TowerOne tower = new TowerOne(context, 1, 1);
        tower.setSelected(true);
        context.towers().add(tower);

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites()).hasSize(1);
        TowerSpriteDraw sprite = frame.towerSprites().get(0);
        assertThat(sprite.palette()).isEqualTo(Palette.TOWER_ONE_BODY);
        assertThat(sprite.selected()).isTrue();
    }

    @Test
    void aPlacedTowerYieldsATurretHeadDrawWithItsBodyPalette() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new TowerOne(context, 1, 1));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerHeads()).hasSize(1);
        assertThat(frame.towerHeads().get(0).palette()).isEqualTo(Palette.TOWER_ONE_BODY);
    }

    @Test
    void anUnselectedTowerYieldsASpriteDrawWithSelectedFalse() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new TowerOne(context, 1, 1));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites().get(0).selected()).isFalse();
    }

    @Test
    void aTowerWithNoChosenUpgradePathYieldsNoAccent() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new TowerOne(context, 1, 1));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites().get(0).accent()).isEmpty();
    }

    @Test
    void aTowersSecondUpgradePathYieldsTheAccentPathBRole() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.economy().startEconomy(1000, 5);
        TowerOne tower = new TowerOne(context, 1, 1);
        context.towers().add(tower);
        // TowerOne.availablePaths() = [Veteran, Overclock] - Overclock (index 1) is money-gated,
        // so it's choosable immediately without grinding out Veteran's kill-count condition.
        tower.chooseUpgradePath(tower.availablePaths().get(1));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites().get(0).accent()).contains(Palette.TOWER_UPGRADE_PATH_B);
    }

    @Test
    void aDeadEnemyYieldsAFadeDrawAndNoBodyDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        enemy.doDamage(Damage.physical(5000));
        enemy.doTick(1); // captures deathTick, matching how AbstractEnemyMob really ticks

        context.enemies().setEnemies(new EnemyMob[]{enemy});

        RenderFrame frame = rendererFor(engine, context).buildFrame(1, 0.0, 0.0);

        assertThat(frame.enemies()).hasSize(1).first().isInstanceOf(EnemyFadeDraw.class);
    }

    @Test
    void anEmptyEnemyYieldsNoDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.enemies().setEnemies(new EnemyMob[]{EnemyFactory.getEnemy("e", context, 0, 50, 3, 1)});

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.enemies()).isEmpty();
    }

    @Test
    void onlyHighlightedCellsProduceADraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        // a freshly loaded level has HighlightType.none everywhere

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.cells()).isEmpty();
    }

    @Test
    void aSelectedCellProducesExactlyOneCellDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        engine.cells().at(0, 0).setHighlight(Cell.HighlightType.select);

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.cells()).hasSize(1);
        assertThat(frame.cells().get(0).highlight()).isEqualTo(Cell.HighlightType.select);
    }

    @Test
    void aCinderTowerYieldsAConeDrawEveryTick() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new TowerCinder(context, 1, 1));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerEffects()).hasSize(1);
        assertThat(frame.towerEffects().get(0)).isInstanceOf(ConeDraw.class);
    }

    @Test
    void anInFlightProjectileYieldsAProjectileDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.projectiles().add(new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        }));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.projectiles()).hasSize(1);
        assertThat(frame.projectiles().get(0)).isInstanceOf(ProjectileDraw.class);
    }
}
