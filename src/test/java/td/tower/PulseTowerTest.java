package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.pulse.FieldMode;
import td.tower.upgrade.UpgradeBonus;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PulseTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void resonantCoilsIsChoosableOnceAttuneIsBoughtAndLeavesTheDamageAlone() {
        this.context.economy().startEconomy(1000, 5);
        PulseTower tower = new PulseTower(this.context, 0, 0);
        UpgradePaths.awakenVeteran(tower);
        UpgradeNode resonantCoils = UpgradePaths.named(tower, "Resonant Coils");

        boolean chosen = tower.buyUpgrade(resonantCoils);

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
        List<PulseTower> all = new java.util.ArrayList<>(List.of(lone, paired, new PulseTower(this.context, 11, 10), crowded));
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

    private final GameWorld board = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

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
        enemy.applyEffect(Effect.toll(cap, 100, d -> {
        }).withStackCap(cap));
    }

    @Test
    void resonantCoilsMakesTollBuildTwiceAsFast() {
        PulseTower tower = this.pulseWith("Resonant Coils");
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
    void resonantCoilsIiMakesAnEnemyAtFullTollTwiceVulnerableForTwoSecondsAndNoOther() {
        PulseTower tower = this.pulseWith("Resonant Coils", "Resonant Coils II");
        FakeEnemyMob full = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(full, 5);
        FakeEnemyMob partial = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(partial, 4);
        this.board.enemies().setEnemies(new EnemyMob[]{full, partial});

        tower.doTick(1);
        tower.doTick(2);

        assertThat(full.effectStacks(EffectKind.VULNERABLE)).isEqualTo(2);
        assertThat(full.appliedEffects().stream().filter(e -> e.kind() == EffectKind.VULNERABLE).toList().getLast()
                .remainingTicks()).isEqualTo(40);
        assertThat(has(partial, EffectKind.VULNERABLE)).isFalse();
        assertThat(full.hits().getFirst()).isEqualTo(partial.hits().getFirst());
    }

    @Test
    void arcDischargeMarksTheHealthiestVisibleEnemyInsideOnceASecondForEveryTowerAndHitsNoHarder() {
        PulseTower tower = this.pulseWith("Resonant Coils", "Resonant Coils II", "Arc Discharge");
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(tower.getX(), tower.getY()).withHealth(5000);
        FakeEnemyMob healthy = FakeEnemyMob.at(tower.getX(), tower.getY()).withHealth(900);
        FakeEnemyMob weak = FakeEnemyMob.at(tower.getX() + 10, tower.getY()).withHealth(100);
        this.board.enemies().setEnemies(new EnemyMob[]{hidden, healthy, weak});

        tower.doTick(20);
        tower.doTick(21);
        tower.doTick(40);

        assertThat(healthy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.PRIORITY).count()).isEqualTo(2);
        assertThat(healthy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.EXPOSED).toList().getLast()
                .remainingTicks()).isEqualTo(60);
        assertThat(has(weak, EffectKind.PRIORITY)).isFalse();
        assertThat(has(hidden, EffectKind.PRIORITY)).isFalse();
        assertThat(healthy.hits()).allMatch(Damage.magic(tower.damageCurrent())::equals);
        assertThat(tower.getZaps()).hasSize(1);
    }

    @Test
    void meltdownLetsTollStackToTenAndTheFieldsEffectsGrowWithIt() {
        PulseTower tower = this.shapedPulse("Meltdown", "Corrosion");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(enemy, 10);
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(20);

        assertThat(tollOn(enemy).getLast().effectiveStackCap()).isEqualTo(10);
        assertThat(enemy.effectStacks(EffectKind.CORRODED)).isEqualTo(15);
    }

    @Test
    void teslaCoilChainsToThreeMoreVisibleEnemiesEvenOutsideTheFieldExposingAndDazingEveryOneItHits() {
        PulseTower tower = this.pulseWith("Resonant Coils", "Resonant Coils II", "Arc Discharge",
                "Transcendent", "Tesla Coil");
        float x = tower.getX();
        float y = tower.getY();
        FakeEnemyMob first = FakeEnemyMob.at(x, y).withHealth(900);
        FakeEnemyMob second = FakeEnemyMob.at(x + 40, y).withHealth(100);
        FakeEnemyMob third = FakeEnemyMob.at(x + 80, y);
        FakeEnemyMob fourth = FakeEnemyMob.at(x + 118, y);
        FakeEnemyMob fifth = FakeEnemyMob.at(x + 156, y);
        this.board.enemies().setEnemies(new EnemyMob[]{first, second, third, fourth, fifth});

        tower.doTick(20);

        for (FakeEnemyMob zapped : List.of(first, second, third, fourth)) {
            assertThat(has(zapped, EffectKind.EXPOSED)).isTrue();
            assertThat(zapped.appliedEffects().stream().filter(e -> e.kind() == EffectKind.DAZED).toList())
                    .singleElement().extracting(Effect::remainingTicks).isEqualTo(5);
        }
        assertThat(has(first, EffectKind.PRIORITY)).isTrue();
        assertThat(has(second, EffectKind.PRIORITY)).isFalse();
        assertThat(fifth.appliedEffects()).isEmpty();
        assertThat(tower.getZaps()).hasSize(4);
    }

    @Test
    void wideFieldAddsTwentyPercentRangeAndMakesTollLastASecondLongerAfterAnEnemyLeaves() {
        PulseTower plain = this.pulseWith("Resonant Coils", "Resonant Coils II", "Arc Discharge");
        PulseTower wide = this.pulseWith("Resonant Coils", "Resonant Coils II", "Arc Discharge", "Transcendent",
                "Range", "Range II", "Range III");
        FakeEnemyMob enemy = FakeEnemyMob.at(wide.getX(), wide.getY());
        fullToll(enemy, 5);
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        wide.doTick(1);

        assertThat(tollOn(enemy).getLast().remainingTicks()).isEqualTo(40);
        assertThat(wide.rangeReal()).isGreaterThan(plain.rangeReal());
    }

    private static long revealsOn(FakeEnemyMob enemy) {
        return enemy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.REVEALED).count();
    }

    /** The overcharged path to Transcendent, which opens the extra node's later levels. */
    private static final String[] COILS = {"Resonant Coils", "Resonant Coils II", "Arc Discharge",
        "Transcendent"};

    private PulseTower shapedPulse(String... more) {
        String[] nodes = java.util.stream.Stream.concat(java.util.Arrays.stream(COILS), java.util.Arrays.stream(more))
                .toArray(String[]::new);
        return this.pulseWith(nodes);
    }

    private static boolean has(FakeEnemyMob enemy, EffectKind kind) {
        return enemy.hasEffect(kind);
    }

    @Test
    void corrosionLowersArmorOnlyOnEnemiesInsideTheField() {
        PulseTower tower = this.shapedPulse("Corrosion");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob outside = FakeEnemyMob.at(tower.getX() + 300, tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside, outside});

        tower.doTick(1);

        assertThat(inside.appliedEffects().stream().filter(e -> e.kind() == EffectKind.CORRODED).toList())
                .singleElement().extracting(Effect::remainingTicks).isEqualTo(FieldMode.LINGER_TICKS);
        assertThat(has(outside, EffectKind.CORRODED)).isFalse();
    }

    @Test
    void theFieldsEffectsHoldAtHalfStrengthOnEntryAndFullAtFullToll() {
        PulseTower tower = this.shapedPulse("Corrosion");
        FakeEnemyMob fresh = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob tolled = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(tolled, 5);
        this.board.enemies().setEnemies(new EnemyMob[]{fresh, tolled});

        tower.doTick(1);
        tower.doTick(2);

        assertThat(fresh.effectStacks(EffectKind.CORRODED)).isEqualTo(5);
        assertThat(tolled.effectStacks(EffectKind.CORRODED)).isEqualTo(10);
    }

    /** A real enemy standing at {@code tower}, which nothing ticks, wearing a shield no test breaks. */
    private td.enemy.EnemyMob shieldedEnemyAt(PulseTower tower) {
        this.board.setPath(new td.wave.PathNormal(List.of(new td.wave.Vec2(tower.getX(), tower.getY()),
                new td.wave.Vec2(tower.getX() + 900, tower.getY()))));
        td.enemy.EnemyMob enemy = EnemyFactory.getEnemy("c", this.board, 0, 1_000_000, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.shield(1_000_000f, 10_000, d -> {
        }));
        return enemy;
    }

    @Test
    void mirrorFieldReturnsTheWholeOfWhatAShieldTookOfTheFieldsOwnHit() {
        PulseTower tower = this.shapedPulse("Corrosion", "Mirror Field");
        td.enemy.EnemyMob shielded = this.shieldedEnemyAt(tower);
        this.board.enemies().setEnemies(new EnemyMob[]{shielded});
        int before = shielded.getHealth();

        tower.doTick(1);

        assertThat(before - shielded.getHealth()).isCloseTo(tower.damageCurrent(), org.assertj.core.api.Assertions.within(1));
    }

    @Test
    void mirrorFieldReturnsWhatAShieldTookOfAnotherSourcesHitOnAnEnemyInsideAndNoOther() {
        PulseTower tower = this.shapedPulse("Corrosion", "Mirror Field");
        td.enemy.EnemyMob inside = this.shieldedEnemyAt(tower);
        this.board.enemies().setEnemies(new EnemyMob[]{inside});
        tower.doTick(1);
        int before = inside.getHealth();
        int hit = td.damage.DamageUnits.ofPoints(100);

        inside.doDamage(Damage.magic(hit));

        assertThat(before - inside.getHealth()).isCloseTo(hit, org.assertj.core.api.Assertions.within(1));
    }

    @Test
    void mirrorFieldLeavesAShieldedEnemyOutsideTheFieldAlone() {
        PulseTower tower = this.shapedPulse("Corrosion", "Mirror Field");
        td.enemy.EnemyMob inside = this.shieldedEnemyAt(tower);
        this.board.enemies().setEnemies(new EnemyMob[]{inside});
        tower.doTick(1);
        this.board.enemies().setEnemies(new EnemyMob[]{});
        tower.doTick(2);
        int before = inside.getHealth();
        int hit = td.damage.DamageUnits.ofPoints(100);

        inside.doDamage(Damage.magic(hit));

        assertThat(before - inside.getHealth()).isZero();
    }

    @Test
    void undertowChillsAndAnchorsEveryEnemyInside() {
        PulseTower tower = this.shapedPulse("Corrosion", "Mirror Field", "Undertow");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside});

        tower.doTick(1);

        assertThat(has(inside, EffectKind.UNDERTOW)).isTrue();
        assertThat(has(inside, EffectKind.ANCHORED)).isTrue();
    }

    @Test
    void eventHorizonWidensTheFieldATenthForEveryDeathInsideUntilTheNextWaveStarts() {
        PulseTower tower = this.shapedPulse("Corrosion", "Mirror Field", "Undertow", "Event Horizon");
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

        assertThat(widened).isCloseTo(tower.rangeReal() * 1.1f, org.assertj.core.api.Assertions.within(0.01f));
        assertThat(tower.getFieldRadius()).isEqualTo(tower.rangeReal());
    }

    @Test
    void eventHorizonStopsWideningAtTwiceTheRange() {
        PulseTower tower = this.shapedPulse("Corrosion", "Mirror Field", "Undertow", "Event Horizon");
        List<FakeEnemyMob> crowd = new java.util.ArrayList<>();
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

        assertThat(tower.getFieldRadius()).isCloseTo(tower.rangeReal() * 2f, org.assertj.core.api.Assertions.within(0.01f));
    }

    private GameWorld rolledBoard(double... rolls) {
        int[] next = {0};
        GameWorld world = WorldFixtures.newWorld(() -> rolls[Math.min(next[0]++, rolls.length - 1)]);
        world.setBoard(td.board.BoardGeometry.of(BoardFixtures.SCALE, 20, 20));
        return world;
    }

    private PulseTower rattlingPulse(GameWorld world) {
        world.playtestRules().setUpgradeGatesIgnored(true);
        PulseTower tower = new PulseTower(world, 3, 3);
        UpgradePaths.buy(tower, world, "Rattle Field");
        return tower;
    }

    @Test
    void rattleFieldAddsASunderedStackOnASuccessfulRollAndAHeadsFlip() {
        GameWorld world = this.rolledBoard(0.0, 0.0);
        PulseTower tower = this.rattlingPulse(world);
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        world.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(1);

        assertThat(enemy.effectStacks(EffectKind.SUNDERED)).isEqualTo(1);
        assertThat(has(enemy, EffectKind.EXPOSED)).isFalse();
    }

    @Test
    void rattleFieldAddsExposedOnATailsFlip() {
        GameWorld world = this.rolledBoard(0.0, 0.9);
        PulseTower tower = this.rattlingPulse(world);
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        world.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(1);

        assertThat(has(enemy, EffectKind.EXPOSED)).isTrue();
        assertThat(enemy.effectStacks(EffectKind.SUNDERED)).isZero();
    }

    @Test
    void rattleFieldAddsNothingWhenTheRollFails() {
        GameWorld world = this.rolledBoard(0.5);
        PulseTower tower = this.rattlingPulse(world);
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        world.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(1);

        assertThat(enemy.appliedEffects()).isEmpty();
    }

    @Test
    void soulDrainCostsFifteenSpiritEachSecondAndTheFieldHitsNoHarder() {
        PulseTower tower = this.pulseWith("Soul Drain");
        FakeEnemyMob drained = FakeEnemyMob.at(tower.getX(), tower.getY());
        drained.reportSpirit(-50f);
        this.board.enemies().setEnemies(new EnemyMob[]{drained});

        tower.doTick(19);
        tower.doTick(20);

        assertThat(drained.hits().getFirst()).isEqualTo(Damage.magic(tower.damageCurrent()));
        assertThat(drained.appliedEffects().stream().filter(e -> e.kind() == EffectKind.SICKENED).toList())
                .singleElement().extracting(Effect::stacks).isEqualTo(FieldMode.SOUL_DRAIN_SPIRIT_PER_SECOND);
    }

    @Test
    void atNoSpiritLeftSoulDrainKeepsHealsAndShieldsOut() {
        PulseTower tower = this.pulseWith("Soul Drain");
        FakeEnemyMob hollow = FakeEnemyMob.at(tower.getX(), tower.getY());
        hollow.reportSpirit(-100f);
        FakeEnemyMob hardy = FakeEnemyMob.at(tower.getX(), tower.getY() + 5);
        this.board.enemies().setEnemies(new EnemyMob[]{hollow, hardy});

        tower.doTick(1);

        assertThat(has(hollow, EffectKind.DEAD_ZONE)).isTrue();
        assertThat(has(hardy, EffectKind.DEAD_ZONE)).isFalse();
    }

    @Test
    void killZoneMakesEveryEnemyInsideTakeMoreFromEverySource() {
        PulseTower tower = this.pulseWith("Kill Zone");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside});

        tower.doTick(1);

        assertThat(has(inside, EffectKind.KILL_ZONE)).isTrue();
    }

    @Test
    void theFieldIsColouredByItsRules() {
        assertThat(this.pulseWith().getFieldLook()).isEqualTo(PulseTower.FieldLook.PLAIN);
        assertThat(this.shapedPulse("Corrosion").getFieldLook()).isEqualTo(PulseTower.FieldLook.CORROSION);
        assertThat(this.shapedPulse("Corrosion", "Mirror Field", "Undertow").getFieldLook())
                .isEqualTo(PulseTower.FieldLook.UNDERTOW);
        assertThat(this.pulseWith("Phase Field", "Phase Field II", "Null Field").getFieldLook())
                .isEqualTo(PulseTower.FieldLook.NULL);
    }

    @Test
    void phaseFieldAddsRangeAndMakesTollStayTwoSecondsAfterAnEnemyLeaves() {
        PulseTower plain = this.pulseWith();
        PulseTower phase = this.pulseWith("Phase Field");
        FakeEnemyMob enemy = FakeEnemyMob.at(phase.getX(), phase.getY());
        fullToll(enemy, 5);
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        phase.doTick(1);

        assertThat(tollOn(enemy).getLast().remainingTicks()).isEqualTo(40);
        assertThat(phase.rangeReal()).isGreaterThan(plain.rangeReal());
    }

    @Test
    void phaseFieldRevealsAnEnemyOnItsFirstTollStackOfAVisitAndAgainOnlyAfterItHasLeftAndComeBack() {
        PulseTower tower = this.pulseWith("Phase Field");
        FakeEnemyMob enemy = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});
        for (int t = 0; t < 19; t++) {
            tower.doTick(t);
        }
        long beforeTheStack = revealsOn(enemy);

        for (int t = 19; t < 100; t++) {
            tower.doTick(t);
        }
        long afterTheFirstVisit = revealsOn(enemy);
        this.board.enemies().setEnemies(new EnemyMob[]{});
        tower.doTick(100);
        enemy.expire(EffectKind.TOLL);
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});
        for (int t = 101; t < 200; t++) {
            tower.doTick(t);
        }

        assertThat(beforeTheStack).isZero();
        assertThat(afterTheFirstVisit).isEqualTo(1);
        assertThat(revealsOn(enemy)).isEqualTo(2);
        assertThat(enemy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.REVEALED).findFirst()
                .orElseThrow().remainingTicks()).isEqualTo(10);
        assertThat(enemy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.DAZED).findFirst()
                .orElseThrow().remainingTicks()).isEqualTo(10);
    }

    @Test
    void phaseFieldIiMakesTheVisitsRevealLastTwoSeconds() {
        PulseTower tower = this.pulseWith("Phase Field", "Phase Field II");
        FakeEnemyMob enemy = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 0; t < 20; t++) {
            tower.doTick(t);
        }

        assertThat(enemy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.REVEALED).toList())
                .singleElement().extracting(Effect::remainingTicks).isEqualTo(40);
    }

    @Test
    void nullFieldSilencesEveryEnemyInsideAndOnlyThose() {
        PulseTower tower = this.pulseWith("Phase Field", "Phase Field II", "Null Field");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob outside = FakeEnemyMob.at(tower.getX() + 300, tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside, outside});

        tower.doTick(1);

        assertThat(inside.appliedEffects().stream().filter(e -> e.kind() == EffectKind.SILENCED).toList())
                .singleElement().extracting(Effect::remainingTicks).isEqualTo(FieldMode.LINGER_TICKS);
        assertThat(outside.hasEffect(EffectKind.SILENCED)).isFalse();
    }

    @Test
    void trueSightRevealsForFourSecondsAndDazesForTwo() {
        PulseTower tower = this.pulseWith("Phase Field", "Phase Field II", "Null Field", "Transcendent",
                "True Sight");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 0; t < 20; t++) {
            tower.doTick(t);
        }

        assertThat(enemy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.REVEALED).toList())
                .singleElement().extracting(Effect::remainingTicks).isEqualTo(80);
        assertThat(enemy.appliedEffects().stream().filter(e -> e.kind() == EffectKind.DAZED).toList())
                .singleElement().extracting(Effect::remainingTicks).isEqualTo(40);
    }

    @Test
    void deadZoneKeepsHealsAndShieldsFromTakingHoldInsideTheField() {
        PulseTower tower = this.pulseWith("Phase Field", "Phase Field II", "Null Field", "Transcendent", "Dead Zone");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{inside});

        tower.doTick(1);

        assertThat(inside.appliedEffects().stream().filter(e -> e.kind() == EffectKind.DEAD_ZONE).toList())
                .singleElement().extracting(Effect::remainingTicks).isEqualTo(FieldMode.LINGER_TICKS);
    }

    @Test
    void teslaCoilSaysWhatItDoes() {
        PulseTower tower = new PulseTower(this.context, 0, 0);

        assertThat(tower.bonusesOf(UpgradePaths.named(tower, "Tesla Coil"))).contains(new UpgradeBonus(
                "The zap chains to 3 more enemies within 1.5 cells, even outside the field, Exposing each; "
                + "every enemy zapped is Dazed 0.25s", ""));
    }
}
