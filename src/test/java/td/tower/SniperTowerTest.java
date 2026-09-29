package td.tower;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.EffectKind;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class SniperTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void focusedOpticsIsChoosableOnceAwakenIsBoughtAndAppliesItsDamageBonus() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        UpgradeNode focusedOptics = UpgradePaths.named(tower, "Focused Optics");

        boolean chosen = tower.buyUpgrade(focusedOptics);

        assertThat(chosen).isTrue();
        assertThat(tower.damageCurrent()).isGreaterThan(tower.damageBase);
    }

    @Test
    void marksmansEyeIsNotYetChoosableBeforeFifteenKills() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        UpgradeNode marksmansEye = UpgradePaths.named(tower, "Marksman's Eye");

        boolean chosen = tower.buyUpgrade(marksmansEye);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void marksmansEyeGrantsACritChanceOnceKillCountIsMet() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        for (int i = 0; i < 15; i++) {
            tower.dealDamage(fodder, Damage.physical(1_000_000));
            fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        }
        UpgradeNode marksmansEye = UpgradePaths.named(tower, "Marksman's Eye");

        boolean chosen = tower.buyUpgrade(marksmansEye);

        assertThat(chosen).isTrue();
        assertThat(tower.critChance()).isGreaterThan(0f);
    }

    @Test
    void buyingAnyHeadRootForeclosesTheOtherHeadRootForever() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        tower.buyUpgrade(UpgradePaths.named(tower, "Focused Optics"));

        boolean chosenMarksmansEye = tower.buyUpgrade(UpgradePaths.named(tower, "Marksman's Eye"));

        assertThat(chosenMarksmansEye).isFalse();
    }

    @Test
    void doTickRecordsWhetherTheShotThatJustFiredWasACriticalHit() {
        GameWorld alwaysCrits = new GameWorld(new RecordingGameHost(), () -> 0.0);
        alwaysCrits.setBoard(BoardGeometry.of(BoardFixtures.SCALE, 20, 20));
        SniperTower tower = new SniperTower(alwaysCrits, 3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        target.landEveryHitCritical();
        alwaysCrits.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(tower.wasLastShotCritical()).isTrue();
        assertThat(target.attackers().getFirst().critChance()).isEqualTo(tower.critChance());
    }

    private static SniperTower awakenedSniper(GameWorld world, int killsEarned) {
        world.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(world, 3, 3);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        for (int i = 0; i < killsEarned; i++) {
            tower.dealDamage(EnemyFactory.getEnemy("c", world, 0, 1, 1, Rank.GRUNT), Damage.physical(1_000_000));
        }
        return tower;
    }

    private static FakeEnemyMob targetFor(GameWorld world) {
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        world.enemies().setEnemies(new EnemyMob[]{target});
        return target;
    }

    private static void fireShots(SniperTower tower, FakeEnemyMob target, int shots) {
        for (int tick = 1; target.hits().size() < shots; tick++) {
            tower.doTick(tick);
        }
    }

    @Test
    void marksmansEyeTwoIgnoresHalfOfTheTargetsArmor() {
        GameWorld world = WorldFixtures.newWorld();
        SniperTower tower = awakenedSniper(world, 15);
        tower.buyUpgrade(UpgradePaths.named(tower, "Marksman's Eye"));
        tower.dealDamage(EnemyFactory.getEnemy("c", world, 0, 100_000, 1, Rank.GRUNT), Damage.physical(20_000));

        boolean bought = tower.buyUpgrade(UpgradePaths.named(tower, "Marksman's Eye II"));

        assertThat(bought).isTrue();
        assertThat(tower.stats().attack().armorPenetration()).isEqualTo(0.5f);
    }

    @Test
    void fifthShotMakesEveryFifthShotACritWorthTwoAndAHalfTimes() {
        GameWorld world = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SniperTower tower = awakenedSniper(world, 15);
        tower.buyUpgrade(UpgradePaths.named(tower, "Fifth Shot"));
        FakeEnemyMob target = targetFor(world);

        fireShots(tower, target, 5);

        assertThat(target.attackers()).extracting(AttackProfile::critChance)
                .containsExactly(tower.critChance(), tower.critChance(), tower.critChance(), tower.critChance(), 1f);
        assertThat(target.attackers()).extracting(AttackProfile::critMultiplier).containsOnly(2.5f);
    }

    @Test
    void momentumTurnsTheShotAfterACritIntoAFivefoldShotThatIgnoresArmorAndPlating() {
        GameWorld world = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SniperTower tower = awakenedSniper(world, 20);
        tower.buyUpgrade(UpgradePaths.named(tower, "Momentum"));
        FakeEnemyMob target = targetFor(world);
        target.landEveryHitCritical();

        fireShots(tower, target, 2);

        assertThat(target.hits().get(1).amount()).isEqualTo(5 * target.hits().get(0).amount());
        assertThat(target.attackers().get(0).armorPenetration()).isZero();
        assertThat(target.attackers().get(1).armorPenetration()).isEqualTo(1f);
        assertThat(target.attackers().get(1).platingPenetration()).isEqualTo(1f);
    }

    @Test
    void momentumHalvesTheCooldownForFiveSecondsAfterAKillThenItEnds() {
        GameWorld world = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SniperTower tower = awakenedSniper(world, 20);
        tower.buyUpgrade(UpgradePaths.named(tower, "Momentum"));
        FakeEnemyMob target = targetFor(world);
        target.dieOnAnyHit();
        int unbuffed = tower.coolDownCurrent();

        tower.beginTick(1);
        tower.doTick(1); // kills the target

        assertThat(tower.coolDownCurrent()).isEqualTo(Math.round(unbuffed * 0.5f));
        tower.beginTick(1 + 99);
        assertThat(tower.coolDownCurrent()).isLessThan(unbuffed);
        tower.beginTick(1 + 100);
        assertThat(tower.coolDownCurrent()).isEqualTo(unbuffed);
    }

    @Test
    void withoutMomentumAKillGrantsNoBuff() {
        GameWorld world = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SniperTower tower = awakenedSniper(world, 10);
        FakeEnemyMob target = targetFor(world);
        target.dieOnAnyHit();
        int unbuffed = tower.coolDownCurrent();

        tower.doTick(1);

        assertThat(tower.coolDownCurrent()).isEqualTo(unbuffed);
    }

    @Test
    void markedRoundAppliesAVulnerabilityStackOnlyWhenTheShotCrits() {
        GameWorld world = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SniperTower tower = new SniperTower(world, 0, 0);
        UpgradePaths.buy(tower, world, "Marked Round");
        FakeEnemyMob target = targetFor(world);

        fireShots(tower, target, 1);
        assertThat(target.appliedEffects()).isEmpty();

        target.landEveryHitCritical();
        fireShots(tower, target, 2);

        assertThat(target.appliedEffects()).hasSize(1);
        assertThat(target.appliedEffects().getFirst().kind()).isEqualTo(EffectKind.VULNERABLE);
        assertThat(target.appliedEffects().getFirst().stacks()).isEqualTo(1);
    }
}
