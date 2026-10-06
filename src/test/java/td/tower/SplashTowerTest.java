package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.EffectKind;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exactly one normal mob is in range, so it is the random primary; the splash targets are
 * invisible, which splash reaches but targeting does not.
 */
class SplashTowerTest {

    private static final float SPREAD_RADIUS = SplashTower.SPREAD_RADIUS_BASE * BoardFixtures.SCALE; // 56.0

    private final GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private SplashTower towerNear(int cellX, int cellY) {
        return new SplashTower(this.context, cellX, cellY);
    }

    @Test
    void splashDamageFallsOffWithDistanceFromTheBlastCentre() {
        SplashTower tower = towerNear(3, 3);
        FakeEnemyMob blastCentre = FakeEnemyMob.at(100, 100);
        FakeEnemyMob quarterOut = FakeEnemyMob.ghostAt(100, 128);
        FakeEnemyMob threeQuartersOut = FakeEnemyMob.ghostAt(100, 142);
        FakeEnemyMob atTheEdge = FakeEnemyMob.ghostAt(100, 155);
        this.context.enemies().setEnemies(new EnemyMob[]{blastCentre, quarterOut, threeQuartersOut, atTheEdge});

        tower.doTick(0);

        // damage * (1 - (d/radius)^2), radius 56 and base damage 1600:
        // d=0 -> 1600, d=28 -> 1200, d=42 -> 700, d=55 -> 57
        assertThat(blastCentre.onlyHitAmount()).isEqualTo(1600);
        assertThat(quarterOut.onlyHitAmount()).isEqualTo(1200);
        assertThat(threeQuartersOut.onlyHitAmount()).isEqualTo(700);
        assertThat(atTheEdge.onlyHitAmount()).isEqualTo(57);
    }

    @Test
    void everyStepAwayFromTheBlastCentreTakesStrictlyLessDamage() {
        SplashTower tower = towerNear(3, 3);
        FakeEnemyMob blastCentre = FakeEnemyMob.at(100, 100);
        FakeEnemyMob[] ring = new FakeEnemyMob[11];
        for (int i = 0; i < ring.length; i++) {
            ring[i] = FakeEnemyMob.ghostAt(100, 100 + i * 5);
        }
        EnemyMob[] all = new EnemyMob[ring.length + 1];
        all[0] = blastCentre;
        System.arraycopy(ring, 0, all, 1, ring.length);
        this.context.enemies().setEnemies(all);

        tower.doTick(0);

        for (int i = 1; i < ring.length; i++) {
            assertThat(ring[i].onlyHitAmount())
                    .as("mob %d cells further out than mob %d", i, i - 1)
                    .isLessThan(ring[i - 1].onlyHitAmount());
        }
    }

    @Test
    void theBlastCentreItselfTakesFullDamage() {
        SplashTower tower = towerNear(3, 3);
        FakeEnemyMob blastCentre = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{blastCentre});

        tower.doTick(0);

        assertThat(blastCentre.onlyHitAmount()).isEqualTo(DamageUnits.ofPoints(SplashTower.DAMAGE_POINTS));
    }

    @Test
    void anEnemyBeyondTheBlastRadiusIsNotHitAtAll() {
        SplashTower tower = towerNear(3, 3);
        FakeEnemyMob blastCentre = FakeEnemyMob.at(100, 100);
        // one pixel past the radius, but still well inside the tower's own range
        FakeEnemyMob outside = FakeEnemyMob.ghostAt(100, 100 + SPREAD_RADIUS + 1);
        this.context.enemies().setEnemies(new EnemyMob[]{blastCentre, outside});

        tower.doTick(0);

        assertThat(blastCentre.hits()).hasSize(1);
        assertThat(outside.hits()).isEmpty();
    }

    @Test
    void blastEngineeringBumpsTheSpreadRadiusBeyondTheBase() {
        SplashTower tower = towerNear(3, 3);
        UpgradeNode blastEngineering = UpgradePaths.named(tower, "Blast Engineering");
        float radiusBeforeChoosing = tower.getSpreadRadius();

        tower.onUpgradeBought(blastEngineering);

        assertThat(tower.getSpreadRadius()).isGreaterThan(radiusBeforeChoosing);
    }

    @Test
    void blastEngineeringIiFlattensTheFalloffCurve() {
        SplashTower before = towerNear(3, 3);
        FakeEnemyMob edgeBefore = FakeEnemyMob.ghostAt(100, 100 + SPREAD_RADIUS - 1);
        this.context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(100, 100), edgeBefore});
        before.doTick(0);

        SplashTower after = towerNear(3, 3);
        after.onUpgradeBought(UpgradePaths.named(after, "Blast Engineering II"));
        FakeEnemyMob edgeAfter = FakeEnemyMob.ghostAt(100, 100 + SPREAD_RADIUS - 1);
        this.context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(100, 100), edgeAfter});
        after.doTick(0);

        assertThat(edgeAfter.onlyHitAmount()).isGreaterThan(edgeBefore.onlyHitAmount());
    }

    @Test
    void rapidBatteryAppliesItsFireRateBonus() {
        this.context.economy().startEconomy(1000, 5);
        SplashTower tower = towerNear(3, 3);
        UpgradePaths.awakenVeteran(tower);
        UpgradeNode rapidBattery = UpgradePaths.named(tower, "Rapid Battery");

        boolean chosen = tower.buyUpgrade(rapidBattery);

        assertThat(chosen).isTrue();
        assertThat(tower.coolDownCurrent()).isLessThan(tower.coolDownMax);
    }

    @Test
    void concussiveBlastAppliesSlowToEverySplashTarget() {
        this.context.economy().startEconomy(1000, 5);
        SplashTower tower = towerNear(3, 3);
        UpgradePaths.awakenVeteran(tower);
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        for (int i = 0; i < 15; i++) {
            tower.dealDamage(fodder, Damage.physical(1_000_000));
            fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        }
        tower.buyUpgrade(UpgradePaths.named(tower, "Concussive Blast"));
        FakeEnemyMob blastCentre = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{blastCentre});

        tower.doTick(0);

        assertThat(blastCentre.appliedEffects()).hasSize(1);
        assertThat(blastCentre.appliedEffects().getFirst().kind()).isEqualTo(EffectKind.CHILL);
    }

    private SplashTower upgradedTower(String... nodes) {
        SplashTower tower = towerNear(3, 3);
        this.context.towers().add(tower);
        UpgradePaths.buy(tower, this.context, nodes);
        return tower;
    }

    private static void tickThrough(SplashTower tower, int ticks) {
        for (int t = 0; t < ticks; t++) {
            tower.doTick(t);
        }
    }

    @Test
    void blastEngineeringThreeBlastsThreeDistinctTargetsAndFewerWhenFewerAreInRange() {
        SplashTower tower = upgradedTower("Blast Engineering", "Blast Engineering II", "Blast Engineering III");
        FakeEnemyMob a = FakeEnemyMob.at(60, 60);
        FakeEnemyMob b = FakeEnemyMob.at(60, 160);
        FakeEnemyMob c = FakeEnemyMob.at(160, 60);
        this.context.enemies().setEnemies(new EnemyMob[]{a, b, c});

        tower.doTick(0);

        assertThat(tower.getBlasts()).hasSize(3);
        assertThat(a.hits()).hasSize(1);
        assertThat(b.hits()).hasSize(1);
        assertThat(c.hits()).hasSize(1);
        this.context.enemies().setEnemies(new EnemyMob[]{a});
        tickThrough(tower, tower.coolDownCurrent() + 2);
        assertThat(tower.getBlasts()).hasSize(1);
    }

    @Test
    void rapidBatteryThreeWidensTheBlastOfACriticalShotOnly() {
        SplashTower tower = upgradedTower("Rapid Battery", "Rapid Battery II", "Rapid Battery III");
        FakeEnemyMob primary = FakeEnemyMob.at(100, 100);
        FakeEnemyMob outerRing = FakeEnemyMob.ghostAt(100, 100 + Math.round(SPREAD_RADIUS) + 14);
        this.context.enemies().setEnemies(new EnemyMob[]{primary, outerRing});

        tower.doTick(0);
        assertThat(outerRing.hits()).isEmpty();

        primary.landEveryHitCritical();
        tickThrough(tower, tower.coolDownCurrent() + 2);

        assertThat(outerRing.hits()).isNotEmpty();
        assertThat(tower.getBlasts().getFirst().area().radius()).isEqualTo(tower.getSpreadRadius() * 1.5f);
    }

    @Test
    void overpressureBlastsEveryVisibleEnemyOnTheShotAfterACrit() {
        SplashTower tower = upgradedTower("Overpressure");
        FakeEnemyMob a = FakeEnemyMob.at(60, 60);
        FakeEnemyMob b = FakeEnemyMob.at(60, 200);
        a.landEveryHitCritical();
        b.landEveryHitCritical();
        this.context.enemies().setEnemies(new EnemyMob[]{a, b});

        tower.doTick(0);
        assertThat(tower.getBlasts()).hasSize(1);
        tickThrough(tower, tower.coolDownCurrent() + 2);

        assertThat(tower.getBlasts()).hasSize(2);
        assertThat(a.hits().size() + b.hits().size()).isEqualTo(3);
    }

    @Test
    void withoutACritOverpressureStaysUnarmed() {
        SplashTower tower = upgradedTower("Overpressure");
        FakeEnemyMob a = FakeEnemyMob.at(60, 60);
        FakeEnemyMob b = FakeEnemyMob.at(60, 200);
        this.context.enemies().setEnemies(new EnemyMob[]{a, b});

        tower.doTick(0);
        tickThrough(tower, tower.coolDownCurrent() + 2);

        assertThat(tower.getBlasts()).hasSize(1);
    }

    @Test
    void toxicBloomPoisonsEveryEnemyTheBlastCatches() {
        SplashTower tower = upgradedTower("Toxic Bloom");
        FakeEnemyMob primary = FakeEnemyMob.at(100, 100);
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(100, 120);
        this.context.enemies().setEnemies(new EnemyMob[]{primary, ghost});

        tower.doTick(0);

        assertThat(primary.appliedEffects()).extracting(td.effect.Effect::kind).containsExactly(EffectKind.POISON);
        assertThat(ghost.appliedEffects()).extracting(td.effect.Effect::kind).containsExactly(EffectKind.POISON);
    }

    @Test
    void aCriticalBlastStartsAStrongerPoisonThanAPlainOne() {
        FakeEnemyMob plain = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{plain});
        upgradedTower("Toxic Bloom").doTick(0);
        FakeEnemyMob critical = FakeEnemyMob.at(100, 100);
        critical.landEveryHitCritical();
        this.context.enemies().setEnemies(new EnemyMob[]{critical});
        upgradedTower("Toxic Bloom").doTick(0);

        int plainPulse = plain.appliedEffects().getFirst().damagePerTick().amount();
        int criticalPulse = critical.appliedEffects().getFirst().damagePerTick().amount();

        assertThat(criticalPulse).isEqualTo(Math.round(plainPulse * AttackProfile.DEFAULT_CRIT_MULTIPLIER));
    }

    @Test
    void concussiveBlastMakesAKilledEnemyExplodeOntoItsNeighboursWithoutChaining() {
        SplashTower tower = upgradedTower("Concussive Blast");
        FakeEnemyMob primary = FakeEnemyMob.at(100, 100);
        primary.dieOnAnyHit();
        FakeEnemyMob neighbour = FakeEnemyMob.ghostAt(100, 150);
        neighbour.dieOnAnyHit();
        FakeEnemyMob beyond = FakeEnemyMob.ghostAt(100, 200);
        this.context.enemies().setEnemies(new EnemyMob[]{primary, neighbour, beyond});

        tower.doTick(0);

        assertThat(neighbour.hits()).contains(Damage.physical(Math.round(tower.damageCurrent() * 0.5f)));
        assertThat(beyond.hits()).isEmpty();
    }
}
