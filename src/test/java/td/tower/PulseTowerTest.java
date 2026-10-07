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
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PulseTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void overchargedCoilsIsChoosableOnceAwakenIsBoughtAndAppliesItsDamageBonus() {
        this.context.economy().startEconomy(1000, 5);
        PulseTower tower = new PulseTower(this.context, 0, 0);
        UpgradePaths.awakenVeteran(tower);
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 100000, 3, Rank.GRUNT);
        tower.dealDamage(fodder, Damage.physical(11000));
        UpgradeNode overchargedCoils = UpgradePaths.named(tower, "Overcharged Coils");

        boolean chosen = tower.buyUpgrade(overchargedCoils);

        assertThat(chosen).isTrue();
        assertThat(tower.damageCurrent()).isGreaterThan(tower.damageBase);
    }


    @Test
    void theFieldHitsEveryEnemyInRangeForMagicEveryTickHiddenOnesIncluded() {
        PulseTower tower = new PulseTower(this.context, 0, 0);
        FakeEnemyMob visible = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{visible, hidden});

        tower.doTick(0);
        tower.doTick(1);

        assertThat(visible.hits()).containsExactly(Damage.magic(200), Damage.magic(200));
        assertThat(hidden.hits()).containsExactly(Damage.magic(200), Damage.magic(200));
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
    void eachTollStackMakesTheFieldHitTenPercentHarder() {
        PulseTower tower = this.attunedTower();
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int t = 0; t < 21; t++) {
            tower.doTick(t);
        }

        assertThat(enemy.hits().getFirst()).isEqualTo(Damage.magic(200));
        assertThat(enemy.hits().getLast()).isEqualTo(Damage.magic(220));
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
    void overchargedCoilsMakesTollBuildTwiceAsFast() {
        PulseTower tower = this.pulseWith("Overcharged Coils");
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
    void overchargedCoilsIiHitsAnEnemyAtFullTollAQuarterHarder() {
        PulseTower tower = this.pulseWith("Overcharged Coils", "Overcharged Coils II");
        FakeEnemyMob full = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(full, 5);
        FakeEnemyMob partial = FakeEnemyMob.at(tower.getX(), tower.getY());
        fullToll(partial, 4);
        this.board.enemies().setEnemies(new EnemyMob[]{full, partial});

        tower.doTick(1);

        int tick = tower.damageCurrent();
        assertThat(full.hits().getFirst()).isEqualTo(Damage.magic(Math.round(tick * (1f + 0.5f + 0.25f))));
        assertThat(partial.hits().getFirst()).isEqualTo(Damage.magic(Math.round(tick * (1f + 0.4f))));
    }

    @Test
    void arcDischargeZapsTheHealthiestEnemyInsideOnceASecondForFifteenTicksOfDamage() {
        PulseTower tower = this.pulseWith("Overcharged Coils", "Overcharged Coils II", "Arc Discharge");
        FakeEnemyMob healthy = FakeEnemyMob.at(tower.getX(), tower.getY()).withHealth(900);
        FakeEnemyMob weak = FakeEnemyMob.at(tower.getX() + 10, tower.getY()).withHealth(100);
        this.board.enemies().setEnemies(new EnemyMob[]{healthy, weak});

        tower.doTick(20);
        tower.doTick(21);
        tower.doTick(40);

        Damage zap = Damage.magic(tower.damageCurrent() * 15);
        assertThat(healthy.hits().stream().filter(zap::equals)).hasSize(2);
        assertThat(weak.hits()).doesNotContain(zap);
        assertThat(tower.getZaps()).hasSize(1);
    }

    @Test
    void theZapCanCritWhileTheFieldTicksNeverDo() {
        PulseTower tower = this.pulseWith("Overcharged Coils", "Overcharged Coils II", "Arc Discharge");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(20);

        assertThat(enemy.attackers().get(0).delivery().name()).isEqualTo("PERIODIC");
        assertThat(enemy.attackers().get(1).delivery().name()).isEqualTo("HIT");
        assertThat(enemy.attackers().get(1).critChance()).isEqualTo(0.1f);
    }

    @Test
    void meltdownLetsTollStackToTenAndRaisesTheZapWithIt() {
        PulseTower plain = this.pulseWith("Overcharged Coils", "Overcharged Coils II", "Arc Discharge");
        PulseTower melting = this.pulseWith("Overcharged Coils", "Overcharged Coils II", "Arc Discharge",
                "Transcendent", "Meltdown");
        FakeEnemyMob enemy = FakeEnemyMob.at(melting.getX(), melting.getY());
        fullToll(enemy, 10);
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        melting.doTick(20);

        Damage zap = Damage.magic(Math.round(melting.damageCurrent() * 15 * 2f));
        assertThat(enemy.hits()).contains(zap);
        assertThat(enemy.attackers().get(1).critChance()).isEqualTo(0.2f);
        assertThat(tollOn(enemy).getLast().effectiveStackCap()).isEqualTo(10);
        assertThat(plain.damageCurrent()).isEqualTo(melting.damageCurrent());
    }

    @Test
    void teslaCoilChainsToThreeMoreEnemiesEvenOutsideTheFieldAndDazesEveryOneItHits() {
        PulseTower tower = this.pulseWith("Overcharged Coils", "Overcharged Coils II", "Arc Discharge",
                "Transcendent", "Tesla Coil");
        float x = tower.getX();
        float y = tower.getY();
        FakeEnemyMob first = FakeEnemyMob.at(x, y).withHealth(900);
        FakeEnemyMob second = FakeEnemyMob.at(x + 40, y).withHealth(100);
        FakeEnemyMob third = FakeEnemyMob.ghostAt(x + 80, y);
        FakeEnemyMob fourth = FakeEnemyMob.ghostAt(x + 118, y);
        FakeEnemyMob fifth = FakeEnemyMob.ghostAt(x + 156, y);
        this.board.enemies().setEnemies(new EnemyMob[]{first, second, third, fourth, fifth});

        tower.doTick(20);

        Damage zap = Damage.magic(tower.damageCurrent() * 15);
        for (FakeEnemyMob zapped : List.of(first, second, third, fourth)) {
            assertThat(zapped.hits()).contains(zap);
            assertThat(zapped.appliedEffects().stream().filter(e -> e.kind() == EffectKind.DAZED).toList())
                    .singleElement().extracting(Effect::remainingTicks).isEqualTo(5);
        }
        assertThat(fifth.hits()).isEmpty();
        assertThat(tower.getZaps()).hasSize(4);
    }

    @Test
    void wideFieldAddsTwentyPercentRangeAndMakesTollLastASecondLongerAfterAnEnemyLeaves() {
        PulseTower plain = this.pulseWith("Overcharged Coils", "Overcharged Coils II", "Arc Discharge");
        PulseTower wide = this.pulseWith("Overcharged Coils", "Overcharged Coils II", "Arc Discharge", "Transcendent",
                "Range", "Range II", "Range III");
        FakeEnemyMob enemy = FakeEnemyMob.at(wide.getX(), wide.getY());
        fullToll(enemy, 5);
        this.board.enemies().setEnemies(new EnemyMob[]{enemy});

        wide.doTick(1);

        assertThat(tollOn(enemy).getLast().remainingTicks()).isEqualTo(40);
        assertThat(wide.rangeReal()).isGreaterThan(plain.rangeReal());
    }

    @Test
    void wardingFieldAppliesAVulnerabilityStackToEveryEnemyHitWhenTheChanceRollSucceeds() {
        GameWorld lucky = WorldFixtures.newWorld(() -> 0.0);
        PulseTower tower = new PulseTower(lucky, 0, 0);
        UpgradePaths.buy(tower, lucky, "Warding Field");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        lucky.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(0);

        assertThat(enemy.appliedEffects()).extracting(Effect::kind).containsExactly(EffectKind.VULNERABLE);
    }

    @Test
    void wardingFieldAppliesNothingWhenTheChanceRollFails() {
        GameWorld unlucky = WorldFixtures.newWorld(() -> 0.5);
        PulseTower tower = new PulseTower(unlucky, 0, 0);
        UpgradePaths.buy(tower, unlucky, "Warding Field");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        unlucky.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(0);

        assertThat(enemy.appliedEffects()).isEmpty();
    }

    @Test
    void resonantFieldTwoRevealsAHiddenEnemyItHitsForTwoSeconds() {
        PulseTower tower = new PulseTower(this.context, 0, 0);
        UpgradePaths.buy(tower, this.context, "Resonant Field", "Resonant Field II");
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        FakeEnemyMob visible = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{ghost, visible});

        tower.doTick(0);

        assertThat(ghost.appliedEffects()).extracting(Effect::kind).containsExactly(EffectKind.REVEALED);
        assertThat(ghost.appliedEffects().getFirst().remainingTicks()).isEqualTo(40);
        assertThat(visible.appliedEffects()).isEmpty();
    }
}
