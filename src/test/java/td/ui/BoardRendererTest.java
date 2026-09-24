package td.ui;

import org.junit.jupiter.api.Test;
import td.GameEngine;
import td.cell.Cell;
import td.damage.Damage;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.LevelFixtures;
import td.projectile.CannonballProjectile;
import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.SniperTower;
import td.tower.upgrade.UpgradeSlot;
import td.ui.render.ConeDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.Palette;
import td.ui.render.ProjectileDraw;
import td.ui.render.RenderFrame;
import td.ui.render.SlotMarkDraw;
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

    private static BoardRenderer rendererFor(GameEngine engine, GameWorld context) {
        return new BoardRenderer(context);
    }

    @Test
    void aSelectedTowerYieldsASpriteDrawWithItsBodyPaletteAndSelectionFlag() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        SniperTower tower = new SniperTower(context, 1, 1);
        tower.setSelected(true);
        context.towers().add(tower);

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites()).hasSize(1);
        TowerSpriteDraw sprite = frame.towerSprites().getFirst();
        assertThat(sprite.palette()).isEqualTo(Palette.TOWER_SNIPER_BODY);
        assertThat(sprite.selected()).isTrue();
    }

    @Test
    void aPlacedTowerYieldsATurretHeadDrawWithItsBodyPalette() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new SniperTower(context, 1, 1));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerHeads()).hasSize(1);
        assertThat(frame.towerHeads().getFirst().palette()).isEqualTo(Palette.TOWER_SNIPER_BODY);
    }

    @Test
    void anUnselectedTowerYieldsASpriteDrawWithSelectedFalse() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new SniperTower(context, 1, 1));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerSprites().getFirst().selected()).isFalse();
    }

    @Test
    void aTowerWithNothingBoughtYieldsEmptySlotMarksAndNoEnchantPulse() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new SniperTower(context, 1, 1));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        TowerSpriteDraw sprite = frame.towerSprites().getFirst();
        assertThat(sprite.slotMarks()).extracting(SlotMarkDraw::level).containsExactly(0, 0, 0);
        assertThat(sprite.enchantPulse()).isZero();
    }

    @Test
    void aFundedFreshTowersBaseSlotIsMarkedReadyButHeadIsNotYetLocked() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.economy().startEconomy(1000, 5);
        context.towers().add(new SniperTower(context, 1, 1));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        List<SlotMarkDraw> marks = frame.towerSprites().getFirst().slotMarks();
        assertThat(marks.get(UpgradeSlot.BASE.ordinal()).ready()).isTrue();
        assertThat(marks.get(UpgradeSlot.HEAD.ordinal()).ready()).isFalse();
        assertThat(marks.get(UpgradeSlot.SPECIAL.ordinal()).ready()).isFalse();
    }

    @Test
    void buyingTwoHeadNodesShowsTwoPipsInTheHeadSlotAndOneInBase() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(context, 1, 1);
        context.towers().add(tower);
        tower.buyUpgrade(tower.upgradeTree().nodesIn(UpgradeSlot.BASE).get(1));
        tower.buyUpgrade(tower.upgradeTree().nodesIn(UpgradeSlot.HEAD).get(0));
        tower.buyUpgrade(tower.upgradeTree().nodesIn(UpgradeSlot.HEAD).get(1));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        List<SlotMarkDraw> marks = frame.towerSprites().getFirst().slotMarks();
        assertThat(marks.get(UpgradeSlot.BASE.ordinal()).level()).isEqualTo(1);
        assertThat(marks.get(UpgradeSlot.HEAD.ordinal()).level()).isEqualTo(2);
        assertThat(marks.get(UpgradeSlot.SPECIAL.ordinal()).level()).isZero();
    }

    @Test
    void owningASpecialNodeGivesTheSpriteAPositiveEnchantPulse() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.economy().startEconomy(1000, 5);
        // its special root is gated on a cluster of 2 towers, reachable without combat
        AuraTower tower = new AuraTower(context, 1, 1);
        context.towers().add(tower);
        context.towers().add(new SniperTower(context, 0, 0));
        context.towers().add(new SniperTower(context, 2, 2));
        tower.buyUpgrade(tower.upgradeTree().nodesIn(UpgradeSlot.BASE).get(1));
        tower.buyUpgrade(tower.upgradeTree().nodesIn(UpgradeSlot.SPECIAL).get(0));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        TowerSpriteDraw sprite = frame.towerSprites().stream()
                .filter(s -> s.palette() == Palette.TOWER_AURA_BODY)
                .findFirst().orElseThrow();
        assertThat(sprite.enchantPulse()).isGreaterThan(0f);
    }

    @Test
    void aDeadEnemyYieldsAFadeDrawAndNoBodyDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.doDamage(Damage.physical(5000));
        enemy.doTick(1); // captures deathTick, matching how a mob really ticks

        context.enemies().setEnemies(new EnemyMob[]{enemy});

        RenderFrame frame = rendererFor(engine, context).buildFrame(1, 0.0, 0.0);

        assertThat(frame.enemies()).hasSize(1).first().isInstanceOf(EnemyFadeDraw.class);
    }

    @Test
    void onlyHighlightedCellsProduceADraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        // a freshly loaded level has HighlightType.NONE everywhere

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.cells()).isEmpty();
    }

    @Test
    void aSelectedCellProducesExactlyOneCellDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        engine.cells().at(0, 0).setHighlight(Cell.HighlightType.SELECT);

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.cells()).hasSize(1);
        assertThat(frame.cells().getFirst().highlight()).isEqualTo(Cell.HighlightType.SELECT);
    }

    @Test
    void aCinderTowerWithNoInFlightWaveYieldsNoConeDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new CinderTower(context, 1, 1));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerEffects()).isEmpty();
    }

    @Test
    void aCinderTowerThatJustFiredAWaveYieldsAConeDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        CinderTower tower = new CinderTower(context, 1, 1);
        context.towers().add(tower);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        context.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(1); // in range, cools down from 0 - fires a wave

        RenderFrame frame = rendererFor(engine, context).buildFrame(1, 0.0, 0.0);

        assertThat(frame.towerEffects()).hasSize(1);
        assertThat(frame.towerEffects().getFirst()).isInstanceOf(ConeDraw.class);
    }

    @Test
    void anInFlightProjectileYieldsAProjectileDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.projectiles().add(new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        }));

        RenderFrame frame = rendererFor(engine, context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.projectiles()).hasSize(1);
        assertThat(frame.projectiles().getFirst()).isInstanceOf(ProjectileDraw.class);
    }
}
