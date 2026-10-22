package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.enemy.Rank;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionPenalty;
import td.util.GameWorld;
import td.zone.Zone;
import td.zone.ZoneOwner;

import static org.assertj.core.api.Assertions.assertThat;

/** In the package of {@code AbstractTower} so it can land hits directly. */
class BlindedTowerTest {

    private static final float MISS_CHANCE = 0.75f;

    private HitResult hitWithRoll(double roll, boolean blinded) {
        GameWorld world = WorldFixtures.newWorldOnBoard(() -> roll, 32, 500, 500);
        SniperTower tower = new SniperTower(world, 0, 0);
        if (blinded) {
            world.zones().add(Zone.blinding(tower.centerX, tower.centerY, 64, Zone.UNTIL_WAVE_END,
                    DisruptionPenalty.blinding(MISS_CHANCE), ZoneOwner.nobody()));
        }
        tower.beginTick(1);
        EnemyMob enemy = EnemyFactory.getEnemy("c", world, 0, 1000, 3, Rank.GRUNT);
        return tower.landHit(enemy, Damage.physical(100), AttackProfile.none());
    }

    @Test
    void aBlindedTowerMissesWhenTheRollIsUnderTheMissChance() {
        HitResult result = this.hitWithRoll(0.5, true);

        assertThat(result.dodged()).isTrue();
    }

    @Test
    void aBlindedTowerStillHitsWhenTheRollIsOverTheMissChance() {
        HitResult result = this.hitWithRoll(0.9, true);

        assertThat(result.dodged()).isFalse();
        assertThat(result.credited()).isPositive();
    }

    @Test
    void aTowerOutsideEveryBlindingFieldNeverMisses() {
        HitResult result = this.hitWithRoll(0.0, false);

        assertThat(result.dodged()).isFalse();
    }
}
