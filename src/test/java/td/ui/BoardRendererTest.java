package td.ui;

import org.junit.jupiter.api.Test;
import td.GameEngine;
import td.cell.Cell;
import td.damage.Damage;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.EnemyFixtures;
import td.fixtures.LevelFixtures;
import td.projectile.CannonballProjectile;
import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.SniperTower;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerRank;
import td.tower.TowerTerm;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.ui.render.CellGridDraw;
import td.ui.render.ConeDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.Palette;
import td.ui.render.ProjectileDraw;
import td.ui.render.RenderFrame;
import td.ui.render.SheetLine;
import td.ui.render.SlotMarkDraw;
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
        return new BoardRenderer(context);
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
        assertThat(sprite.palette()).isEqualTo(Palette.TOWER_SNIPER_BODY);
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
        assertThat(sheet.sheet().lines().getFirst()).isInstanceOf(SheetLine.Title.class);
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
    void aPlacedTowerYieldsATurretHeadDrawWithItsBodyPalette() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new SniperTower(context, 1, 1));

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerHeads()).hasSize(1);
        assertThat(frame.towerHeads().getFirst().palette()).isEqualTo(Palette.TOWER_SNIPER_BODY);
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
    void aTowerWithNothingBoughtYieldsEmptySlotMarksAndNoEnchantPulse() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new SniperTower(context, 1, 1));

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        TowerSpriteDraw sprite = frame.towerSprites().getFirst();
        assertThat(sprite.slotMarks()).extracting(SlotMarkDraw::level).containsExactly(0, 0, 0, 0);
        assertThat(sprite.enchantPulse()).isZero();
    }

    @Test
    void aFundedFreshTowersCoreSlotIsMarkedReadyButHeadIsNotYetLocked() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.economy().startEconomy(1000, 5);
        context.towers().add(new SniperTower(context, 1, 1));

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        List<SlotMarkDraw> marks = frame.towerSprites().getFirst().slotMarks();
        assertThat(marks.get(UpgradeSlot.CORE.ordinal()).ready()).isTrue();
        assertThat(marks.get(UpgradeSlot.HEAD.ordinal()).ready()).isFalse();
        assertThat(marks.get(UpgradeSlot.SPECIAL.ordinal()).ready()).isFalse();
    }

    @Test
    void buyingTwoHeadNodesShowsTwoPipsInTheHeadSlotAndOneInCore() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(context, 1, 1);
        context.towers().add(tower);
        tower.earnXp(1_000);
        tower.buyUpgrade(coreNode(tower, StandardBaseSlot.ATTUNE_ID));
        tower.buyUpgrade(tower.upgradeTree().nodesIn(UpgradeSlot.HEAD).get(0));
        tower.buyUpgrade(tower.upgradeTree().nodesIn(UpgradeSlot.HEAD).get(1));

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        List<SlotMarkDraw> marks = frame.towerSprites().getFirst().slotMarks();
        assertThat(marks.get(UpgradeSlot.CORE.ordinal()).level()).isEqualTo(1);
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
        tower.earnXp(1_000);
        tower.buyUpgrade(coreNode(tower, StandardBaseSlot.ATTUNE_ID));
        tower.buyUpgrade(coreNode(tower, StandardBaseSlot.AWAKEN_ID));
        tower.buyUpgrade(tower.upgradeTree().nodesIn(UpgradeSlot.SPECIAL).getFirst());

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        TowerSpriteDraw sprite = frame.towerSprites().stream()
                .filter(s -> s.palette() == Palette.TOWER_AURA_BODY)
                .findFirst().orElseThrow();
        assertThat(sprite.enchantPulse()).isGreaterThan(0f);
    }

    @Test
    void aTowerShowsItsRankAndGlowsForAMomentAfterRankingUp() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        SniperTower tower = new SniperTower(context, 1, 1);
        context.towers().add(tower);
        tower.beginTick(100);
        tower.earnXp(160);

        TowerSpriteDraw justRanked = rendererFor(context).buildFrame(110, 0.0, 0.0).towerSprites().getFirst();
        TowerSpriteDraw later = rendererFor(context).buildFrame(200, 0.0, 0.0).towerSprites().getFirst();

        assertThat(justRanked.rank()).isEqualTo(TowerRank.EXPERT.ordinal());
        assertThat(justRanked.rankUpProgress()).isBetween(0f, 1f);
        assertThat(later.rankUpProgress()).isEqualTo(-1f);
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
    void onlyHighlightedCellsProduceADraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        // a freshly loaded level has HighlightType.NONE everywhere

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.cells()).isEmpty();
    }

    @Test
    void aPlacementHoverIsAffordableOnlyWhileTheCreditsCoverItsPrice() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        Cell cell = engine.cells().at(0, 0);
        cell.setHighlight(Cell.HighlightType.PLACE);
        cell.setHighlightPrice(context.economy().getCredits() + 1);

        boolean shortOfCash = rendererFor(context).buildFrame(0, 0.0, 0.0).cells().getFirst().affordable();
        cell.setHighlightPrice(context.economy().getCredits());
        boolean covered = rendererFor(context).buildFrame(0, 0.0, 0.0).cells().getFirst().affordable();

        assertThat(shortOfCash).isFalse();
        assertThat(covered).isTrue();
    }

    @Test
    void aSelectedCellProducesExactlyOneCellDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        engine.cells().at(0, 0).setHighlight(Cell.HighlightType.SELECT);

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.cells()).hasSize(1);
        assertThat(frame.cells().getFirst().highlight()).isEqualTo(Cell.HighlightType.SELECT);
    }

    @Test
    void aCinderTowerWithNoInFlightWaveYieldsNoConeDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        context.towers().add(new CinderTower(context, 1, 1));

        RenderFrame frame = rendererFor(context).buildFrame(0, 0.0, 0.0);

        assertThat(frame.towerEffects()).isEmpty();
    }

    @Test
    void aCinderTowerThatJustFiredAWaveYieldsAConeDraw() {
        GameEngine engine = newEngine();
        GameWorld context = engine.getGameWorld();
        CinderTower tower = new CinderTower(context, 1, 1);
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

    private static UpgradeNode coreNode(Tower tower, String id) {
        return tower.upgradeTree().nodesIn(UpgradeSlot.CORE).stream().filter(n -> n.id().equals(id)).findFirst().orElseThrow();
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
        cinder.buyUpgrade(coreNode(cinder, StandardBaseSlot.ATTUNE_ID));
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
