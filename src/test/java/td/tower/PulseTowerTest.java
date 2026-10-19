package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.DragStrength;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.FieldStrength;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.pulse.FieldMode;
import td.tower.upgrade.UpgradeBonus;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PulseTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();
    private final GameWorld board = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    @Test
    void undertowIsChoosableOnceAttuneIsBoughtAndLeavesTheDamageAlone() {
        this.context.economy().startEconomy(1000, 5);
        PulseTower tower = new PulseTower(this.context, 0, 0);
        UpgradePaths.awakenVeteran(tower);
        UpgradeNode undertow = UpgradePaths.named(tower, "Undertow");

        boolean chosen = tower.buyUpgrade(undertow);

        assertThat(chosen).isTrue();
        assertThat(tower.damageCurrent()).isEqualTo(tower.damageBase);
    }


    @Test
    void theFieldHitsEveryEnemyInRangeForMagicEveryTickHiddenOnesIncluded() {
        PulseTower tower = new PulseTower(this.context, 0, 0);
        FakeEnemyMob visible = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{visible, hidden});

        tower.doTick(0);
        tower.doTick(1);

        assertThat(visible.hits()).containsExactly(Damage.magic(100), Damage.magic(100));
        assertThat(hidden.hits()).containsExactly(Damage.magic(100), Damage.magic(100));
    }

    @Test
    void theFieldFiresWithOnlyAGhostInRangeAndNotWithNobody() {
        PulseTower tower = new PulseTower(this.context, 0, 0);
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{ghost});
        tower.doTick(0);
        boolean withGhost = tower.isFiring();

        this.context.enemies().setEnemies(new EnemyMob[]{});
        tower.doTick(1);

        assertThat(withGhost).isTrue();
        assertThat(tower.isFiring()).isFalse();
    }

    @Test
    void theFieldNeverReachesAnEnemyOutsideItsRange() {
        PulseTower tower = new PulseTower(this.context, 0, 0);
        FakeEnemyMob outside = FakeEnemyMob.at(tower.getX() + 400, tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{outside});

        tower.doTick(0);

        assertThat(outside.hits()).isEmpty();
    }

    private PulseTower attunedTower() {
        PulseTower tower = new PulseTower(this.context, 0, 0);
        UpgradePaths.buy(tower, this.context);
        return tower;
    }

    @Test
    void withoutAttuneAnEnemyThatStaysBuildsNoToll() {
        PulseTower tower = new PulseTower(this.context, 0, 0);
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 0; t < 100; t++) {
            tower.doTick(t);
        }

        assertThat(enemy.hasEffect(EffectKind.TOLL)).isFalse();
    }

    @Test
    void attunedAnEnemyThatStaysEarnsOneTollStackASecondUpToFive() {
        PulseTower tower = this.attunedTower();
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 0; t < 19; t++) {
            tower.doTick(t);
        }
        int beforeTheSecond = enemy.effectStacks(EffectKind.TOLL);
        tower.doTick(19);
        int afterTheSecond = enemy.effectStacks(EffectKind.TOLL);
        for (int t = 20; t < 300; t++) {
            tower.doTick(t);
        }

        assertThat(beforeTheSecond).isZero();
        assertThat(afterTheSecond).isEqualTo(1);
        assertThat(enemy.effectStacks(EffectKind.TOLL)).isEqualTo(5);
    }

    @Test
    void tollLeavesTheFieldsDamageAlone() {
        PulseTower tower = this.attunedTower();
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 0; t < 41; t++) {
            tower.doTick(t);
        }

        assertThat(enemy.effectStacks(EffectKind.TOLL)).isEqualTo(2);
        assertThat(enemy.hits().getLast()).isEqualTo(Damage.magic(100));
    }

    @Test
    void eachOtherFieldOverlappingThisOneTakesAQuarterOffItsDamageDownToAQuarter() {
        PulseTower lone = new PulseTower(this.context, 0, 0);
        PulseTower paired = new PulseTower(this.context, 10, 10);
        PulseTower crowded = new PulseTower(this.context, 15, 15);
        List<PulseTower> all = new ArrayList<>(List.of(lone, paired, new PulseTower(this.context, 11, 10), crowded));
        for (int i = 0; i < 4; i++) {
            all.add(new PulseTower(this.context, 16 + i % 2, 15 + i / 2));
        }
        all.forEach(this.context.towers()::add);
        FakeEnemyMob atLone = FakeEnemyMob.at(lone.getX(), lone.getY());
        FakeEnemyMob atPaired = FakeEnemyMob.at(paired.getX(), paired.getY());
        FakeEnemyMob atCrowded = FakeEnemyMob.at(crowded.getX(), crowded.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{atLone, atPaired, atCrowded});
        all.forEach(tower -> tower.doTick(19));

        lone.doTick(20);
        paired.doTick(20);
        crowded.doTick(20);

        assertThat(atLone.hits().getLast()).isEqualTo(Damage.magic(100));
        assertThat(atPaired.hits().getLast()).isEqualTo(Damage.magic(75));
        assertThat(atCrowded.hits().getLast()).isEqualTo(Damage.magic(25));
    }

    @Test
    void tollFadesAfterASecondWhileTheFieldKeepsRefreshingItForAnEnemyInside() {
        PulseTower tower = this.attunedTower();
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});
        for (int t = 0; t < 40; t++) {
            tower.doTick(t);
        }

        long refreshes = enemy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.TOLL).count();

        assertThat(refreshes).isEqualTo(21);
        assertThat(enemy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.TOLL)
                .allMatch(e -> e.remainingTicks() == 20)).isTrue();
        assertThat(tower.getHighestToll()).isEqualTo(2);
    }

    @Test
    void aSecondAtFullTollInsideCountsAsADeedOnce() {
        PulseTower tower = this.attunedTower();
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 0; t < 99; t++) {
            tower.beginTick(t);
            tower.doTick(t);
        }
        int beforeFull = tower.experience().deeds();
        for (int t = 99; t < 150; t++) {
            tower.beginTick(t);
            tower.doTick(t);
        }

        assertThat(beforeFull).isZero();
        assertThat(tower.experience().deeds()).isEqualTo(3);
    }


    /** A Pulse at cell (3, 3) of a 32-pixel board, so its range is 48 px, with every gate waived. */
    private PulseTower pulseWith(String... nodes) {
        this.board.playtestRules().setUpgradeGatesIgnored(true);
        PulseTower tower = new PulseTower(this.board, 3, 3);
        UpgradePaths.buy(tower, this.board, nodes);
        return tower;
    }

    private static List<Effect> tollOn(FakeEnemyMob enemy) {
        return enemy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.TOLL).toList();
    }

    private static void fullToll(FakeEnemyMob enemy, int cap) {
        enemy.applyEffect(Effect.toll(cap, 100, DamageSink.none()).withStackCap(cap));
    }

    private static List<Effect> applied(FakeEnemyMob enemy, EffectKind kind) {
        return enemy.appliedEffects().stream().filter(e -> e.kind() == kind).toList();
    }

    private static boolean has(FakeEnemyMob enemy, EffectKind kind) {
        return enemy.hasEffect(kind);
    }

    /** The Gravity head up to Transcendent, which opens its level IV. */
    private static final String[] GRAVITY = {"Undertow", "Snare", "Event Horizon", "Transcendent"};
    /** The Ward head up to Transcendent. */
    private static final String[] WARD = {"Sight", "Null Field", "Dead Zone", "Transcendent"};

    private PulseTower pulseAfter(String[] line, String... more) {
        String[] nodes = Stream.concat(Arrays.stream(line), Arrays.stream(more)).toArray(String[]::new);
        return this.pulseWith(nodes);
    }

    @Test
    void theBaseFieldDragsEveryEnemyInsideFifteenPercentWhateverItsTollAndOnlyThose() {
        PulseTower tower = this.pulseWith();
        FakeEnemyMob fresh = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob tolled = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(tolled, 5);
        FakeEnemyMob outside = FakeEnemyMob.at(tower.getX() + 300, tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{fresh, tolled, outside});

        tower.doTick(1);

        assertThat(fresh.effectStacks(EffectKind.DRAGGED)).isEqualTo(DragStrength.BASE);
        assertThat(tolled.effectStacks(EffectKind.DRAGGED)).isEqualTo(DragStrength.BASE);
        assertThat(applied(fresh, EffectKind.DRAGGED).getFirst().remainingTicks()).isEqualTo(FieldMode.LINGER_TICKS);
        assertThat(has(outside, EffectKind.DRAGGED)).isFalse();
    }

    @Test
    void twoFieldsDragAnEnemyInsideBothNoHarderThanOne() {
        PulseTower first = this.pulseWith();
        PulseTower second = this.pulseWith();
        FakeEnemyMob enemy = FakeEnemyMob.at(first.getX(), first.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        first.doTick(1);
        second.doTick(1);

        assertThat(enemy.effectStacks(EffectKind.DRAGGED)).isEqualTo(DragStrength.BASE);
    }

    @Test
    void undertowDeepensTheDragWithTollAnchorsAndCountsAsAChill() {
        PulseTower tower = this.pulseWith("Undertow");
        FakeEnemyMob fresh = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob tolled = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(tolled, 5);
        this.board.enemies().setEnemies(new EnemyMob[]{fresh, tolled});

        tower.doTick(1);

        assertThat(fresh.effectStacks(EffectKind.DRAGGED)).isEqualTo(15);
        assertThat(tolled.effectStacks(EffectKind.DRAGGED)).isEqualTo(30);
        assertThat(has(tolled, EffectKind.ANCHORED)).isTrue();
        assertThat(has(tolled, EffectKind.UNDERTOW)).isTrue();
    }

    @Test
    void snareDazesAnEnemyForHalfASecondOnItsFirstTollStackOfAVisitOnly() {
        PulseTower tower = this.pulseWith("Undertow", "Snare");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 1; t <= 60; t++) {
            tower.doTick(t);
        }

        assertThat(applied(enemy, EffectKind.DAZED)).singleElement().extracting(Effect::remainingTicks).isEqualTo(10);
    }

    @Test
    void gravityWellDragsHardestAtTheCentreAndLowersWhatItGaveAsTheEnemyMovesOut() {
        PulseTower tower = this.pulseAfter(GRAVITY, "Gravity Well");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});
        tower.doTick(1);
        int atCentre = enemy.effectStacks(EffectKind.DRAGGED);

        enemy.moveTo(tower.getX() + 36, tower.getY());
        tower.doTick(2);

        assertThat(atCentre).isEqualTo(DragStrength.MOST);
        assertThat(enemy.effectStacks(EffectKind.DRAGGED)).isEqualTo(24);
    }

    @Test
    void aFieldNeverLowersADragAStrongerFieldGave() {
        PulseTower well = this.pulseAfter(GRAVITY, "Gravity Well");
        PulseTower plain = this.pulseWith();
        FakeEnemyMob enemy = FakeEnemyMob.at(well.getX(), well.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        well.doTick(1);
        plain.doTick(1);

        assertThat(enemy.effectStacks(EffectKind.DRAGGED)).isEqualTo(DragStrength.MOST);
    }

    @Test
    void stasisDazesAndStillsAnEnemyAtFullTollWhichLosesAStackEveryQuarterSecondWhileDazed() {
        PulseTower tower = this.pulseAfter(GRAVITY, "Stasis");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(enemy, 5);
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(20);
        for (int t = 21; t <= 30; t++) {
            tower.doTick(t);
        }

        assertThat(applied(enemy, EffectKind.DAZED)).singleElement().extracting(Effect::remainingTicks).isEqualTo(20);
        assertThat(applied(enemy, EffectKind.STILLED)).singleElement().extracting(Effect::remainingTicks).isEqualTo(20);
        assertThat(enemy.effectStacks(EffectKind.TOLL)).isEqualTo(3);
    }

    @Test
    void sightRevealsEveryEnemyInsideAndHalfASecondAfterAndNoOther() {
        PulseTower tower = this.pulseWith("Sight");
        FakeEnemyMob inside = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        FakeEnemyMob outside = FakeEnemyMob.ghostAt(tower.getX() + 300, tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside, outside});

        tower.doTick(1);

        assertThat(applied(inside, EffectKind.REVEALED)).singleElement().extracting(Effect::remainingTicks).isEqualTo(10);
        assertThat(has(inside, EffectKind.UNVEILED)).isFalse();
        assertThat(has(outside, EffectKind.REVEALED)).isFalse();
    }

    @Test
    void nullFieldSilencesEveryEnemyInsideAndOnlyThose() {
        PulseTower tower = this.pulseWith("Sight", "Null Field");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob outside = FakeEnemyMob.at(tower.getX() + 300, tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside, outside});

        tower.doTick(1);

        assertThat(applied(inside, EffectKind.SILENCED)).singleElement().extracting(Effect::remainingTicks)
                .isEqualTo(FieldMode.LINGER_TICKS);
        assertThat(outside.hasEffect(EffectKind.SILENCED)).isFalse();
    }

    @Test
    void deadZoneKeepsHealsAndShieldsFromTakingHoldInsideTheField() {
        PulseTower tower = this.pulseWith("Sight", "Null Field", "Dead Zone");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside});

        tower.doTick(1);

        assertThat(applied(inside, EffectKind.DEAD_ZONE)).singleElement().extracting(Effect::remainingTicks)
                .isEqualTo(FieldMode.LINGER_TICKS);
    }

    @Test
    void trueSightRevealsForFourSecondsAfterAndUnveilsMeanwhile() {
        PulseTower tower = this.pulseAfter(WARD, "True Sight");
        FakeEnemyMob enemy = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(1);

        assertThat(applied(enemy, EffectKind.REVEALED)).singleElement().extracting(Effect::remainingTicks).isEqualTo(80);
        assertThat(applied(enemy, EffectKind.UNVEILED)).singleElement().extracting(Effect::remainingTicks).isEqualTo(80);
    }

    @Test
    void purgeStripsTheBuffsAndShieldsOfAnEnemyOnceAsItComesIn() {
        PulseTower tower = this.pulseAfter(WARD, "Purge");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX(), tower.getY());
        inside.applyEffect(Effect.sprinting(40, DamageSink.none()));
        FakeEnemyMob outside = FakeEnemyMob.at(tower.getX() + 300, tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside, outside});

        tower.doTick(1);
        inside.applyEffect(Effect.sprinting(40, DamageSink.none()));
        tower.doTick(2);

        assertThat(has(inside, EffectKind.SPRINTING)).isTrue();
        assertThat(inside.buffDispels()).isEqualTo(1);
        assertThat(inside.dispels()).isEqualTo(1);
        assertThat(outside.buffDispels()).isZero();
    }

    @Test
    void frailtyMakesEnemiesInsideTakeMoreAtHalfOnEntryAndInFullAtFullToll() {
        PulseTower tower = this.pulseWith("Frailty");
        FakeEnemyMob fresh = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob tolled = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(tolled, 5);
        this.board.enemies().setEnemies(new EnemyMob[]{fresh, tolled});

        tower.doTick(1);
        tower.doTick(2);

        assertThat(fresh.effectStacks(EffectKind.FRAIL)).isEqualTo(FieldStrength.ENTRY);
        assertThat(tolled.effectStacks(EffectKind.FRAIL)).isEqualTo(FieldStrength.FULL);
    }

    @Test
    void corrosionLowersArmorOnlyOnEnemiesInsideTheField() {
        PulseTower tower = this.pulseWith("Frailty", "Corrosion");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob outside = FakeEnemyMob.at(tower.getX() + 300, tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside, outside});

        tower.doTick(1);

        assertThat(applied(inside, EffectKind.CORRODED)).singleElement().extracting(Effect::remainingTicks)
                .isEqualTo(FieldMode.LINGER_TICKS);
        assertThat(has(outside, EffectKind.CORRODED)).isFalse();
    }

    @Test
    void resonantCoilsMakesTollBuildTwiceAsFast() {
        PulseTower tower = this.pulseWith("Frailty", "Corrosion", "Resonant Coils");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 1; t < 10; t++) {
            tower.doTick(t);
        }
        int before = enemy.effectStacks(EffectKind.TOLL);
        tower.doTick(10);

        assertThat(before).isZero();
        assertThat(enemy.effectStacks(EffectKind.TOLL)).isEqualTo(1);
    }

    @Test
    void meltdownLetsTollStackToTenAndTheFieldsEffectsGrowWithIt() {
        PulseTower tower = this.pulseAfter(GRAVITY, "Frailty", "Corrosion", "Resonant Coils", "Meltdown");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(enemy, 10);
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(20);

        assertThat(tollOn(enemy).getLast().effectiveStackCap()).isEqualTo(10);
        assertThat(enemy.effectStacks(EffectKind.FRAIL)).isEqualTo(FieldStrength.MOST);
        assertThat(enemy.effectStacks(EffectKind.DRAGGED)).isEqualTo(45);
    }

    @Test
    void eventHorizonWidensTheFieldATenthForEveryDeathInsideUntilTheNextWaveStarts() {
        PulseTower tower = this.pulseWith("Undertow", "Snare", "Event Horizon");
        FakeEnemyMob doomed = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob survivor = FakeEnemyMob.at(tower.getX(), tower.getY() + 5);
        this.board.enemies().setEnemies(new EnemyMob[]{doomed, survivor});
        tower.doTick(1);
        doomed.invalidate();

        tower.doTick(2);
        tower.doTick(3);
        float widened = tower.getFieldRadius();
        this.board.waves().announce();
        tower.doTick(4);

        assertThat(widened).isCloseTo(tower.rangeReal() * 1.1f, within(0.01f));
        assertThat(tower.getFieldRadius()).isEqualTo(tower.rangeReal());
    }

    @Test
    void eventHorizonStopsWideningAtTwiceTheRange() {
        PulseTower tower = this.pulseWith("Undertow", "Snare", "Event Horizon");
        List<FakeEnemyMob> crowd = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            crowd.add(FakeEnemyMob.at(tower.getX(), tower.getY()));
        }
        FakeEnemyMob survivor = FakeEnemyMob.at(tower.getX(), tower.getY() + 5);
        crowd.add(survivor);
        this.board.enemies().setEnemies(crowd.toArray(new EnemyMob[0]));
        tower.doTick(1);
        crowd.stream().filter(enemy -> enemy != survivor).forEach(FakeEnemyMob::invalidate);

        tower.doTick(2);
        tower.doTick(3);

        assertThat(tower.getFieldRadius()).isCloseTo(tower.rangeReal() * 2f, within(0.01f));
    }

    @Test
    void wideFieldAddsTwentyPercentRangeAndMakesTollLastASecondLongerAfterAnEnemyLeaves() {
        PulseTower plain = this.pulseWith("Undertow", "Snare", "Event Horizon");
        PulseTower wide = this.pulseAfter(GRAVITY, "Range", "Range II", "Range III");
        FakeEnemyMob enemy = FakeEnemyMob.at(wide.getX(), wide.getY());
        fullToll(enemy, 5);
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        wide.doTick(1);

        assertThat(tollOn(enemy).getLast().remainingTicks()).isEqualTo(40);
        assertThat(wide.rangeReal()).isGreaterThan(plain.rangeReal());
    }

    @Test
    void arcDischargeCondemnsTheVisibleEnemyOfTheHighestRankInsideAStackAZapAndHitsNoHarder() {
        PulseTower tower = this.pulseWith("Arc Discharge");
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(tower.getX(), tower.getY()).ranked(Rank.BOSS);
        FakeEnemyMob boss = FakeEnemyMob.at(tower.getX(), tower.getY()).withHealth(300).ranked(Rank.BOSS);
        FakeEnemyMob healthier = FakeEnemyMob.at(tower.getX() + 10, tower.getY()).withHealth(900).ranked(Rank.ELITE);
        this.board.enemies().setEnemies(new EnemyMob[]{hidden, boss, healthier});

        tower.doTick(20);
        tower.doTick(21);
        tower.doTick(40);

        assertThat(applied(boss, EffectKind.PRIORITY)).extracting(Effect::remainingTicks).containsExactly(60, 60);
        assertThat(boss.effectStacks(EffectKind.CONDEMNED)).isEqualTo(2);
        assertThat(has(healthier, EffectKind.PRIORITY)).isFalse();
        assertThat(has(hidden, EffectKind.CONDEMNED)).isFalse();
        assertThat(boss.hits()).allMatch(Damage.magic(tower.damageCurrent())::equals);
        assertThat(tower.getZaps()).hasSize(1);
    }

    @Test
    void soulDrainCostsFifteenSpiritEachSecondAndTheFieldHitsNoHarder() {
        PulseTower tower = this.pulseWith("Soul Drain");
        FakeEnemyMob drained = FakeEnemyMob.at(tower.getX(), tower.getY());
        drained.reportSpirit(-100f);
        this.board.enemies().setEnemies(new EnemyMob[]{drained});

        tower.doTick(19);
        tower.doTick(20);

        assertThat(drained.hits().getFirst()).isEqualTo(Damage.magic(tower.damageCurrent()));
        assertThat(applied(drained, EffectKind.SICKENED)).singleElement().extracting(Effect::stacks)
                .isEqualTo(FieldMode.SOUL_DRAIN_SPIRIT_PER_SECOND);
        assertThat(has(drained, EffectKind.DEAD_ZONE)).isFalse();
    }

    @Test
    void theFieldIsColouredByItsHead() {
        assertThat(this.pulseWith().getFieldLook()).isEqualTo(PulseTower.FieldLook.PLAIN);
        assertThat(this.pulseWith("Frailty").getFieldLook()).isEqualTo(PulseTower.FieldLook.PLAIN);
        assertThat(this.pulseWith("Frailty").bites()).isTrue();
        assertThat(this.pulseWith("Undertow").getFieldLook()).isEqualTo(PulseTower.FieldLook.GRAVITY);
        assertThat(this.pulseWith("Sight").getFieldLook()).isEqualTo(PulseTower.FieldLook.WARD);
        assertThat(this.pulseAfter(GRAVITY, "Gravity Well").hasGravityWell()).isTrue();
    }

    @Test
    void aLeyLineLinksToTheObeliskWhoseBeamCrossesThePathAndGivesAnEnemyOnItBothFieldsRulesButNoDamage() {
        this.board.setPath(new PathNormal(List.of(new Vec2(0, 176), new Vec2(640, 176))));
        PulseTower caster = this.pulseWith("Frailty", "Ley Line");
        PulseTower across = this.pulseAt(3, 7, "Sight");
        PulseTower beside = this.pulseAt(7, 3);
        List.of(caster, across, beside).forEach(this.board.towers()::add);
        FakeEnemyMob onTheLine = FakeEnemyMob.ghostAt(caster.getX(), 176);
        this.board.enemies().setEnemies(new EnemyMob[]{onTheLine});

        caster.doTick(20);
        across.doTick(20);
        across.doTick(21);

        assertThat(caster.getLeyLine()).hasValueSatisfying(line -> assertThat(line.toY()).isEqualTo(across.getY()));
        assertThat(has(onTheLine, EffectKind.FRAIL)).isTrue();
        assertThat(has(onTheLine, EffectKind.REVEALED)).isTrue();
        assertThat(onTheLine.effectStacks(EffectKind.DRAGGED)).isEqualTo(DragStrength.BASE);
        assertThat(onTheLine.hits()).isEmpty();
        assertThat(has(onTheLine, EffectKind.TOLL)).isFalse();
        assertThat(caster.isLeyLineBusy()).isTrue();
    }

    @Test
    void aPinnedLeyLineLinksToTheChosenObeliskUntilPinnedAgain() {
        this.board.setPath(new PathNormal(List.of(new Vec2(0, 176), new Vec2(640, 176))));
        PulseTower caster = this.pulseWith("Ley Line");
        PulseTower across = this.pulseAt(3, 7);
        PulseTower beside = this.pulseAt(7, 3);
        List.of(caster, across, beside).forEach(this.board.towers()::add);

        boolean pinned = caster.pinLeyLine(beside);
        caster.doTick(20);
        float pinnedTo = caster.getLeyLine().orElseThrow().toX();
        caster.pinLeyLine(beside);
        caster.doTick(40);

        assertThat(pinned).isTrue();
        assertThat(pinnedTo).isEqualTo(beside.getX());
        assertThat(caster.getLeyLine().orElseThrow().toY()).isEqualTo(across.getY());
        assertThat(caster.isLeyLinePinned()).isFalse();
    }

    @Test
    void aLeyLineCannotBePinnedBeyondItsReachOrWithoutTheSpecial() {
        PulseTower caster = this.pulseWith("Ley Line");
        PulseTower plain = this.pulseWith();
        PulseTower far = this.pulseAt(15, 3);
        PulseTower near = this.pulseAt(3, 7);

        assertThat(caster.pinLeyLine(far)).isFalse();
        assertThat(plain.pinLeyLine(near)).isFalse();
        assertThat(caster.pinLeyLine(near)).isTrue();
    }

    private PulseTower pulseAt(int x, int y, String... nodes) {
        this.board.playtestRules().setUpgradeGatesIgnored(true);
        PulseTower tower = new PulseTower(this.board, x, y);
        UpgradePaths.buy(tower, this.board, nodes);
        return tower;
    }

    @Test
    void stasisSaysWhatItDoes() {
        PulseTower tower = new PulseTower(this.context, 0, 0);

        assertThat(tower.bonusesOf(UpgradePaths.named(tower, "Stasis"))).contains(new UpgradeBonus(
                "An enemy at full Toll is Dazed for 1s and Stilled: while the Daze holds it loses a Toll stack every "
                + "0.25s; a Daze that doesn't take hold costs nothing and is tried again a second later", ""));
    }
}
