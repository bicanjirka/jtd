package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SilenceTest {

    private static GameWorld worldWithStraightPath() {
        GameWorld world = WorldFixtures.newWorldOnBoard(32, 1000, 1000);
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        return world;
    }

    private static DefinedEnemyMob spawn(GameWorld world, String id, int health, int slot) {
        DefinedEnemyMob mob = (DefinedEnemyMob) world.getEnemyCatalog().spawn(id, world, 0, health, slot, Rank.GRUNT);
        world.enemies().add(mob);
        return mob;
    }

    private static void silence(DefinedEnemyMob mob) {
        mob.applyEffect(Effect.silenced(1000, damage -> {
        }));
    }

    @Test
    void aSilencedMenderHealsNobody() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob mender = spawn(world, "m", 60, 3);
        DefinedEnemyMob ally = spawn(world, "c", 100, 4);
        ally.doDamage(Damage.physical(5000));
        int woundedHealth = ally.getHealth();
        silence(mender);

        for (int t = 1; t <= 40; t++) {
            mender.doTick(t);
            ally.doTick(t);
        }

        assertThat(ally.getHealth()).isEqualTo(woundedHealth);
    }

    @Test
    void aSilencedEnemyStillWalks() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob mender = spawn(world, "m", 60, 3);
        silence(mender);

        mender.doTick(1);

        assertThat(mender.getX()).isGreaterThan(0.0);
    }

    @Test
    void aSilencedEnemysDeathAbilityStillFires() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob spawnling = spawn(world, "tSpawn", 140, 2);
        DefinedEnemyMob ally = spawn(world, "c", 50, 2);
        ally.doDamage(Damage.physical(2000));
        int woundedHealth = ally.getHealth();
        silence(spawnling);

        spawnling.doDamage(Damage.physical(5_000_000));
        spawnling.doTick(1);
        ally.doTick(2);

        assertThat(ally.getHealth()).isGreaterThan(woundedHealth);
    }

    @Test
    void aSilenceThatRanOutLetsTheEnemyCastAgain() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob mender = spawn(world, "m", 60, 3);
        DefinedEnemyMob ally = spawn(world, "c", 100, 4);
        ally.doDamage(Damage.physical(5000));
        int woundedHealth = ally.getHealth();
        mender.applyEffect(Effect.silenced(5, damage -> {
        }));

        for (int t = 1; t <= 60; t++) {
            mender.doTick(t);
            ally.doTick(t);
        }

        assertThat(mender.hasEffect(EffectKind.SILENCED)).isFalse();
        assertThat(ally.getHealth()).isGreaterThan(woundedHealth);
    }

    @Test
    void dispellingStripsAnEnemysShieldAndHealButNothingElse() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob enemy = spawn(world, "c", 100, 4);
        enemy.applyEffect(Effect.shield(0.3f, 100, damage -> {
        }));
        enemy.applyEffect(Effect.heal(5, 100, damage -> {
        }));
        enemy.applyEffect(Effect.exposed(100, damage -> {
        }));

        enemy.dispelRestoratives();

        assertThat(enemy.activeEffectKinds()).containsExactly(EffectKind.EXPOSED);
    }

    @Test
    void anAnchoredEnemyIsHeldToThreeQuartersOfItsBaseSpeedEvenWhenHurtSpeedWouldRaiseIt() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob frenzied = spawn(world, "t", 100, 5);
        frenzied.doDamage(Damage.physical(9000));
        float hurried = frenzied.getSpeed();

        frenzied.applyEffect(Effect.anchored(100, damage -> {
        }));

        assertThat(hurried).isGreaterThan(1.28f);
        assertThat(frenzied.getSpeed()).isCloseTo(1.28f * 0.75f, within(0.001f));
    }
}
