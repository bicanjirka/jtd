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
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.tower.pulse.FieldMode;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PulseTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();
    private final GameWorld board = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    @Test
    void theFieldHitsEveryEnemyInRangeForMagicEveryTickHiddenOnesIncluded() {
        PulseTower tower = new PulseTower(this.context, 0, 0);
        FakeEnemyMob visible = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{visible, hidden});

        tower.doTick(0);
        tower.doTick(1);

        Damage field = Damage.magic(tower.damageCurrent());
        assertThat(visible.hits()).containsExactly(field, field);
        assertThat(hidden.hits()).containsExactly(field, field);
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
        UpgradePaths.buy(tower, this.context, "Attune");
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
    void attunedAnEnemyThatStaysEarnsTollOverTimeUpToTheFieldsCap() {
        PulseTower tower = this.attunedTower();
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(0);
        int atOnce = enemy.effectStacks(EffectKind.TOLL);
        for (int t = 1; t < 600; t++) {
            tower.doTick(t);
        }

        assertThat(atOnce).isZero();
        assertThat(enemy.effectStacks(EffectKind.TOLL)).isEqualTo(tollCap(tower));
    }

    @Test
    void aHigherTollCapLetsTollBuildFurtherAndAFasterRateBuildsItSooner() {
        PulseTower plain = this.attunedTower();
        PulseTower tuned = TowerFixtures.buffed(this.attunedTower(), this.context,
                TowerModifiers.tollCap(2).plus(TowerModifiers.tollRate(1f)));
        FakeEnemyMob atPlain = FakeEnemyMob.at(plain.getX(), plain.getY());
        FakeEnemyMob atTuned = FakeEnemyMob.at(tuned.getX(), tuned.getY());

        int plainFirst = ticksToFirstToll(plain, atPlain);
        int tunedFirst = ticksToFirstToll(tuned, atTuned);
        for (int t = 600; t < 1200; t++) {
            this.context.enemies().setEnemies(new EnemyMob[]{atTuned});
            tuned.doTick(t);
        }

        assertThat(tunedFirst).isLessThan(plainFirst);
        assertThat(atTuned.effectStacks(EffectKind.TOLL)).isEqualTo(tollCap(tuned)).isGreaterThan(tollCap(plain));
    }

    /** The ticks {@code tower}'s field takes to put a first stack of Toll on {@code enemy}, standing alone in it. */
    private int ticksToFirstToll(PulseTower tower, FakeEnemyMob enemy) {
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});
        int t = 0;
        while (enemy.effectStacks(EffectKind.TOLL) == 0 && t < 5000) {
            tower.doTick(t++);
        }
        return t;
    }

    @Test
    void tollLeavesTheFieldsDamageAlone() {
        PulseTower tower = this.attunedTower();
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 0; t < 600; t++) {
            tower.doTick(t);
        }

        assertThat(enemy.effectStacks(EffectKind.TOLL)).isPositive();
        assertThat(enemy.hits().getLast()).isEqualTo(Damage.magic(tower.damageCurrent()));
    }

    private static int tollCap(PulseTower tower) {
        return Math.round(tower.stats().value(TowerStat.TOLL_CAP));
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

        int full = lone.damageCurrent();
        assertThat(atLone.hits().getLast()).isEqualTo(Damage.magic(full));
        assertThat(atPaired.hits().getLast()).isEqualTo(Damage.magic(Math.round(full * 0.75f)));
        assertThat(atCrowded.hits().getLast()).isEqualTo(Damage.magic(Math.round(full * 0.25f)));
    }

    @Test
    void theFieldKeepsRefreshingTheTollOfAnEnemyInsideToTheSameLinger() {
        PulseTower tower = this.attunedTower();
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 0; t < 100; t++) {
            tower.doTick(t);
        }

        List<Effect> refreshes = tollOn(enemy);
        assertThat(refreshes).hasSizeGreaterThan(1);
        assertThat(refreshes).extracting(Effect::remainingTicks).containsOnly(refreshes.getFirst().remainingTicks());
        assertThat(tower.getHighestToll()).isEqualTo(enemy.effectStacks(EffectKind.TOLL));
    }

    @Test
    void timeAtFullTollInsideCountsAsDeedsAndTimeBeforeItNone() {
        PulseTower tower = this.attunedTower();
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        int t = 0;
        for (; enemy.effectStacks(EffectKind.TOLL) < tollCap(tower) - 1; t++) {
            tower.beginTick(t);
            tower.doTick(t);
        }
        int beforeFull = tower.experience().deeds();
        for (int end = t + 100; t < end; t++) {
            tower.beginTick(t);
            tower.doTick(t);
        }

        assertThat(beforeFull).isZero();
        assertThat(tower.experience().deeds()).isPositive();
    }

    /** A Pulse at cell (3, 3) of a 32-pixel board. */
    private PulseTower pulseWith(String... nodes) {
        PulseTower tower = new PulseTower(this.board, 3, 3);
        UpgradePaths.buy(tower, this.board, nodes);
        return tower;
    }

    private static List<Effect> tollOn(FakeEnemyMob enemy) {
        return enemy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.TOLL).toList();
    }

    /** {@code enemy} at the most Toll {@code tower} builds. */
    private static void fullToll(FakeEnemyMob enemy, PulseTower tower) {
        int cap = tollCap(tower);
        enemy.applyEffect(Effect.toll(cap, 100, DamageSink.none()).withStackCap(cap));
    }

    private static List<Effect> applied(FakeEnemyMob enemy, EffectKind kind) {
        return enemy.appliedEffects().stream().filter(e -> e.kind() == kind).toList();
    }

    private static boolean has(FakeEnemyMob enemy, EffectKind kind) {
        return enemy.hasEffect(kind);
    }

    @Test
    void theBaseFieldDragsEveryEnemyInsideAtItsBaseStrengthWhateverItsTollAndOnlyThose() {
        PulseTower tower = this.pulseWith();
        FakeEnemyMob fresh = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob tolled = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(tolled, tower);
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
        fullToll(tolled, tower);
        this.board.enemies().setEnemies(new EnemyMob[]{fresh, tolled});

        tower.doTick(1);

        assertThat(tolled.effectStacks(EffectKind.DRAGGED)).isGreaterThan(fresh.effectStacks(EffectKind.DRAGGED));
        assertThat(has(tolled, EffectKind.ANCHORED)).isTrue();
        assertThat(has(tolled, EffectKind.UNDERTOW)).isTrue();
    }

    @Test
    void snareDazesAnEnemyOnItsFirstTollStackOfAVisitOnly() {
        PulseTower tower = this.pulseWith("Snare");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 1; t <= 60; t++) {
            tower.doTick(t);
        }

        assertThat(applied(enemy, EffectKind.DAZED)).hasSize(1);
    }

    @Test
    void gravityWellDragsHardestAtTheCentreAndLowersWhatItGaveAsTheEnemyMovesOut() {
        PulseTower tower = this.pulseWith("Gravity Well");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});
        tower.doTick(1);
        int atCentre = enemy.effectStacks(EffectKind.DRAGGED);

        enemy.moveTo(tower.getX() + 36, tower.getY());
        tower.doTick(2);

        assertThat(atCentre).isEqualTo(DragStrength.MOST);
        assertThat(enemy.effectStacks(EffectKind.DRAGGED)).isLessThan(atCentre);
    }

    @Test
    void aFieldNeverLowersADragAStrongerFieldGave() {
        PulseTower well = this.pulseWith("Gravity Well");
        PulseTower plain = this.pulseWith();
        FakeEnemyMob enemy = FakeEnemyMob.at(well.getX(), well.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        well.doTick(1);
        plain.doTick(1);

        assertThat(enemy.effectStacks(EffectKind.DRAGGED)).isEqualTo(DragStrength.MOST);
    }

    @Test
    void stasisDazesAndStillsAnEnemyAtFullTollWhichLosesTollWhileDazed() {
        PulseTower tower = this.pulseWith("Stasis");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(enemy, tower);
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(20);
        for (int t = 21; t <= 30; t++) {
            tower.doTick(t);
        }

        assertThat(applied(enemy, EffectKind.DAZED)).hasSize(1);
        assertThat(applied(enemy, EffectKind.STILLED)).hasSize(1);
        assertThat(enemy.effectStacks(EffectKind.TOLL)).isLessThan(tollCap(tower));
    }

    @Test
    void sightRevealsEveryEnemyInsideForAMomentAfterAndNoOther() {
        PulseTower tower = this.pulseWith("Sight");
        FakeEnemyMob inside = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        FakeEnemyMob outside = FakeEnemyMob.ghostAt(tower.getX() + 300, tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside, outside});

        tower.doTick(1);

        assertThat(applied(inside, EffectKind.REVEALED)).hasSize(1);
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
    void trueSightRevealsLongerThanAFieldLingersAndUnveilsMeanwhile() {
        PulseTower tower = this.pulseWith("True Sight");
        FakeEnemyMob enemy = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(1);

        int revealed = applied(enemy, EffectKind.REVEALED).getFirst().remainingTicks();
        assertThat(revealed).isGreaterThan(FieldMode.LINGER_TICKS);
        assertThat(applied(enemy, EffectKind.UNVEILED)).singleElement().extracting(Effect::remainingTicks).isEqualTo(revealed);
    }

    @Test
    void purgeStripsTheBuffsAndShieldsOfAnEnemyOnceAsItComesIn() {
        PulseTower tower = this.pulseWith("Purge");
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
        fullToll(tolled, tower);
        this.board.enemies().setEnemies(new EnemyMob[]{fresh, tolled});

        tower.doTick(1);
        tower.doTick(2);

        assertThat(fresh.effectStacks(EffectKind.FRAIL)).isEqualTo(FieldStrength.ENTRY);
        assertThat(tolled.effectStacks(EffectKind.FRAIL)).isEqualTo(FieldStrength.FULL);
    }

    @Test
    void corrosionLowersArmorOnlyOnEnemiesInsideTheField() {
        PulseTower tower = this.pulseWith("Corrosion");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob outside = FakeEnemyMob.at(tower.getX() + 300, tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside, outside});

        tower.doTick(1);

        assertThat(applied(inside, EffectKind.CORRODED)).singleElement().extracting(Effect::remainingTicks)
                .isEqualTo(FieldMode.LINGER_TICKS);
        assertThat(has(outside, EffectKind.CORRODED)).isFalse();
    }

    @Test
    void eventHorizonWidensTheFieldATenthForEveryDeathInsideUntilTheNextWaveStarts() {
        PulseTower tower = this.pulseWith("Event Horizon");
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
        PulseTower tower = this.pulseWith("Event Horizon");
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
    void wideFieldWidensTheFieldAndMakesTollLingerLongerAfterAnEnemyLeaves() {
        PulseTower plain = this.pulseWith("Attune");
        PulseTower wide = this.pulseWith("Range III");
        FakeEnemyMob plainEnemy = FakeEnemyMob.at(plain.getX(), plain.getY());
        FakeEnemyMob wideEnemy = FakeEnemyMob.at(wide.getX(), wide.getY());
        fullToll(plainEnemy, plain);
        fullToll(wideEnemy, wide);
        this.board.enemies().setEnemies(new EnemyMob[]{plainEnemy});
        plain.doTick(1);
        this.board.enemies().setEnemies(new EnemyMob[]{wideEnemy});

        wide.doTick(1);

        assertThat(tollOn(wideEnemy).getLast().remainingTicks()).isGreaterThan(tollOn(plainEnemy).getLast().remainingTicks());
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

        assertThat(applied(boss, EffectKind.PRIORITY)).hasSize(2);
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
        assertThat(this.pulseWith("Gravity Well").hasGravityWell()).isTrue();
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
        PulseTower tower = new PulseTower(this.board, x, y);
        UpgradePaths.buy(tower, this.board, nodes);
        return tower;
    }
}
