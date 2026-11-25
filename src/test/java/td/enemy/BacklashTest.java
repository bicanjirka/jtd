package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.HealTemplate;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.StatCurves;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** What Backlash does to a live mob's heals and shields, and which mobs it is worth casting on. */
class BacklashTest {

    private static final int LONG = 1000;

    private final GameWorld world = WorldFixtures.newWorldOnStraightPath();

    /** Hexes {@code mob} with Backlash, crediting what it deals to {@code credited}. */
    private static void backlash(DefinedEnemyMob mob, List<Damage> credited) {
        mob.applyEffect(Effect.hex(EffectKind.BACKLASH, LONG, damage -> {
            credited.add(damage);
            mob.doDamage(damage);
        }));
    }

    @Test
    void aBacklashedEnemysOwnRegenerationIsDealtToItAsMagicCreditedToTheHex() {
        float pointsPerSecond = 20f;
        DefinedEnemyMob enemy = EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("mending")
                .withTraits(List.of(new RegenerationTrait(pointsPerSecond))));
        enemy.doDamage(Damage.physical(enemy.getHealth() / 2));
        List<Damage> credited = new ArrayList<>();
        backlash(enemy, credited);
        int wounded = enemy.getHealth();

        enemy.doTick(1);

        assertThat(credited).containsExactly(Damage.magic(Math.round(DamageUnits.ofPoints(pointsPerSecond) / TickRate.TICKS_PER_SECOND)));
        assertThat(enemy.getHealth()).isLessThan(wounded);
    }

    @Test
    void aShieldOnABacklashedEnemyIsOneHitOfItsPointsAndNeverGoesOn() {
        DefinedEnemyMob enemy = EnemyFixtures.plainMob(this.world, 100, 4);
        List<Damage> credited = new ArrayList<>();
        backlash(enemy, credited);

        enemy.applyEffect(Effect.shield(1500f, 100, DamageSink.none()));

        assertThat(credited).containsExactly(Damage.magic(1500));
        assertThat(enemy.hasEffect(EffectKind.SHIELD)).isFalse();
    }

    @Test
    void aHealOnABacklashedEnemyIsOneHitOfAllItWouldGiveAndNeverGoesOn() {
        DefinedEnemyMob enemy = EnemyFixtures.plainMob(this.world, 100, 4);
        List<Damage> credited = new ArrayList<>();
        backlash(enemy, credited);

        enemy.applyEffect(Effect.heal(30, 50, DamageSink.none()));

        assertThat(credited).containsExactly(Damage.magic(Math.round(30 * 50 * StatCurves.restoreFactor(0f))));
        assertThat(enemy.hasEffect(EffectKind.HEAL)).isFalse();
    }

    @Test
    void aHealRenewedBeforeItRanOutStrikesOnlyWhatTheRenewalAdds() {
        DefinedEnemyMob enemy = EnemyFixtures.plainMob(this.world, 100, 4);
        List<Damage> credited = new ArrayList<>();
        backlash(enemy, credited);
        AttackOrigin mender = AttackOrigin.fresh();
        enemy.applyEffect(Effect.heal(30, 50, DamageSink.none()), mender);
        for (int tick = 1; tick <= 20; tick++) {
            enemy.doTick(tick);
        }
        credited.clear();

        enemy.applyEffect(Effect.heal(30, 50, DamageSink.none()), mender);

        assertThat(credited).containsExactly(Damage.magic(Math.round(30 * 20 * StatCurves.restoreFactor(0f))));
    }

    @Test
    void aHealTheEnemyHadWhenBacklashLandedStrikesAtOnceWhatItHadLeft() {
        DefinedEnemyMob enemy = EnemyFixtures.plainMob(this.world, 100, 4);
        enemy.doDamage(Damage.physical(enemy.getHealth() / 2));
        enemy.applyEffect(Effect.heal(30, 50, DamageSink.none()));
        List<Damage> credited = new ArrayList<>();

        backlash(enemy, credited);

        assertThat(credited).containsExactly(Damage.magic(Math.round(30 * 50 * StatCurves.restoreFactor(0f))));
        assertThat(enemy.hasEffect(EffectKind.HEAL)).isFalse();
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

    @Test
    void anEnemyRegeneratesOnlyWhenATickWouldBringItHealth() {
        DefinedEnemyMob regenerating = EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("regenerating")
                .withTraits(List.of(new RegenerationTrait(20f))));
        DefinedEnemyMob crumbling = EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("crumbling")
                .withTraits(List.of(new CrumbleTrait(20f))));
        DefinedEnemyMob spiritless = EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("spiritless")
                .withTraits(List.of(new RegenerationTrait(20f)))
                .withSpirit(-100f));

        assertThat(regenerating.regenerates()).isTrue();
        assertThat(crumbling.regenerates()).isFalse();
        assertThat(spiritless.regenerates()).isFalse();
        assertThat(EnemyFixtures.plainMob(this.world, 100, 4).regenerates()).isFalse();
    }
}
