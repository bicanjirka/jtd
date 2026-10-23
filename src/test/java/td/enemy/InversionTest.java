package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.HealTemplate;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** What an inversion does to a live mob's heals and shields, and which mobs it is worth casting on. */
class InversionTest {

    private static final int LONG = 1000;

    private final GameWorld world = WorldFixtures.newWorldOnStraightPath();

    /** Inverts {@code mob}, crediting what it deals to {@code credited}. */
    private static void invert(DefinedEnemyMob mob, List<Damage> credited) {
        mob.applyEffect(Effect.hex(EffectKind.INVERSION, LONG, damage -> {
            credited.add(damage);
            mob.doDamage(damage);
        }));
    }

    @Test
    void anInvertedHealCountsAsDamageTaken() {
        DefinedEnemyMob enemy = EnemyFixtures.plainMob(this.world, 100, 4);
        invert(enemy, new ArrayList<>());
        long before = enemy.damageTaken();

        enemy.applyEffect(Effect.heal(100, 10, DamageSink.none()));
        for (int t = 1; t <= 10; t++) {
            enemy.doTick(t);
        }

        assertThat(enemy.damageTaken() - before).isPositive();
    }

    @Test
    void aShieldOnAnInvertedEnemyIsOneHitOfItsPointsAndNeverGoesOn() {
        DefinedEnemyMob enemy = EnemyFixtures.plainMob(this.world, 100, 4);
        List<Damage> credited = new ArrayList<>();
        invert(enemy, credited);

        enemy.applyEffect(Effect.shield(1500f, 100, DamageSink.none()));

        assertThat(credited).containsExactly(Damage.magic(1500));
        assertThat(enemy.hasEffect(EffectKind.SHIELD)).isFalse();
    }

    @Test
    void anEnemyAppliesEffectsOnlyWhenOneOfItsAbilitiesGivesOne() {
        DefinedEnemyMob healer = EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("healer")
                .withAbilities(List.of(new Ability(AbilityTerm.MEND, new PeriodicTrigger(20),
                        new ApplyEffectAction(new HealTemplate(1f, 20), new RadiusTarget(80f))))));
        DefinedEnemyMob blinder = EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("blinder")
                .withAbilities(List.of(new Ability(AbilityTerm.BLINDING_BURST, new OnDeathTrigger(),
                        LeaveZoneAction.blindingField(2f, 0.5f)))));

        assertThat(healer.appliesEffects()).isTrue();
        assertThat(blinder.appliesEffects()).isFalse();
        assertThat(EnemyFixtures.plainMob(this.world, 100, 4).appliesEffects()).isFalse();
    }
}
