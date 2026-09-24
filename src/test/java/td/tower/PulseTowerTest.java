package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
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
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
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
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        UpgradeNode resonantField = UpgradePaths.named(tower, "Resonant Field");

        boolean chosen = tower.buyUpgrade(resonantField);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void resonantFieldFiresEvenWithOnlyAGhostInRange() {
        this.context.economy().startEconomy(1000, 5);
        PulseTower tower = new PulseTower(this.context, 0, 0);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
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
}
