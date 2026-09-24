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

class AbilityCastCaptureTest {

    private static final float GHOST_SHROUD_RADIUS = 100f;

    private static GameWorld worldWithStraightPath() {
        GameWorld world = WorldFixtures.newWorldOnBoard(32, 1000, 1000);
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        return world;
    }

    @Test
    void aRadiusTargetedCastRecordsTheAuthoredKindAndRadiusOnTheCaster() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob eliteGhost = (DefinedEnemyMob) world.getEnemyCatalog().spawn("g", world, 0, 800, 16, Rank.ELITE);
        world.enemies().add(eliteGhost);

        assertThat(eliteGhost.lastAbilityCast()).isEmpty();
        for (int t = 1; t <= 20; t++) { // the shroud ability's own periodic interval
            eliteGhost.doTick(t);
        }

        AbilityCast cast = eliteGhost.lastAbilityCast().orElseThrow();
        assertThat(cast.kind()).isEqualTo(EffectKind.INVISIBLE);
        assertThat(cast.radius()).isEqualTo(GHOST_SHROUD_RADIUS);
        assertThat(cast.tick()).isEqualTo(20);
    }

    @Test
    void aSelfTargetedCastRecordsARadiusOfZero() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob ghost = (DefinedEnemyMob) world.getEnemyCatalog().spawn("g", world, 0, 100, 4, Rank.GRUNT);
        world.enemies().add(ghost);

        ghost.doDamage(Damage.physical(10));
        ghost.doTick(1); // fires the self-targeted vanish ability

        AbilityCast cast = ghost.lastAbilityCast().orElseThrow();
        assertThat(cast.kind()).isEqualTo(EffectKind.INVISIBLE);
        assertThat(cast.radius()).isZero();
    }
}
