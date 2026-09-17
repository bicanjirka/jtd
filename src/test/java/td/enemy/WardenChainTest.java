package td.enemy;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.Damage;
import td.effect.EffectKind;
import td.util.GameWorld;
import td.util.RecordingGameHost;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end, headless proof of the Warden/boss-egg chain: an ability-driven on-death spawn,
 * an ability-driven hatch-on-timeout with {@code consumesSelf}, and the resulting spawn never
 * firing once the spawning mob is legitimately killed first. Everything else about abilities
 * (each trigger kind in isolation, each action shape) is already covered headlessly by
 * {@code AbilityEvaluatorTest} against fakes - this proves the same machinery wired into a
 * real, live {@link DefinedEnemyMob} on a real {@link GameWorld}.
 */
class WardenChainTest {

    private static final Damage LETHAL = Damage.physical(5_000_000);

    private static GameWorld worldWithStraightPath() {
        GameWorld world = new GameWorld(new RecordingGameHost());
        world.setBoard(BoardGeometry.of(32, 1000, 1000));
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        return world;
    }

    @Test
    void killingTheWardenSpawnsItsEggAtTheSamePosition() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob warden = (DefinedEnemyMob) world.getEnemyCatalog().spawn("warden1", world, 0, 8000, 100, 1);
        world.enemies().add(warden);

        for (int t = 1; t <= 10; t++) {
            warden.doTick(t);
        }
        double wardenX = warden.getX();
        double wardenY = warden.getY();

        warden.doDamage(LETHAL);
        warden.doTick(11); // captures deathTick and fires the on-death ability in the same call

        List<EnemyMob> enemies = List.of(world.enemies().getEnemies());
        assertThat(enemies).hasSize(2); // the fading Warden, plus its newly spawned egg
        EnemyMob egg = enemies.stream().filter(e -> e != warden).findFirst().orElseThrow();
        assertThat(egg).isInstanceOf(DefinedEnemyMob.class);
        assertThat(((DefinedEnemyMob) egg).archetype()).isEqualTo(BodyArchetype.EGG);
        assertThat(egg.getX()).isEqualTo(wardenX);
        assertThat(egg.getY()).isEqualTo(wardenY);
    }

    @Test
    void anEggLeftAliveForItsFullDelayHatchesIntoTheNextWardenStageInPlace() {
        GameWorld world = worldWithStraightPath();
        EnemyMob egg = world.getEnemyCatalog().spawn("wardenEgg1", world, 0, 1500, 20, 1);
        world.enemies().add(egg);

        for (int t = 1; t <= 165; t++) { // past the 160-tick hatch delay
            for (EnemyMob e : world.enemies().getEnemies()) {
                e.doTick(t);
            }
        }

        List<EnemyMob> enemies = List.of(world.enemies().getEnemies());
        assertThat(enemies).hasSize(1); // replaced, not added alongside - consumesSelf
        assertThat(enemies).doesNotContain(egg);
        assertThat(((DefinedEnemyMob) enemies.getFirst()).archetype()).isEqualTo(BodyArchetype.SQUARE);
    }

    @Test
    void anEggKilledBeforeItsDelayNeverHatches() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob egg = (DefinedEnemyMob) world.getEnemyCatalog().spawn("wardenEgg1", world, 0, 1500, 20, 1);
        world.enemies().add(egg);

        for (int t = 1; t <= 50; t++) {
            egg.doTick(t);
        }
        egg.doDamage(LETHAL);
        egg.doTick(51); // captures deathTick

        // well past the 160-tick hatch delay - nothing should fire once the egg is dead
        for (int t = 52; t <= 250; t++) {
            for (EnemyMob e : world.enemies().getEnemies()) {
                e.doTick(t);
            }
        }

        List<EnemyMob> enemies = List.of(world.enemies().getEnemies());
        assertThat(enemies).hasSize(1);
        assertThat(enemies.getFirst()).isSameAs(egg);
        assertThat(egg.isDead()).isTrue();
    }

    @Test
    void theFinalStageEggHasNoHatchAbilityAndMustSimplyBeDefeated() {
        EnemyDefinition finalEgg = EnemyCatalog.builtIn().get("wardenEgg3");

        assertThat(finalEgg.abilities()).isEmpty();
    }

    @Test
    void theWardenGainsAShieldAfterSurvivingACriticalHit() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob warden = (DefinedEnemyMob) world.getEnemyCatalog().spawn("warden1", world, 0, 8000, 100, 1);
        world.enemies().add(warden);

        warden.doDamage(Damage.physical(5000).asCritical());
        warden.doTick(1); // captures the critical hit and fires the new ability in the same call

        assertThat(warden.activeEffectKinds()).contains(EffectKind.SHIELD);
    }
}
