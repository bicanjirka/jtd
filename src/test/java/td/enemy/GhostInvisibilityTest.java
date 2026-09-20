package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.EffectKind;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end, headless proof that the Ghost's invisibility is entirely ability/effect-driven -
 * no native {@link EnemyMob.Type} override - the same way {@code WardenChainTest} proves the
 * boss's on-death/hatch chain against a real, live {@link DefinedEnemyMob}.
 */
class GhostInvisibilityTest {

    private static final int VANISH_DURATION_TICKS = 200;

    private static GameWorld worldWithStraightPath() {
        GameWorld world = WorldFixtures.newWorldOnBoard(32, 1000, 1000);
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        return world;
    }

    @Test
    void aFreshGhostIsAnOrdinaryVisibleTargetUntilItTakesItsFirstHit() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob ghost = (DefinedEnemyMob) world.getEnemyCatalog().spawn("g", world, 0, 100, 4, Rank.GRUNT);
        world.enemies().add(ghost);

        assertThat(ghost.validTarget(EnemyMob.Type.NORMAL)).isTrue();
        assertThat(ghost.activeEffectKinds()).doesNotContain(EffectKind.INVISIBLE);
    }

    @Test
    void aGhostVanishesFromSingleTargetTowersForItsFullDurationAfterTakingDamageThenReappears() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob ghost = (DefinedEnemyMob) world.getEnemyCatalog().spawn("g", world, 0, 100, 4, Rank.GRUNT);
        world.enemies().add(ghost);

        ghost.doDamage(Damage.physical(10));
        ghost.doTick(1); // captures the hit and fires the vanish ability in the same call

        assertThat(ghost.activeEffectKinds()).contains(EffectKind.INVISIBLE);
        assertThat(ghost.validTarget(EnemyMob.Type.NORMAL)).isFalse();
        assertThat(ghost.validTarget(EnemyMob.Type.INVISIBLE)).isTrue();
        // still a valid target for area damage, which doesn't filter by type at all
        assertThat(ghost.validTarget()).isTrue();

        for (int t = 2; t <= VANISH_DURATION_TICKS + 2; t++) {
            ghost.doTick(t);
        }

        assertThat(ghost.activeEffectKinds()).doesNotContain(EffectKind.INVISIBLE);
        assertThat(ghost.validTarget(EnemyMob.Type.NORMAL)).isTrue();
    }

    @Test
    void aGhostOnlyVanishesOnceEverNotOnEverySubsequentHit() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob ghost = (DefinedEnemyMob) world.getEnemyCatalog().spawn("g", world, 0, 100, 4, Rank.GRUNT);
        world.enemies().add(ghost);

        ghost.doDamage(Damage.physical(10));
        ghost.doTick(1);
        for (int t = 2; t <= VANISH_DURATION_TICKS + 2; t++) {
            ghost.doTick(t);
        }
        assertThat(ghost.activeEffectKinds()).doesNotContain(EffectKind.INVISIBLE);

        ghost.doDamage(Damage.physical(10));
        ghost.doTick(VANISH_DURATION_TICKS + 3); // a second hit must not re-trigger the ability

        assertThat(ghost.activeEffectKinds()).doesNotContain(EffectKind.INVISIBLE);
    }

    @Test
    void anEliteGhostPermanentlyShroudsANearbyAllyButNeverItself() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob eliteGhost = (DefinedEnemyMob) world.getEnemyCatalog().spawn("g", world, 0, 800, 16, Rank.ELITE);
        DefinedEnemyMob ally = (DefinedEnemyMob) world.getEnemyCatalog().spawn("c", world, 0, 50, 2, Rank.GRUNT);
        world.enemies().add(eliteGhost);
        world.enemies().add(ally);

        for (int t = 1; t <= 20; t++) { // the shroud ability's own periodic interval
            eliteGhost.doTick(t);
            ally.doTick(t);
        }

        assertThat(ally.activeEffectKinds()).contains(EffectKind.INVISIBLE);
        assertThat(eliteGhost.activeEffectKinds()).doesNotContain(EffectKind.INVISIBLE);
    }
}
