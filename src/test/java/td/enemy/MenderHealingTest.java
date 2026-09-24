package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MenderHealingTest {

    private static GameWorld worldWithStraightPath() {
        GameWorld world = WorldFixtures.newWorldOnBoard(32, 1000, 1000);
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        return world;
    }

    @Test
    void aWoundedAllyNearAMenderIsHealedOverTime() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob mender = (DefinedEnemyMob) world.getEnemyCatalog().spawn("m", world, 0, 60, 3, Rank.GRUNT);
        DefinedEnemyMob ally = (DefinedEnemyMob) world.getEnemyCatalog().spawn("c", world, 0, 100, 4, Rank.GRUNT);
        world.enemies().add(mender);
        world.enemies().add(ally);
        ally.doDamage(Damage.physical(5000)); // down from 10000
        int woundedHealth = ally.getHealth();

        for (int t = 1; t <= 20; t++) { // the heal ability's own periodic interval
            mender.doTick(t);
            ally.doTick(t);
        }

        assertThat(ally.getHealth()).isGreaterThan(woundedHealth);
    }

    @Test
    void aMenderNeverHealsItself() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob mender = (DefinedEnemyMob) world.getEnemyCatalog().spawn("m", world, 0, 60, 3, Rank.GRUNT);
        world.enemies().add(mender);
        mender.doDamage(Damage.physical(1000)); // down from 6000
        int woundedHealth = mender.getHealth();

        for (int t = 1; t <= 20; t++) {
            mender.doTick(t);
        }

        assertThat(mender.getHealth()).isEqualTo(woundedHealth);
    }

    @Test
    void anAllyOutsideTheHealRadiusIsUnaffected() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob mender = (DefinedEnemyMob) world.getEnemyCatalog().spawn("m", world, 0, 60, 3, Rank.GRUNT);
        DefinedEnemyMob farAlly = (DefinedEnemyMob) world.getEnemyCatalog().spawn("c", world, 0, 100, 4, Rank.GRUNT);
        world.enemies().add(mender);
        world.enemies().add(farAlly);
        farAlly.jumpToDistance(900); // far outside the heal radius
        farAlly.doDamage(Damage.physical(5000));
        int woundedHealth = farAlly.getHealth();

        for (int t = 1; t <= 20; t++) {
            mender.doTick(t);
            farAlly.doTick(t);
        }

        assertThat(farAlly.getHealth()).isEqualTo(woundedHealth);
    }
}
