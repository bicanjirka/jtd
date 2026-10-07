package td.enemy;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.util.TickRate;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArmoredBossReinforcementTest {

    private static final int INTERVAL = Math.round(3 * TickRate.TICKS_PER_SECOND);

    private static GameWorld worldWithStraightPath() {
        GameWorld world = WorldFixtures.newWorldOnBoard(32, 1000, 1000);
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        return world;
    }

    private static void tick(DefinedEnemyMob mob, int fromInclusive, int toInclusive) {
        for (int t = fromInclusive; t <= toInclusive; t++) {
            mob.doTick(t);
        }
    }

    @Test
    void aBossRankArmoredCallsThreeReinforcementsEveryThreeSeconds() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob boss = (DefinedEnemyMob) world.getEnemyCatalog().spawn("s", world, 0, 1280, 125, Rank.BOSS);
        world.enemies().add(boss);

        tick(boss, 1, INTERVAL - 1);
        int beforeTheInterval = world.enemies().getEnemies().length;
        tick(boss, INTERVAL, INTERVAL);
        int afterOne = world.enemies().getEnemies().length;
        tick(boss, INTERVAL + 1, 2 * INTERVAL);
        int afterTwo = world.enemies().getEnemies().length;

        assertThat(beforeTheInterval).isEqualTo(1);
        assertThat(afterOne).isEqualTo(4);
        assertThat(afterTwo).isEqualTo(7);
    }

    @Test
    void theReinforcementsAreSquaresThatDoNotReinforceInTurn() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob boss = (DefinedEnemyMob) world.getEnemyCatalog().spawn("s", world, 0, 1280, 125, Rank.BOSS);
        world.enemies().add(boss);

        tick(boss, 1, INTERVAL);

        List<DefinedEnemyMob> reinforcements = List.of(world.enemies().getEnemies()).stream()
                .filter(mob -> mob != boss)
                .map(DefinedEnemyMob.class::cast)
                .toList();
        assertThat(reinforcements).allMatch(mob -> mob.archetype() == BodyArchetype.SQUARE);
        assertThat(reinforcements).allMatch(mob -> mob.definition().abilities().isEmpty());
    }

    @Test
    void anArmoredBelowBossRankCallsNoReinforcements() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob elite = (DefinedEnemyMob) world.getEnemyCatalog().spawn("s", world, 0, 640, 50, Rank.ELITE);
        world.enemies().add(elite);

        tick(elite, 1, INTERVAL);

        assertThat(world.enemies().getEnemies()).hasSize(1);
    }
}
