package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InversionTest {

    private static final int LONG = 1000;

    private static GameWorld worldWithStraightPath() {
        GameWorld world = WorldFixtures.newWorldOnBoard(32, 1000, 1000);
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        return world;
    }

    private static DefinedEnemyMob spawn(GameWorld world, String id, int slot) {
        DefinedEnemyMob mob = (DefinedEnemyMob) world.getEnemyCatalog().spawn(id, world, 0, 100, slot, Rank.GRUNT);
        world.enemies().add(mob);
        return mob;
    }

    /** Inverts {@code mob}, crediting what it deals to {@code credited}. */
    private static void invert(DefinedEnemyMob mob, List<Damage> credited) {
        mob.applyEffect(Effect.hex(EffectKind.INVERSION, LONG, damage -> {
            credited.add(damage);
            mob.doDamage(damage);
        }));
    }

    @Test
    void aMendersHealOnAnInvertedAllyDamagesItAsMagicInsteadOfHealingIt() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob mender = spawn(world, "m", 3);
        DefinedEnemyMob ally = spawn(world, "c", 4);
        ally.doDamage(Damage.physical(5000));
        int woundedHealth = ally.getHealth();
        List<Damage> credited = new ArrayList<>();
        invert(ally, credited);

        for (int t = 1; t <= 20; t++) {
            mender.doTick(t);
            ally.doTick(t);
        }

        assertThat(ally.getHealth()).isLessThan(woundedHealth);
        assertThat(credited).isNotEmpty().allSatisfy(damage -> assertThat(damage.type()).isEqualTo(DamageType.MAGIC));
    }

    @Test
    void anInvertedHealCountsAsDamageTaken() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob ally = spawn(world, "c", 4);
        invert(ally, new ArrayList<>());
        long before = ally.damageTaken();

        ally.applyEffect(Effect.heal(100, 10, damage -> {
        }));
        for (int t = 1; t <= 10; t++) {
            ally.doTick(t);
        }

        assertThat(ally.damageTaken() - before).isPositive();
    }

    @Test
    void aShieldOnAnInvertedEnemyIsOneHitOfItsPercentOfFullHealthAndNeverGoesOn() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob enemy = spawn(world, "c", 4);
        List<Damage> credited = new ArrayList<>();
        invert(enemy, credited);

        enemy.applyEffect(Effect.shield(0.3f, 100, damage -> {
        }));

        assertThat(credited).containsExactly(Damage.magic(Math.round(0.3f * enemy.getMaxHealthPoints() * 100)));
        assertThat(enemy.hasEffect(EffectKind.SHIELD)).isFalse();
    }

    @Test
    void anInvertedEnemyCannotVanish() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob ghost = spawn(world, "g", 4);
        invert(ghost, new ArrayList<>());

        ghost.applyEffect(Effect.invisible(200, damage -> {
        }));

        assertThat(ghost.hasEffect(EffectKind.INVISIBLE)).isFalse();
        assertThat(ghost.isHidden()).isFalse();
    }

    @Test
    void invertingAnEnemyThatHasAlreadyVanishedBringsItBack() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob ghost = spawn(world, "g", 4);
        ghost.applyEffect(Effect.invisible(200, damage -> {
        }));

        invert(ghost, new ArrayList<>());

        assertThat(ghost.isHidden()).isFalse();
    }

    @Test
    void anEnemyThatIsNotInvertedHealsAndShieldsAsBefore() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob enemy = spawn(world, "c", 4);

        enemy.applyEffect(Effect.shield(0.3f, 100, damage -> {
        }));
        enemy.applyEffect(Effect.heal(100, 10, damage -> {
        }));

        assertThat(enemy.activeEffectKinds()).contains(EffectKind.SHIELD, EffectKind.HEAL);
    }

    @Test
    void anEnemyWithAHealAbilityAppliesEffectsAndOneThatOnlyWalksDoesNot() {
        GameWorld world = worldWithStraightPath();

        assertThat(spawn(world, "m", 3).appliesEffects()).isTrue();
        assertThat(spawn(world, "g", 4).appliesEffects()).isTrue();
        assertThat(spawn(world, "c", 5).appliesEffects()).isFalse();
    }
}
