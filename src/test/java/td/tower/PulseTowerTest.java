package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

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
