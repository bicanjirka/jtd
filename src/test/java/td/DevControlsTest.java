package td;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.EffectKind;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeTower;
import td.fixtures.LevelFixtures;
import td.level.LevelOutcome;
import td.term.Term;
import td.tower.Tower;
import td.tower.TowerBuffState;
import td.tower.TowerFactory;
import td.tower.TowerTerm;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.wave.WaveDefinition;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DevControlsTest {

    private final GameEngine engine = FakeGameHost.newBoundEngine();
    private final GameWorld world = this.engine.getGameWorld();
    private final DevControls dev = new DevControls(this.world);

    DevControlsTest() {
        this.world.economy().addEconomyListener(this.dev);
        this.world.playtestHooks().add(this.dev::doTick);
    }

    @Test
    void creditsAndLivesAreSetToExactlyWhatWasTyped() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        this.dev.setCredits(4321);
        this.dev.setLives(42);

        assertThat(this.world.economy().getCredits()).isEqualTo(4321);
        assertThat(this.world.economy().getLives()).isEqualTo(42);
    }

    @Test
    void withInfiniteLivesALeakOnTheLastLifeComesBackAndTheLevelGoesOn() {
        this.engine.loadLevel(LevelFixtures.biggerLevelWith(
                List.of(new WaveDefinition("simple", Rank.GRUNT), new WaveDefinition("simple", Rank.GRUNT)), 100)
                .withStartingLives(1));
        this.dev.setInfiniteLives(true);
        this.engine.nextWave();

        tickUntilClear();

        assertThat(this.engine.outcome()).isEqualTo(LevelOutcome.PLAYING);
        assertThat(this.world.economy().getLives()).isEqualTo(1);
    }

    @Test
    void freeBuildKeepsCreditsAtAMillionThroughAPurchase() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.dev.setFreeBuild(true);

        Tower tower = this.placeSniper(1, 1);

        assertThat(tower).isNotNull();
        assertThat(this.world.economy().getCredits()).isEqualTo(DevControls.FREE_BUILD_CREDITS);
    }

    @Test
    void aSpawnedScriptJoinsTheBoardOnlyWhenTheGameLoopRunsIt() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        String message = this.dev.spawnWave("3 simple", Rank.GRUNT, 0);
        int beforeRun = this.world.enemies().getEnemies().length;
        this.dev.runPending();

        assertThat(message).isEqualTo("Spawning 3 on path 1");
        assertThat(beforeRun).isZero();
        assertThat(this.world.enemies().aliveCount()).isEqualTo(3);
    }

    @Test
    void aScriptThatDoesNotParseSpawnsNothingAndSaysWhy() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        String message = this.dev.spawnWave("3 nosuchenemy", Rank.GRUNT, 0);
        this.dev.runPending();

        assertThat(message).contains("nosuchenemy");
        assertThat(this.world.enemies().getEnemies()).isEmpty();
    }

    @Test
    void checkingAScriptPreviewsItsWaveOnTheChosenPathAndSpawnsNothing() {
        this.engine.loadLevel(LevelFixtures.twoPathLevelWith(List.of(), List.of()));

        DevControls.WaveScriptCheck check = this.dev.checkWaveScript("2 elite simple", Rank.GRUNT, 1);
        this.dev.runPending();

        assertThat(check.wave()).get().satisfies(wave -> {
            assertThat(wave.enemyCount()).isEqualTo(2);
            assertThat(wave.getPathIndex()).isEqualTo(1);
            assertThat(wave.enemySet()).allSatisfy(enemy -> assertThat(wave.rankFor(enemy)).isEqualTo(Rank.ELITE));
        });
        assertThat(this.world.enemies().getEnemies()).isEmpty();
    }

    @Test
    void checkingAScriptThatDoesNotParseGivesTheProblemAndNoWave() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        DevControls.WaveScriptCheck check = this.dev.checkWaveScript("3 nosuchenemy", Rank.GRUNT, 0);

        assertThat(check.wave()).isEmpty();
        assertThat(check.problem()).contains("nosuchenemy");
    }

    @Test
    void killAllPaysEachEnemysBountyAndXpLikeARealKill() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        Tower tower = this.placeSniper(1, 1);
        this.dev.spawnWave("simple", Rank.GRUNT, 0);
        this.dev.runPending();
        int bounty = this.world.enemies().getEnemies()[0].getBounty();
        int creditsBefore = this.world.economy().getCredits();

        this.dev.killAll();
        this.dev.runPending();

        assertThat(this.world.enemies().aliveCount()).isZero();
        assertThat(this.world.economy().getCredits()).isEqualTo(creditsBefore + bounty);
        assertThat(tower.experience().xp()).isEqualTo(bounty);
    }

    @Test
    void clearAllRemovesEveryEnemyAndPaysNothing() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.dev.spawnWave("4 simple", Rank.GRUNT, 0);
        this.dev.runPending();
        int creditsBefore = this.world.economy().getCredits();

        this.dev.clearAll();
        this.dev.runPending();

        assertThat(this.world.enemies().getEnemies()).isEmpty();
        assertThat(this.world.economy().getCredits()).isEqualTo(creditsBefore);
    }

    @Test
    void xpGoesToTheSelectedTowerAndNeedsOne() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        Tower tower = this.placeSniper(1, 1);
        String withoutSelection = this.dev.grantXpToSelected(100);
        tower.setSelected(true);
        this.dev.runPending();

        String withSelection = this.dev.grantXpToSelected(100);
        this.dev.runPending();

        assertThat(withoutSelection).isEqualTo("Select a tower first");
        assertThat(withSelection).isEqualTo("+100 XP to " + tower.displayName());
        assertThat(tower.experience().xp()).isEqualTo(100);
    }

    @Test
    void ignoringGatesLetsATowerBuyANodeBeforeItHasTheXp() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 1000));
        UpgradeNode needsXp = UpgradeNode.of("gated", UpgradeSlot.HEAD, "Gated", 10).withXp(50);
        Tower tower = FakeTower.offering(this.world, 1, 1, UpgradeTree.of(needsXp));
        boolean gated = tower.buyUpgrade(needsXp);

        this.dev.setUpgradeGatesIgnored(true);
        boolean ignored = tower.buyUpgrade(needsXp);

        assertThat(gated).isFalse();
        assertThat(ignored).isTrue();
    }

    @Test
    void enemyHealthPercentScalesOnlyTheEnemiesSpawnedAfterItAndRefusesZero() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.dev.spawnWave("simple", Rank.GRUNT, 0);
        this.dev.runPending();
        int authored = this.world.enemies().getEnemies()[0].getHealth();

        String refused = this.dev.setEnemyHealthPercent(0);
        String set = this.dev.setEnemyHealthPercent(250);
        this.dev.spawnWave("simple", Rank.GRUNT, 0);
        this.dev.runPending();

        assertThat(refused).isEqualTo("Enemy health must be above 0%");
        assertThat(set).isEqualTo("New enemies get 250% health");
        assertThat(this.world.enemies().getEnemies()).extracting(EnemyMob::getHealth)
                .containsExactly(authored, DamageUnits.ofPoints(Math.round(EnemyCatalog.builtIn().get("simple").baseHealth() * 2.5f)));
    }

    @Test
    void anEffectGoesOnTheSelectedEnemyOnlyAndNeedsOne() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.dev.spawnWave("2 simple", Rank.GRUNT, 0);
        this.dev.runPending();
        String withoutSelection = this.dev.applyEffectToEnemy(EffectKind.FREEZE, List.of(5f));
        this.selectFirstEnemy();

        String withSelection = this.dev.applyEffectToEnemy(EffectKind.FREEZE, List.of(5f));
        this.dev.runPending();

        assertThat(withoutSelection).isEqualTo("Select an enemy first");
        assertThat(withSelection).isEqualTo("Applying " + EffectKind.FREEZE.title());
        assertThat(this.dev.report().text()).isEqualTo("Applied " + EffectKind.FREEZE.title());
        assertThat(this.frozenEnemies()).isEqualTo(1);
    }


    @Test
    void anEffectThatDoesNotTakeHoldIsReportedOnceTheGameLoopRunsIt() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.spawnAndSelectOne();
        this.dev.applyEffectToEnemy(EffectKind.INVERSION, List.of(5f));
        this.dev.runPending();
        long before = this.dev.report().serial();

        this.dev.applyEffectToEnemy(EffectKind.SHIELD, List.of(5f, 50f));
        this.dev.runPending();

        assertThat(this.dev.report().serial()).isGreaterThan(before);
        assertThat(this.dev.report().text()).startsWith(EffectKind.SHIELD.title() + " didn't take hold");
    }

    @Test
    void aDamageEffectFromThePanelHurtsTheEnemy() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        EnemyMob mob = this.spawnAndSelectOne();
        int full = mob.getHealth();

        this.dev.applyEffectToEnemy(EffectKind.POISON, List.of(5f, 100f));
        this.dev.runPending();
        for (int t = 1; t <= 20; t++) {
            this.engine.doTick(t);
        }

        assertThat(mob.getHealth()).isLessThan(full);
    }

    @Test
    void aChargeFromThePanelDischargesIntoTheEnemyWhenAnotherAttackerHitsIt() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        EnemyMob charged = this.spawnAndSelectOne();
        this.dev.applyEffectToEnemy(EffectKind.CHARGED, List.of(5f));
        this.dev.runPending();
        EnemyMob plain = this.spawnAnotherLive();
        AttackProfile tower = AttackProfile.none().withOrigin(AttackOrigin.fresh());

        charged.doDamage(Damage.magic(charged.getHealth() / 4), tower);
        plain.doDamage(Damage.magic(plain.getHealth() / 4), tower);

        assertThat(charged.getHealth()).isLessThan(plain.getHealth());
    }

    @Test
    void aDoomFromThePanelPaysAShareOfWhatTheEnemyTookUnderItWhenItEnds() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        EnemyMob doomed = this.spawnAndSelectOne();
        this.dev.applyEffectToEnemy(EffectKind.DOOM, List.of(0.5f));
        this.dev.runPending();
        EnemyMob plain = this.spawnAnotherLive();
        doomed.doDamage(Damage.magic(doomed.getHealth() / 4));
        plain.doDamage(Damage.magic(plain.getHealth() / 4));

        for (int t = 1; t <= 20; t++) {
            this.engine.doTick(t);
        }

        assertThat(doomed.hasEffect(EffectKind.DOOM)).isFalse();
        assertThat(doomed.getHealth()).isLessThan(plain.getHealth());
    }

    @Test
    void anEffectThePanelCannotMakeWorkIsRefusedWithItsReason() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.spawnAndSelectOne();

        String message = this.dev.applyEffectToEnemy(EffectKind.STILLED, List.of(5f));
        this.dev.runPending();

        assertThat(message).isEqualTo(EffectKind.STILLED.title() + " " + DevEffects.whyUnusable(EffectKind.STILLED).orElseThrow());
        assertThat(this.world.enemies().getEnemies()[0].hasEffect(EffectKind.STILLED)).isFalse();
    }

    @Test
    void aValueOutsideItsFieldsRangeIsRefusedByNameAndAppliesNothing() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        EnemyMob mob = this.spawnAndSelectOne();
        DevField stacks = DevEffects.enemyFields(EffectKind.VULNERABLE).get(1);

        String message = this.dev.applyEffectToEnemy(EffectKind.VULNERABLE, List.of(5f, stacks.max() + 1));
        this.dev.runPending();

        assertThat(message).isEqualTo(stacks.problemWith(stacks.max() + 1).orElseThrow());
        assertThat(message).startsWith(stacks.label());
        assertThat(mob.hasEffect(EffectKind.VULNERABLE)).isFalse();
    }

    @Test
    void clearingTheSelectedEnemysEffectsEndsThemAll() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.dev.spawnWave("simple", Rank.GRUNT, 0);
        this.dev.runPending();
        this.selectFirstEnemy();
        this.dev.applyEffectToEnemy(EffectKind.FREEZE, List.of(5f));
        this.dev.runPending();

        this.dev.clearEnemyEffects();
        this.dev.runPending();

        assertThat(this.frozenEnemies()).isZero();
    }

    @Test
    void killingTheSelectedEnemyPaysItsBounty() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.dev.spawnWave("simple", Rank.GRUNT, 0);
        this.dev.runPending();
        this.selectFirstEnemy();
        int creditsBefore = this.world.economy().getCredits();

        this.dev.killSelectedEnemy();
        this.dev.runPending();

        assertThat(this.world.enemies().aliveCount()).isZero();
        assertThat(this.world.economy().getCredits()).isGreaterThan(creditsBefore);
    }

    @Test
    void removingTheSelectedEnemyTakesItOffTheBoardAndPaysNothing() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.dev.spawnWave("simple", Rank.GRUNT, 0);
        this.dev.runPending();
        this.selectFirstEnemy();
        int creditsBefore = this.world.economy().getCredits();

        this.dev.removeSelectedEnemy();
        this.dev.runPending();

        assertThat(this.world.enemies().getEnemies()).isEmpty();
        assertThat(this.world.enemies().aliveCount()).isZero();
        assertThat(this.world.economy().getCredits()).isEqualTo(creditsBefore);
    }

    @Test
    void healingTheSelectedEnemyRestoresItsFullHealth() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.dev.spawnWave("simple", Rank.GRUNT, 0);
        this.dev.runPending();
        EnemyMob mob = this.world.enemies().getEnemies()[0];
        int full = mob.getHealth();
        mob.doDamage(Damage.physical(full / 2));
        this.selectFirstEnemy();

        this.dev.healSelectedEnemy();
        this.dev.runPending();

        assertThat(mob.getHealth()).isEqualTo(full);
    }

    @Test
    void theSelectionNamesTheTowerBeforeTheEnemyAndNothingWhenNoneIsPicked() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        Tower tower = this.placeSniper(1, 1);
        this.dev.spawnWave("simple", Rank.GRUNT, 0);
        this.dev.runPending();
        DevSelection nothing = this.dev.selection();
        this.selectFirstEnemy();
        DevSelection enemy = this.dev.selection();
        tower.setSelected(true);
        this.dev.runPending();

        assertThat(nothing.kind()).isEqualTo(DevSelection.Kind.NONE);
        assertThat(enemy.kind()).isEqualTo(DevSelection.Kind.ENEMY);
        assertThat(this.dev.selection().kind()).isEqualTo(DevSelection.Kind.TOWER);
    }

    @Test
    void theSelectionIsNamedByItsNameAlone() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        Tower tower = this.placeSniper(1, 1);
        this.dev.spawnWave("simple", Rank.GRUNT, 0);
        this.dev.runPending();
        this.selectFirstEnemy();
        String enemy = this.dev.selection().description();

        tower.setSelected(true);
        this.dev.runPending();

        assertThat(enemy).isEqualTo(EnemyCatalog.builtIn().get("simple").displayName());
        assertThat(this.dev.selection().description()).isEqualTo(tower.displayName());
    }

    @Test
    void aJamFromThePanelHoldsOnlyTheSelectedTowerJammedUntilItRunsOut() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        Tower jammed = this.placeSniper(1, 1);
        Tower neighbour = this.placeSniper(2, 1);
        jammed.setSelected(true);
        this.dev.runPending();
        this.dev.grantToSelectedTower(TowerTerm.JAMMED, List.of(0.5f, 30f, 1f));
        this.dev.runPending();

        this.engine.doTick(1);
        boolean whileHeld = jammed.isDisrupted();
        List<Term> shown = jammed.conditions().stream().map(TowerBuffState::term).toList();
        for (int t = 2; t <= 20; t++) {
            this.engine.doTick(t);
        }

        assertThat(whileHeld).isTrue();
        assertThat(shown).contains(TowerTerm.JAMMED);
        assertThat(neighbour.isDisrupted()).isFalse();
        assertThat(jammed.isDisrupted()).isFalse();
    }

    @Test
    void aTowerTermThePanelCannotGrantIsRefusedWithItsReason() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        Tower tower = this.placeSniper(1, 1);
        tower.setSelected(true);
        this.dev.runPending();

        String message = this.dev.grantToSelectedTower(TowerTerm.STOKE, List.of());

        assertThat(message).isEqualTo(TowerTerm.STOKE.title() + " " + DevEffects.whyUngrantable(TowerTerm.STOKE).orElseThrow());
    }

    @Test
    void aBuffGoesOnTheSelectedTowerAndNeedsOne() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        Tower tower = this.placeSniper(1, 1);
        String withoutSelection = this.dev.grantToSelectedTower(TowerTerm.HASTE, List.of(5f, 50f));
        tower.setSelected(true);
        this.dev.runPending();

        String withSelection = this.dev.grantToSelectedTower(TowerTerm.HASTE, List.of(5f, 50f));
        this.dev.runPending();

        assertThat(withoutSelection).isEqualTo("Select a tower first");
        assertThat(withSelection).startsWith("Granted " + TowerTerm.HASTE.title());
        assertThat(tower.conditions()).extracting(TowerBuffState::term).contains(TowerTerm.HASTE);
    }

    private EnemyMob spawnAndSelectOne() {
        this.dev.spawnWave("simple", Rank.GRUNT, 0);
        this.dev.runPending();
        this.selectFirstEnemy();
        return this.world.enemies().getEnemies()[0];
    }

    /** One more enemy, the first of a wave of its own, so it is live at once and takes hits like the first. */
    private EnemyMob spawnAnotherLive() {
        this.dev.spawnWave("simple", Rank.GRUNT, 0);
        this.dev.runPending();
        EnemyMob[] enemies = this.world.enemies().getEnemies();
        return enemies[enemies.length - 1];
    }

    private void selectFirstEnemy() {
        EnemyMob first = this.world.enemies().getEnemies()[0];
        this.world.enemySelection().requestAt((int) first.getX(), (int) first.getY());
        this.engine.inspectSelectedEnemy();
        this.dev.runPending();
    }

    private long frozenEnemies() {
        return Arrays.stream(this.world.enemies().getEnemies()).filter(mob -> mob.hasEffect(EffectKind.FREEZE)).count();
    }

    private Tower placeSniper(int cellX, int cellY) {
        this.engine.startPlacing(TowerFactory.Type.SNIPER, 0f);
        this.engine.mouseClicked(BoardFixtures.cellCenter(cellX), BoardFixtures.cellCenter(cellY));
        return this.engine.cells().at(cellX, cellY).getTower();
    }

    private void tickUntilClear() {
        for (int t = 1; t < 2000 && this.world.enemies().aliveCount() > 0; t++) {
            this.engine.doTick(t);
        }
    }

}
