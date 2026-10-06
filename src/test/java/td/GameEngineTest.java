package td;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageMix;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.FreezeDiminishing;
import td.enemy.BodyArchetype;
import td.enemy.EffectResistTrait;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyInspection;
import td.enemy.EnemyMob;
import td.enemy.HurtSpeedTrait;
import td.enemy.PercentResistTrait;
import td.enemy.Rank;
import td.enemy.Trait;
import td.fixtures.BoardFixtures;
import td.fixtures.EnemyFixtures;
import td.fixtures.LevelFixtures;
import td.level.LevelDefinition;
import td.level.LevelOutcome;
import td.projectile.CannonballProjectile;
import td.stat.DisruptionAura;
import td.tower.AuraTower;
import td.tower.MortarTower;
import td.tower.SniperTower;
import td.tower.SplashTower;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerStat;
import td.tower.TowerStatLine;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeTier;
import td.util.GameWorld;
import td.util.LoadedLevel;
import td.wave.WaveDefinition;
import td.wave.WaveProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

class GameEngineTest {

    // Weak enough for a guaranteed one-shot kill, independent of built-in content.
    private static final EnemyDefinition WEAKLING = EnemyDefinition.of("weakling", "Weakling", 1, 7, 1.28f,
            BodyArchetype.CIRCLE);

    /** Returns the tick the level ended on, or the budget if it never did. */
    private static int tickUntilOver(GameEngine engine, int budget) {
        int t = 1;
        for (; t < budget && !engine.outcome().isOver(); t++) {
            engine.doTick(t);
        }
        return t;
    }

    private static EnemyMob spawnOnly(GameEngine engine, EnemyDefinition definition) {
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition(definition.id(), Rank.GRUNT)), 100)
                .withCustomEnemies(List.of(definition)));
        engine.nextWave();
        return engine.getGameWorld().enemies().getEnemies()[0];
    }

    @Test
    void aHalfPhysicalResistEnemyTakesHalfOfAPhysicalHitAndAllOfAMagicOne() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyMob enemy = spawnOnly(engine, EnemyDefinition.of("resist", "Resist", 100, 1, 1f, BodyArchetype.SQUARE)
                .withTraits(List.of(PercentResistTrait.physicalOnly(0.5f))));

        Damage physical = enemy.doDamage(Damage.physical(1000));
        Damage magic = enemy.doDamage(Damage.magic(1000));

        assertThat(physical).isEqualTo(Damage.physical(500));
        assertThat(magic).isEqualTo(Damage.magic(1000));
    }

    @Test
    void aShieldMultipliesWithArmorRatherThanAddingToIt() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyMob enemy = spawnOnly(engine, EnemyDefinition.of("resist", "Resist", 100, 1, 1f, BodyArchetype.SQUARE)
                .withTraits(List.of(PercentResistTrait.physicalOnly(0.5f))));
        enemy.applyEffect(Effect.shield(0.5f, 100, d -> {
        }));

        Damage landed = enemy.doDamage(Damage.physical(1000));

        assertThat(landed).isEqualTo(Damage.physical(250));
    }

    @Test
    void aShieldedHurtSpeedEnemyStillSpeedsUpWhenHit() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyMob enemy = spawnOnly(engine, EnemyDefinition.of("hurry", "Hurry", 100, 1, 1f, BodyArchetype.TRIANGLE)
                .withTraits(List.of(new HurtSpeedTrait(2f))));
        enemy.applyEffect(Effect.shield(0.5f, 100, d -> {
        }));
        float before = enemy.getSpeed();

        enemy.doDamage(Damage.physical(10000));

        // Half of the 100-point pool is gone, so the multiplier is 1 + (2 - 1) * 0.5.
        assertThat(enemy.getSpeed()).isCloseTo(before * 1.5f, within(0.001f));
    }

    private static EnemyMob spawnStill(GameEngine engine, Rank rank, Trait... traits) {
        EnemyDefinition still = EnemyDefinition.of("still", "Still", 100, 1, 0f, BodyArchetype.CIRCLE)
                .withTraits(List.of(traits));
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100).withCustomEnemies(List.of(still)));
        GameWorld world = engine.getGameWorld();
        return world.getEnemyCatalog().spawn("still", world, 0, 100, 1, rank);
    }

    /** Ticks {@code enemy} until it thaws and returns how many ticks it stayed frozen. */
    private static int ticksFrozen(EnemyMob enemy, int[] clock) {
        int frozen = 0;
        while (enemy.activeEffectKinds().contains(EffectKind.FREEZE)) {
            enemy.doTick(++clock[0]);
            frozen++;
        }
        return frozen;
    }

    private static int freezeFor(EnemyMob enemy, int durationTicks, int[] clock) {
        enemy.applyEffect(Effect.freeze(durationTicks, d -> {
        }));
        return ticksFrozen(enemy, clock);
    }

    @Test
    void aHalfFreezeResistHalvesAFreeze() {
        EnemyMob enemy = spawnStill(FakeGameHost.newBoundEngine(), Rank.GRUNT,
                EffectResistTrait.resisting(EffectKind.FREEZE, 0.5f));

        int frozen = freezeFor(enemy, 20, new int[] {0});

        assertThat(frozen).isEqualTo(10);
    }

    @Test
    void fullFreezeResistBlocksAFreezeOutright() {
        EnemyMob enemy = spawnStill(FakeGameHost.newBoundEngine(), Rank.GRUNT, EffectResistTrait.immuneTo(EffectKind.FREEZE));

        int frozen = freezeFor(enemy, 20, new int[] {0});

        assertThat(frozen).isZero();
    }

    @Test
    void repeatedFreshFreezesOnADiminishingEnemyLastFullHalfQuarterThenNotAtAll() {
        EnemyMob enemy = spawnStill(FakeGameHost.newBoundEngine(), Rank.GRUNT);
        int[] clock = {0};

        List<Integer> durations = List.of(freezeFor(enemy, 20, clock), freezeFor(enemy, 20, clock),
                freezeFor(enemy, 20, clock), freezeFor(enemy, 20, clock));

        assertThat(durations).containsExactly(20, 10, 5, 0);
    }

    @Test
    void aFreezeReappliedWhileFrozenDoesNotSpendADiminishingStep() {
        EnemyMob enemy = spawnStill(FakeGameHost.newBoundEngine(), Rank.GRUNT);
        int[] clock = {0};
        enemy.applyEffect(Effect.freeze(20, d -> {
        }));
        enemy.doTick(++clock[0]);

        enemy.applyEffect(Effect.freeze(20, d -> {
        }));
        ticksFrozen(enemy, clock);

        assertThat(freezeFor(enemy, 20, clock)).isEqualTo(10);
    }

    @Test
    void freezeDiminishingResetsTenSecondsAfterTheLastFreeze() {
        EnemyMob enemy = spawnStill(FakeGameHost.newBoundEngine(), Rank.GRUNT);
        int[] clock = {0};
        freezeFor(enemy, 20, clock);
        freezeFor(enemy, 20, clock);

        while (clock[0] < FreezeDiminishing.WINDOW_TICKS + 30) {
            enemy.doTick(++clock[0]);
        }

        assertThat(freezeFor(enemy, 20, clock)).isEqualTo(20);
    }

    @Test
    void anEliteDiminishesFreezesWithoutTheTrait() {
        EnemyMob elite = spawnStill(FakeGameHost.newBoundEngine(), Rank.ELITE);
        int[] clock = {0};

        freezeFor(elite, 20, clock);

        assertThat(freezeFor(elite, 20, clock)).isEqualTo(10);
    }

    @Test
    void halfBurnResistRoughlyHalvesTotalBurnDamage() {
        int unresisted = totalBurnDamage(spawnStill(FakeGameHost.newBoundEngine(), Rank.GRUNT));
        int resisted = totalBurnDamage(spawnStill(FakeGameHost.newBoundEngine(), Rank.GRUNT,
                EffectResistTrait.resisting(EffectKind.BURN, 0.5f)));

        assertThat((float) resisted / unresisted).isCloseTo(0.5f, within(0.1f));
    }

    private static int totalBurnDamage(EnemyMob enemy) {
        List<Damage> received = new ArrayList<>();
        enemy.applyEffect(Effect.burn(Damage.magic(100), 60, received::add));
        for (int t = 1; enemy.activeEffectKinds().contains(EffectKind.BURN); t++) {
            enemy.doTick(t);
        }
        return received.stream().mapToInt(Damage::amount).sum();
    }

    @Test
    void aDisruptingEnemyShrinksANearbyTowersRangeOnlyWhileItLives() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyDefinition jammer = EnemyDefinition.of("jam", "Jam", 100, 1, 0f, BodyArchetype.SQUARE)
                .withDisruption(new DisruptionAura(1000f, 0.3f, 0.2f));
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("jam", Rank.GRUNT)), 1000)
                .withCustomEnemies(List.of(jammer)));
        engine.startPlacing(TowerFactory.Type.AURA, AuraTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1));
        Tower tower = engine.getGameWorld().towers().all().getFirst();
        float fullRange = tower.getRangeReal();
        engine.nextWave();

        engine.doTick(1);
        boolean disruptedWhileAlive = tower.isDisrupted();
        float disruptedRange = tower.getRangeReal();
        engine.getGameWorld().enemies().getEnemies()[0].doDamage(Damage.physical(1_000_000));
        engine.doTick(2);

        assertThat(disruptedWhileAlive).isTrue();
        assertThat(disruptedRange).isCloseTo(fullRange * 0.8f, within(0.01f));
        assertThat(tower.isDisrupted()).isFalse();
        assertThat(tower.getRangeReal()).isEqualTo(fullRange);
    }

    @Test
    void aDisruptingEnemyLengthensANearbyTowersCooldownOnlyWhileItLives() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyDefinition jammer = EnemyDefinition.of("jam", "Jam", 100, 1, 0f, BodyArchetype.SQUARE)
                .withDisruption(new DisruptionAura(1000f, 0.3f, 0.2f));
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("jam", Rank.GRUNT)), 1000)
                .withCustomEnemies(List.of(jammer)));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1));
        Tower tower = engine.getGameWorld().towers().all().getFirst();
        engine.nextWave();

        engine.doTick(1);
        TowerStatLine jammedRate = fireRate(tower);
        engine.getGameWorld().enemies().getEnemies()[0].doDamage(Damage.physical(1_000_000));
        engine.doTick(2);

        // A fire-rate penalty lengthens the cooldown by that fraction, so 30% leaves 1/1.3 of the shots.
        assertThat(jammedRate.current() / jammedRate.base()).isCloseTo(1f / 1.3f, within(0.02f));
        assertThat(fireRate(tower).current()).isEqualTo(fireRate(tower).base());
    }

    private static TowerStatLine fireRate(Tower tower) {
        return tower.inspect().stats().stream().filter(line -> line.stat() == TowerStat.FIRE_RATE).findFirst().orElseThrow();
    }

    private static final EnemyDefinition WALKER_A = EnemyDefinition.of("walkerA", "Walker A", 100, 1, 2f,
            BodyArchetype.CIRCLE);
    private static final EnemyDefinition WALKER_B = EnemyDefinition.of("walkerB", "Walker B", 100, 1, 2f,
            BodyArchetype.CIRCLE);

    /** Walker A leads Walker B by 20 px along the path. */
    private static EnemyMob[] twoWalkers(GameEngine engine) {
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100).withCustomEnemies(List.of(WALKER_A, WALKER_B)));
        GameWorld world = engine.getGameWorld();
        EnemyMob a = world.getEnemyCatalog().spawn("walkerA", world, 0, 100, 1, Rank.GRUNT);
        world.enemies().add(a);
        for (int t = 1; t <= 10; t++) {
            engine.doTick(t);
        }
        EnemyMob b = world.getEnemyCatalog().spawn("walkerB", world, 0, 100, 1, Rank.GRUNT);
        world.enemies().add(b);
        return new EnemyMob[] {a, b};
    }

    private static Optional<EnemyInspection> clickAndInspect(GameEngine engine, double x, double y) {
        engine.requestEnemySelectionAt((int) Math.round(x), (int) Math.round(y));
        return engine.inspectSelectedEnemy();
    }

    @Test
    void clickingBetweenTwoEnemiesSelectsTheNearerOne() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyMob[] walkers = twoWalkers(engine);

        Optional<EnemyInspection> picked = clickAndInspect(engine, walkers[1].getX() + 4, walkers[1].getY());

        assertThat(picked).map(EnemyInspection::name).contains("Walker B");
    }

    @Test
    void aClickOutsideTheGenerousRadiusSelectsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyMob[] walkers = twoWalkers(engine);

        Optional<EnemyInspection> near = clickAndInspect(engine, walkers[0].getX(), walkers[0].getY() + 11);
        Optional<EnemyInspection> far = clickAndInspect(engine, walkers[0].getX(), walkers[0].getY() + 14);

        assertThat(near).map(EnemyInspection::name).contains("Walker A");
        assertThat(far).isEmpty();
    }

    @Test
    void anInvisibleEnemyCannotBePickedButAnExistingSelectionSurvivesItTurningInvisible() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyMob[] walkers = twoWalkers(engine);
        walkers[0].applyEffect(Effect.invisible(100, d -> {
        }));
        Optional<EnemyInspection> hidden = clickAndInspect(engine, walkers[0].getX(), walkers[0].getY());
        clickAndInspect(engine, walkers[1].getX(), walkers[1].getY());

        walkers[1].applyEffect(Effect.invisible(100, d -> {
        }));

        assertThat(hidden).isEmpty();
        assertThat(engine.inspectSelectedEnemy()).map(EnemyInspection::name).contains("Walker B");
    }

    @Test
    void aKilledEnemyKeepsItsLastSnapshotMarkedKilled() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyMob[] walkers = twoWalkers(engine);
        clickAndInspect(engine, walkers[0].getX(), walkers[0].getY());

        walkers[0].doDamage(Damage.physical(1_000_000));
        for (int t = 11; t <= 200; t++) {
            engine.doTick(t);
        }

        assertThat(engine.inspectSelectedEnemy()).map(EnemyInspection::fate).contains(EnemyInspection.Fate.KILLED);
    }

    @Test
    void aLeakedEnemyKeepsItsLastSnapshotMarkedLeaked() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyMob[] walkers = twoWalkers(engine);
        clickAndInspect(engine, walkers[0].getX(), walkers[0].getY());

        for (int t = 11; t <= 400 && !walkers[0].isDead(); t++) {
            engine.doTick(t);
        }

        assertThat(engine.inspectSelectedEnemy()).map(EnemyInspection::fate).contains(EnemyInspection.Fate.LEAKED);
    }

    @Test
    void anEnemyReplacedWhileAliveIsDeselected() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyMob[] walkers = twoWalkers(engine);
        clickAndInspect(engine, walkers[0].getX(), walkers[0].getY());
        GameWorld world = engine.getGameWorld();

        world.enemies().replace(walkers[0], world.getEnemyCatalog().spawn("walkerB", world, 0, 100, 1, Rank.GRUNT));

        assertThat(engine.inspectSelectedEnemy()).isEmpty();
    }

    @Test
    void loadingALevelClearsTheEnemySelection() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyMob[] walkers = twoWalkers(engine);
        clickAndInspect(engine, walkers[0].getX(), walkers[0].getY());

        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        assertThat(engine.inspectSelectedEnemy()).isEmpty();
    }

    @Test
    void loadingALevelReplacesEveryPartOfTheWorldInOneVisibleStep() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();

        engine.loadLevel(LevelFixtures.biggerLevelWith(List.of(), 50));

        LoadedLevel installed = engine.getGameWorld().level();
        assertThat(installed.cells().width()).isEqualTo(20);
        assertThat(installed.board().maxX()).isEqualTo(20 * BoardFixtures.SCALE - 1);
        assertThat(installed.waveCount()).isZero();
        assertThat(installed.pathAt(0).points()).isNotEmpty();
        assertThat(engine.getCurrentWaveIndex()).isZero();
    }

    @Test
    void damageThatLandsIsTalliedByType() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        EnemyMob enemy = engine.getGameWorld().enemies().getEnemies()[0];

        Damage landed = enemy.doDamage(Damage.physical(10));

        DamageMix mix = engine.getGameWorld().damageTally().mix();
        assertThat(mix.physical()).isEqualTo(landed.amount()).isPositive();
        assertThat(mix.magic()).isZero();
    }

    @Test
    void anEliteSpawnedAfterMostlyPhysicalDamageResistsPhysicalButNotMagic() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        engine.getGameWorld().enemies().getEnemies()[0].doDamage(Damage.physical(10));
        GameWorld world = engine.getGameWorld();

        EnemyMob elite = world.getEnemyCatalog().spawn("c", world, 0, 100_000, 1, Rank.ELITE);

        assertThat(elite.doDamage(Damage.physical(1000)).amount()).isLessThan(1000);
        assertThat(elite.doDamage(Damage.magic(1000)).amount()).isEqualTo(1000);
    }

    @Test
    void loadingALevelStartsANewDamageTally() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        engine.getGameWorld().enemies().getEnemies()[0].doDamage(Damage.magic(10));

        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        assertThat(engine.getGameWorld().damageTally().mix()).isEqualTo(DamageMix.none());
    }

    @Test
    void loadingALevelRegistersItsOwnCustomEnemiesSoItsWavesCanSpawnThem() {
        EnemyDefinition tankySquare = EnemyDefinition.of("tankySquare", "Tanky Square", 100, 5, 1.28f,
                BodyArchetype.SQUARE);
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("tankySquare", Rank.GRUNT)), 100)
                .withCustomEnemies(List.of(tankySquare)));

        assertThat(engine.getGameWorld().getEnemyCatalog().ids()).contains("tankySquare");
        assertThat(engine.nextWave()).isTrue();
        assertThat(engine.getGameWorld().enemies().getEnemies()).hasSize(1);
    }

    @Test
    void waveProgressReportsAnIndexAndCountThatBelongToTheSameLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(
                new WaveDefinition("c", Rank.GRUNT),
                new WaveDefinition("s", Rank.GRUNT)), 100));

        engine.nextWave();

        WaveProgress progress = engine.waveProgress();
        assertThat(progress.index()).isEqualTo(1);
        assertThat(progress.count()).isEqualTo(2);
        assertThat(progress.hasNextWave()).isTrue();
        assertThat(progress.current()).isNotEmpty();
        assertThat(progress.next()).isNotEmpty();
    }

    @Test
    void waveProgressOnTheLastWaveOffersNoNextWave() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));

        engine.nextWave();

        WaveProgress progress = engine.waveProgress();
        assertThat(progress.hasNextWave()).isFalse();
        assertThat(progress.next()).isEmpty();
        assertThat(progress.current()).isNotEmpty();
    }

    @Test
    void waveProgressSurvivesALevelWithFewerWavesThanTheOneBeforeIt() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(
                new WaveDefinition("c", Rank.GRUNT),
                new WaveDefinition("s", Rank.GRUNT)), 100));
        engine.nextWave();

        engine.getGameWorld().installLevel(engine.getGameWorld().level().withCatalog(
                engine.getGameWorld().getEnemyCatalog()));

        assertThatCode(engine::waveProgress).doesNotThrowAnyException();
    }

    @Test
    void placingATowerOnABuildableCellChargesCreditsAndOccupiesTheCell() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        Optional<Tower> selected = engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        assertThat(selected).isEmpty(); // placing doesn't "select" the newly-built tower
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(100 - SniperTower.PRICE);
        assertThat(engine.cells().at(0, 0).hasTower()).isTrue();
        assertThat(engine.isPlacingTower()).isFalse();
    }

    @Test
    void placingATowerWithoutEnoughCreditsCancelsPlacementWithoutBuildingOrCharging() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), SniperTower.PRICE - 1));

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        Optional<Tower> selected = engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        assertThat(selected).isEmpty();
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(SniperTower.PRICE - 1);
        assertThat(engine.cells().at(0, 0).hasTower()).isFalse();
        assertThat(engine.isPlacingTower()).isFalse(); // failed payment still cancels placement mode
    }

    @Test
    void clickingAnOccupiedCellSelectsItsTower() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        Optional<Tower> selected = engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        assertThat(selected).get().extracting(Tower::getType).isEqualTo(TowerFactory.Type.SNIPER);
    }

    @Test
    void buyUpgradeForSelectedBuysTheNthCurrentlyOfferedNodeOfTheSelectedTower() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));
        Tower tower = engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0))
                .orElseThrow();
        tower.setSelected(true);
        int creditsBeforeUpgrade = engine.getGameWorld().economy().getCredits();

        engine.buyUpgradeForSelected(1);

        assertThat(engine.getGameWorld().economy().getCredits()).isLessThan(creditsBeforeUpgrade);
        assertThat(tower.upgrades().owns("base.range")).isTrue();
    }

    @Test
    void buyUpgradeForSelectedWithNoTowerSelectedSpendsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));
        int creditsBeforeUpgrade = engine.getGameWorld().economy().getCredits();

        engine.buyUpgradeForSelected(1);

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsBeforeUpgrade);
    }

    @Test
    void buyUpgradeForSelectedWithAnOutOfRangeNumberSpendsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));
        Tower tower = engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0))
                .orElseThrow();
        tower.setSelected(true);
        int creditsBeforeUpgrade = engine.getGameWorld().economy().getCredits();

        engine.buyUpgradeForSelected(99);

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsBeforeUpgrade);
    }

    @Test
    void placingOnAPathCellIsRejectedAndCostsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(2));

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(100);
        assertThat(engine.cells().at(0, 2).hasTower()).isFalse();
    }

    @Test
    void towerKillsInRangeEnemyCreditsThePlayerAndReArmsTheWave() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        // A second wave, so clearing the first means "next wave ready", not "won".
        engine.loadLevel(LevelFixtures.levelWith(
                List.of(new WaveDefinition("weakling", Rank.GRUNT),
                        new WaveDefinition("weakling", Rank.GRUNT)), 100)
                .withCustomEnemies(List.of(WEAKLING)));

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1));
        int creditsAfterBuild = engine.getGameWorld().economy().getCredits();
        int scoreBefore = engine.getGameWorld().economy().getScore();

        assertThat(engine.nextWave()).isTrue();
        assertThat(engine.isWaveReady()).isFalse();

        for (int t = 1; t <= 10 && engine.getGameWorld().economy().getScore() == scoreBefore; t++) {
            engine.doTick(t);
        }

        assertThat(engine.getGameWorld().economy().getScore()).isEqualTo(scoreBefore + 7);
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsAfterBuild + 7);
        assertThat(engine.isWaveReady()).isTrue(); // 2nd wave exists -> ready to start it
    }

    @Test
    void aSwarmSlotIsNotClearedUntilEveryMemberDies() {
        // A swarm of 3 must cost 3 kills to clear, not 1.
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(
                List.of(new WaveDefinition("swarm 3 weakling", Rank.GRUNT),
                        new WaveDefinition("weakling", Rank.GRUNT)), 100)
                .withCustomEnemies(List.of(WEAKLING)));

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1));
        int scoreBefore = engine.getGameWorld().economy().getScore();

        assertThat(engine.nextWave()).isTrue();
        assertThat(engine.isWaveReady()).isFalse();

        int t = 0;
        for (; t <= 60 && engine.getGameWorld().economy().getScore() == scoreBefore; t++) {
            engine.doTick(t);
        }

        assertThat(engine.getGameWorld().economy().getScore()).isGreaterThan(scoreBefore);
        assertThat(engine.isWaveReady()).isFalse();

        for (; t <= 300 && !engine.isWaveReady(); t++) {
            engine.doTick(t);
        }

        // member bounty shares sum to exactly one normal spawn's bounty
        assertThat(engine.getGameWorld().economy().getScore()).isEqualTo(scoreBefore + 7);
        assertThat(engine.isWaveReady()).isTrue();
    }

    @Test
    void startingARoundSpawnsBothPathsWavesInOneCall() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.twoPathLevelWith(
                List.of(new WaveDefinition("c", Rank.GRUNT)),
                List.of(new WaveDefinition("2 c", Rank.GRUNT))));

        assertThat(engine.nextWave()).isTrue();

        assertThat(engine.getGameWorld().enemies().getEnemies()).hasSize(3);
    }

    @Test
    void aRoundDoesNotClearUntilEveryPathsEnemiesAreGone() {
        // Only path A's enemy is in range; the round must wait for path B's to leak.
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.twoPathLevelWith(
                List.of(new WaveDefinition("c", Rank.GRUNT), new WaveDefinition("c", Rank.GRUNT)),
                List.of(new WaveDefinition("c", Rank.GRUNT), new WaveDefinition("c", Rank.GRUNT))));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1)); // in range of path A's row (y=2) only
        int scoreBefore = engine.getGameWorld().economy().getScore();

        assertThat(engine.nextWave()).isTrue();

        int t = 0;
        for (; t <= 60 && engine.getGameWorld().economy().getScore() == scoreBefore; t++) {
            engine.doTick(t);
        }

        assertThat(engine.getGameWorld().economy().getScore()).isGreaterThan(scoreBefore);
        assertThat(engine.isWaveReady()).isFalse();

        for (; t <= 400 && !engine.isWaveReady(); t++) {
            engine.doTick(t);
        }

        assertThat(engine.isWaveReady()).isTrue();
    }

    @Test
    void buildabilityUnionsEveryPathNotJustTheFirst() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.twoPathLevelWith(List.of(), List.of()));

        // Only path B's coverage makes this cell unbuildable.
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        Optional<Tower> selected = engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(5));

        assertThat(selected).isEmpty();
        assertThat(engine.cells().at(2, 5).hasTower()).isFalse();
    }

    @Test
    void aMortarShellTravelsThenSplashesAnInRangeEnemyCreditingThePlayer() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(
                List.of(new WaveDefinition("weakling", Rank.GRUNT),
                        new WaveDefinition("weakling", Rank.GRUNT)), 100)
                .withCustomEnemies(List.of(WEAKLING)));

        engine.startPlacing(TowerFactory.Type.MORTAR, MortarTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1));
        int creditsAfterBuild = engine.getGameWorld().economy().getCredits();
        int scoreBefore = engine.getGameWorld().economy().getScore();

        assertThat(engine.nextWave()).isTrue();

        for (int t = 1; t <= 20 && engine.getGameWorld().economy().getScore() == scoreBefore; t++) {
            engine.doTick(t);
        }

        assertThat(engine.getGameWorld().economy().getScore()).isEqualTo(scoreBefore + 7);
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsAfterBuild + 7);
        assertThat(engine.getGameWorld().projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void enemyReachingTheEndOfThePathCostsALife() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        int initialLives = engine.getGameWorld().economy().getLives();

        engine.nextWave();
        for (int t = 1; t <= 200; t++) {
            engine.doTick(t);
        }

        assertThat(engine.getGameWorld().economy().getLives()).isLessThan(initialLives);
    }

    @Test
    void sellingATowerRefundsSeventyFivePercentAndClearsTheCell() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));
        int creditsAfterBuild = engine.getGameWorld().economy().getCredits();
        Tower placed = engine.cells().at(0, 0).getTower();

        engine.getGameWorld().towers().sell(placed);

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsAfterBuild + placed.getSellPrice());
        assertThat(engine.cells().at(0, 0).hasTower()).isFalse();
        assertThat(engine.cells().at(0, 0).buildable()).isTrue();
    }

    @Test
    void eachCopyOfATowerAlreadyOnTheBoardRaisesTheNextOnesPriceByFifteenPercent() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        placeSniper(engine, 0, 0);
        placeSniper(engine, 1, 0);
        placeSniper(engine, 2, 0);

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(100 - 10 - 12 - 13);
        assertThat(engine.getGameWorld().towers().priceOf(TowerFactory.Type.SNIPER)).isEqualTo(15);
        assertThat(engine.getGameWorld().towers().priceOf(TowerFactory.Type.SPLASH)).isEqualTo(SplashTower.PRICE);
    }

    @Test
    void sellingATowerRefundsThreeQuartersOfThePriceItWasBoughtFor() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        placeSniper(engine, 0, 0);
        placeSniper(engine, 1, 0);
        int creditsAfterBuilds = engine.getGameWorld().economy().getCredits();

        engine.getGameWorld().towers().sell(engine.cells().at(1, 0).getTower());

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsAfterBuilds + 9);
    }

    @Test
    void sellingTheCheapCopyAndRebuyingItCostsTheRisenPrice() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        placeSniper(engine, 0, 0);
        placeSniper(engine, 1, 0);
        engine.getGameWorld().towers().sell(engine.cells().at(0, 0).getTower());
        int creditsAfterSale = engine.getGameWorld().economy().getCredits();

        placeSniper(engine, 0, 0);

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsAfterSale - 12);
        assertThat(creditsAfterSale).isEqualTo(100 - 10 - 12 + 8);
    }

    @Test
    void aLaterCopysUpgradesCostWhatTheFirstCopysDo() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        placeSniper(engine, 0, 0);
        Tower secondCopy = placeSniper(engine, 1, 0);
        secondCopy.setSelected(true);
        int creditsBeforeUpgrade = engine.getGameWorld().economy().getCredits();

        engine.buyUpgradeForSelected(1);

        assertThat(secondCopy.upgrades().owns(StandardBaseSlot.RANGE_ID)).isTrue();
        assertThat(engine.getGameWorld().economy().getCredits())
                .isEqualTo(creditsBeforeUpgrade - UpgradeTier.RANGE_1.price(SniperTower.PRICE));
    }

    @Test
    void reloadingALevelRemovesTowersLeftFromThePreviousLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));
        assertThat(engine.cells().at(0, 0).hasTower()).isTrue();

        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        assertThat(engine.getGameWorld().towers().all()).isEmpty();
        assertThat(engine.cells().at(0, 0).hasTower()).isFalse();
    }

    @Test
    void reloadingALevelClearsEnemiesStillAliveFromThePreviousLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        assertThat(engine.getGameWorld().enemies().getEnemies()).isNotEmpty();

        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        assertThat(engine.getGameWorld().enemies().getEnemies()).isEmpty();
    }

    @Test
    void reloadingALevelClearsProjectilesStillInFlightFromThePreviousLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.getGameWorld().projectiles().add(new CannonballProjectile(0, 0, 1000, 0, 1f, (x, y) -> {
        }));
        assertThat(engine.getGameWorld().projectiles().getProjectiles()).isNotEmpty();

        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        assertThat(engine.getGameWorld().projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void aProjectileAdvancesOnATickBetweenEnemiesAndTowersMoving() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.getGameWorld().projectiles().add(new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        }));

        engine.doTick(1);

        assertThat(engine.getGameWorld().projectiles().getProjectiles().getFirst().getX()).isEqualTo(10.0);
    }

    @Test
    void reloadingALevelReseedsCreditsAndLivesFromTheNewLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        LevelDefinition next = LevelDefinition.unsmoothed("Next", "", 5, 5, LevelFixtures.STRAIGHT_PATH, List.of(), 75, 3);
        engine.loadLevel(next);

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(75);
        assertThat(engine.getGameWorld().economy().getLives()).isEqualTo(3);
    }

    @Test
    void reloadingALevelRewindsTheWaveCounterAndReArmsTheFirstWave() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);

        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));

        assertThat(engine.getCurrentWaveIndex()).isEqualTo(0);
        assertThat(engine.isWaveReady()).isTrue();
    }

    @Test
    void aWaveRequestedButNotYetStartedDoesNotCarryIntoTheNextLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.requestNextWave();

        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));

        assertThat(engine.doTick(1)).isFalse();
        assertThat(engine.getCurrentWaveIndex()).isZero();
    }

    @Test
    void loadingASmallerLevelWithACellHighlightedFromTheBiggerOneDoesNotCrash() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.biggerLevelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.highlightCell(BoardFixtures.cellCenter(18), BoardFixtures.cellCenter(14));

        assertThatCode(() -> engine.loadLevel(LevelFixtures.levelWith(List.of(), 100)))
                .doesNotThrowAnyException();
        assertThatCode(() -> engine.doTick(1)).doesNotThrowAnyException();
    }

    @Test
    void loadingASmallerLevelClearsTowersAgainstTheOldBoardRatherThanTheNewOne() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.biggerLevelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(18), BoardFixtures.cellCenter(14));
        assertThat(engine.cells().at(18, 14).hasTower()).isTrue();

        assertThatCode(() -> engine.loadLevel(LevelFixtures.levelWith(List.of(), 100)))
                .doesNotThrowAnyException();
    }

    @Test
    void debugSkippingAWaveClearsItAndStartsTheNextOneImmediately() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(
                List.of(new WaveDefinition("c", Rank.GRUNT),
                        new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);

        assertThat(engine.debugSkipCurrentWave()).isTrue();

        assertThat(engine.getCurrentWaveIndex()).isEqualTo(2);
        assertThat(engine.getGameWorld().enemies().getEnemies()).isNotEmpty(); // the 2nd wave's own enemies
        assertThat(engine.isWaveReady()).isFalse();
    }

    @Test
    void debugSkippingTheFinalWaveClearsTheBoardAndWinsTheLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        assertThat(engine.getGameWorld().enemies().getEnemies()).isNotEmpty();

        assertThat(engine.debugSkipCurrentWave()).isFalse();

        assertThat(engine.getGameWorld().enemies().getEnemies()).isEmpty();
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);
        assertThat(engine.outcome()).isEqualTo(LevelOutcome.WON);
    }

    @Test
    void debugSkippingAWaveCostsNoLivesAndPaysNoCredits() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        int livesBefore = engine.getGameWorld().economy().getLives();
        int creditsBefore = engine.getGameWorld().economy().getCredits();

        engine.debugSkipCurrentWave();

        assertThat(engine.getGameWorld().economy().getLives()).isEqualTo(livesBefore);
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsBefore);
    }

    @Test
    void debugSpawnCyclesThroughEveryCatalogIdInOrderThenWraps() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        List<String> ids = engine.getGameWorld().getEnemyCatalog().ids();

        for (String expected : ids) {
            assertThat(engine.debugSpawnNextCatalogEnemy()).contains(expected);
        }
        assertThat(engine.debugSpawnNextCatalogEnemy()).contains(ids.getFirst()); // wraps around
    }

    @Test
    void debugSpawnAddsALiveEnemyToTheRosterImmediately() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        engine.debugSpawnNextCatalogEnemy();

        assertThat(engine.getGameWorld().enemies().getEnemies()).hasSize(1);
    }

    @Test
    void debugSpawnWithNoLevelLoadedSpawnsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();

        assertThat(engine.debugSpawnNextCatalogEnemy()).isEmpty();
    }

    @Test
    void debugGrantingCreditsRaisesTheBalanceByExactlyTheAmount() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        engine.debugGrantCredits(250);

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(350);
    }

    @Test
    void killingTheLastWavesLastEnemyWinsTheLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("weakling", Rank.GRUNT)), 100)
                .withCustomEnemies(List.of(WEAKLING)));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1));
        int livesBefore = engine.getGameWorld().economy().getLives();

        engine.nextWave();
        tickUntilOver(engine, 60);

        assertThat(engine.outcome()).isEqualTo(LevelOutcome.WON);
        assertThat(engine.getGameWorld().economy().getLives()).isEqualTo(livesBefore);
    }

    @Test
    void killingTheLastWavesLastEnemyDoesNotWinWhileItsOnDeathSpawnIsStillToCome() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        EnemyDefinition mother = EnemyDefinition.of("mother", "Mother", 1, 7, 1.28f, BodyArchetype.CIRCLE)
                .withAbilities(EnemyFixtures.definitionThatSpawns("mother", "child").abilities());
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("mother", Rank.GRUNT)), 100)
                .withCustomEnemies(List.of(mother, EnemyFixtures.simpleDefinition("child"))));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1));
        engine.nextWave();
        EnemyMob killed = engine.getGameWorld().enemies().getEnemies()[0];

        for (int t = 1; t < 60 && !killed.isDead(); t++) {
            engine.doTick(t);
        }

        assertThat(killed.isDead()).isTrue();
        assertThat(engine.outcome()).isEqualTo(LevelOutcome.PLAYING);
        assertThat(engine.getGameWorld().enemies().aliveCount()).isEqualTo(1);
    }

    @Test
    void clearingAWaveThatIsNotTheLastReArmsTheNextInsteadOfWinning() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(
                List.of(new WaveDefinition("c", Rank.GRUNT), new WaveDefinition("c", Rank.GRUNT)), 100));

        engine.nextWave();
        for (int t = 1; t <= 200 && !engine.isWaveReady(); t++) {
            engine.doTick(t);
        }

        assertThat(engine.isWaveReady()).isTrue();
        assertThat(engine.outcome()).isEqualTo(LevelOutcome.PLAYING);
    }

    @Test
    void losingTheLastLifeLosesTheLevelBeforeItsLastWave() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(
                List.of(new WaveDefinition("c", Rank.GRUNT), new WaveDefinition("c", Rank.GRUNT)), 100)
                .withStartingLives(1));

        engine.nextWave();
        tickUntilOver(engine, 200);

        assertThat(engine.outcome()).isEqualTo(LevelOutcome.LOST);
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);
    }

    @Test
    void theLastEnemyLeakingTheLastLifeLosesRatherThanWins() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100)
                .withStartingLives(1));

        engine.nextWave();
        tickUntilOver(engine, 200);

        assertThat(engine.outcome()).isEqualTo(LevelOutcome.LOST);
    }

    @Test
    void anEndedLevelIgnoresTicksWavesPlacementAndUpgrades() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(
                List.of(new WaveDefinition("c", Rank.GRUNT), new WaveDefinition("c", Rank.GRUNT)), 100)
                .withStartingLives(1));
        engine.nextWave();
        int lastTick = tickUntilOver(engine, 200);
        int creditsAtEnd = engine.getGameWorld().economy().getCredits();

        engine.requestNextWave();
        boolean waveStarted = engine.doTick(lastTick + 1) || engine.nextWave();
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1));

        assertThat(waveStarted).isFalse();
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);
        assertThat(engine.cells().at(2, 1).hasTower()).isFalse();
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsAtEnd);
    }

    @Test
    void reloadingAnEndedLevelPlaysAgain() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        LevelDefinition level = LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100);
        engine.loadLevel(level);
        engine.nextWave();
        engine.debugSkipCurrentWave();

        engine.loadLevel(level);

        assertThat(engine.outcome()).isEqualTo(LevelOutcome.PLAYING);
        assertThat(engine.nextWave()).isTrue();
    }

    private static Tower placeSniper(GameEngine engine, int cellX, int cellY) {
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(cellX), BoardFixtures.cellCenter(cellY));
        return engine.cells().at(cellX, cellY).getTower();
    }
}
