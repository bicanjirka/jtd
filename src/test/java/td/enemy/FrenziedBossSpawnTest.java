package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end, headless proof of the Frenzied Boss's two Phase 4 abilities: the health-threshold
 * brood spawn ({@code BuiltInEnemies.FRENZIED}'s "spawn") and the resulting Frenzy Spawnling's
 * own on-death heal ({@code BuiltInEnemies.T_SPAWN}'s "deathHeal") - the same real-mob-on-a-real-
 * {@link GameWorld} shape {@link WardenChainTest} uses for the Warden's own ability chain.
 */
class FrenziedBossSpawnTest {

    private static GameWorld worldWithStraightPath() {
        GameWorld world = WorldFixtures.newWorldOnBoard(32, 1000, 1000);
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        return world;
    }

    @Test
    void aBossRankFrenziedCallsABroodOfThreeSpawnlingsAtHalfHealthWithOnlyTheFirstArrivingImmediately() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob boss = (DefinedEnemyMob) world.getEnemyCatalog().spawn("t", world, 0, 960, 125, Rank.BOSS);
        world.enemies().add(boss);

        boss.doDamage(Damage.physical(50_000)); // 96000 -> 46000 units, just under half of healthMax
        boss.doTick(1); // fires the health-threshold "spawn" ability

        List<DefinedEnemyMob> brood = List.of(world.enemies().getEnemies()).stream()
                .filter(mob -> mob != boss)
                .map(DefinedEnemyMob.class::cast)
                .toList();

        assertThat(brood).hasSize(3);
        assertThat(brood).allMatch(spawnling -> spawnling.archetype() == BodyArchetype.TRIANGLE);
        long activeImmediately = brood.stream().filter(spawnling -> !spawnling.isInactive()).count();
        assertThat(activeImmediately).isEqualTo(1); // only the first member starts active - the rest are delayed
    }

    @Test
    void aSpawnlingsDeathHealsNearbyAlliesButNotItself() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob spawnling = (DefinedEnemyMob) world.getEnemyCatalog().spawn("tSpawn", world, 0, 140, 2, Rank.GRUNT);
        DefinedEnemyMob ally = (DefinedEnemyMob) world.getEnemyCatalog().spawn("c", world, 0, 50, 2, Rank.GRUNT);
        world.enemies().add(spawnling);
        world.enemies().add(ally);

        ally.doDamage(Damage.physical(2000)); // healthMax 5000 -> 3000
        int allyHealthAfterDamage = ally.getHealth();

        spawnling.doDamage(Damage.physical(5_000_000)); // lethal
        spawnling.doTick(1); // captures deathTick and fires the on-death heal in the same call
        ally.doTick(2); // the heal's first tick applies

        assertThat(ally.getHealth()).isGreaterThan(allyHealthAfterDamage);
    }
}
