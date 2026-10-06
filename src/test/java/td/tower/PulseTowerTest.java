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
import td.tower.upgrade.UpgradeSlot;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class PulseTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void overchargedCoilsIsChoosableOnceAwakenIsBoughtAndAppliesItsDamageBonus() {
        this.context.economy().startEconomy(1000, 5);
        PulseTower tower = new PulseTower(this.context, 0, 0);
        UpgradePaths.awaken(tower);
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 100000, 3, Rank.GRUNT);
        tower.dealDamage(fodder, Damage.physical(11000));
        UpgradeNode overchargedCoils = UpgradePaths.named(tower, "Overcharged Coils");

        boolean chosen = tower.buyUpgrade(overchargedCoils);

        assertThat(chosen).isTrue();
        assertThat(tower.damageCurrent()).isGreaterThan(tower.damageBase);
    }

    @Test
    void resonantFieldIsNotYetChoosableBeforeTenKills() {
        this.context.economy().startEconomy(1000, 5);
        PulseTower tower = new PulseTower(this.context, 0, 0);
        UpgradePaths.awaken(tower);
        UpgradeNode resonantField = UpgradePaths.named(tower, "Resonant Field");

        boolean chosen = tower.buyUpgrade(resonantField);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void resonantFieldFiresEvenWithOnlyAGhostInRange() {
        this.context.economy().startEconomy(1000, 5);
        PulseTower tower = new PulseTower(this.context, 0, 0);
        UpgradePaths.awaken(tower);
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        for (int i = 0; i < 10; i++) {
            tower.dealDamage(fodder, Damage.physical(1_000_000));
            fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        }
        tower.buyUpgrade(UpgradePaths.named(tower, "Resonant Field"));
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{ghost});

        tower.doTick(0);

        assertThat(tower.isFiring()).isTrue();
        assertThat(ghost.hits()).hasSize(1);
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
