package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.EnemyFixtures;
import td.fixtures.LevelFixtures;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.stat.StatBreakdown;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermText;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DefinedEnemyMobEffectTest {

    private static GameWorld newContext() {
        return WorldFixtures.newWorldOnBoard(1, 1001, 1001);
    }

    @Test
    void afterAFreezeTheNextOneIsDiminishedButAnEnemyNeverFrozenIsNot() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob frozenOnce = EnemyFixtures.plainMob(context, 50, 3);
        EnemyMob fresh = EnemyFixtures.plainMob(context, 50, 4);

        frozenOnce.applyEffect(Effect.freeze(10, DamageSink.none()));

        assertThat(frozenOnce.freezeDiminished()).isTrue();
        assertThat(fresh.freezeDiminished()).isFalse();
    }

    @Test
    void aSlowReducesTheDistanceCoveredInOneTick() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        float fullSpeed = enemy.getSpeed();

        enemy.applyEffect(Effect.chill(0.5f, 5, DamageSink.none()));
        enemy.doTick(1);

        assertThat(enemy.getX()).isCloseTo(fullSpeed * 0.5f, within(1e-6));
    }

    @Test
    void aFreezeHaltsMovementForExactlyItsDurationInTicks() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);

        enemy.applyEffect(Effect.freeze(2, DamageSink.none()));
        enemy.doTick(1);
        enemy.doTick(2);
        assertThat(enemy.getX()).isEqualTo(0.0);

        enemy.doTick(3); // the freeze has expired by now
        assertThat(enemy.getX()).isGreaterThan(0.0);
    }

    @Test
    void aBurnThatKillsTheEnemyStopsItsMovementInTheSameTick() {
        GameWorld context = newContext();
        context.enemies().setCount(1);
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFixtures.plainMob(context, 1, 3); // 100 health

        enemy.applyEffect(Effect.burn(Damage.magic(1000), 3, enemy::doDamage));
        enemy.doTick(1);

        assertThat(enemy.isDead()).isTrue();
        assertThat(enemy.getX()).isEqualTo(0.0);
    }

    @Test
    void aBurnEffectDealsItsFirstPulseThroughItsBoundSinkOnItsFirstTick() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFixtures.plainMob(context, 100, 3); // 10000 health

        enemy.applyEffect(Effect.burn(Damage.magic(2000), 5, enemy::doDamage));
        enemy.doTick(1);

        // alpha = e^(-3/5); the pulse is 2000 * (1 + a + a^2 + a^3 + a^4) ~= 4212
        assertThat(enemy.getHealth()).isEqualTo(5788);
    }

    @Test
    void aHealEffectRestoresHealthEveryTickUntilItExpires() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFixtures.plainMob(context, 100, 3); // 10000 health
        enemy.doDamage(Damage.physical(4000)); // down to 6000

        enemy.applyEffect(Effect.heal(1000, 2, DamageSink.none()));
        enemy.doTick(1);

        assertThat(enemy.getHealth()).isEqualTo(7000);
    }

    @Test
    void aHealTooSmallForOneUnitPerTickStillAddsUpAtReducedSpirit() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFixtures.liveMob(context,
                EnemyDefinition.of("dull", "Dull", 100_000, 5, 0f, BodyArchetype.CIRCLE).withSpirit(-50f));
        enemy.doDamage(Damage.physical(4000));
        int damaged = enemy.getHealth();

        enemy.applyEffect(Effect.heal(1, 10, DamageSink.none()));
        for (int t = 1; t <= 10; t++) {
            enemy.doTick(t);
        }

        assertThat(enemy.getHealth() - damaged).isEqualTo(5);
    }

    @Test
    void spiritScalesWhatEveryRegenerationTickRestoresATraitsIncluded() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        float pointsPerSecond = 20f;
        EnemyMob enemy = EnemyFixtures.liveMob(context, EnemyDefinition.of("mending", "Mending", 100_000, 5, 0f,
                BodyArchetype.CIRCLE).withSpirit(50f).withTraits(List.of(new RegenerationTrait(pointsPerSecond))));
        enemy.doDamage(Damage.physical(40_000));
        int damaged = enemy.getHealth();

        enemy.doTick(1);

        assertThat(enemy.getHealth() - damaged)
                .isEqualTo(Math.round(DamageUnits.ofPoints(pointsPerSecond) * 1.5f / TickRate.TICKS_PER_SECOND));
    }

    @Test
    void theRegenerationBreakdownEndsAtWhatATickRestoresWithSpiritAsItsOwnLine() {
        GameWorld context = newContext();
        DefinedEnemyMob enemy = EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("blessed").withSpirit(60f));
        enemy.applyEffect(Effect.heal(100, 40, DamageSink.none()));

        StatBreakdown breakdown = enemy.breakdown(EnemyStat.REGENERATION);

        assertThat(breakdown.together()).isEqualTo(100f);
        assertThat(breakdown.scaled()).extracting(part -> part.modifier().multiply()).containsExactly(1.6f);
        assertThat(breakdown.current()).isCloseTo(160f, within(1e-3f));
    }

    @Test
    void aHealEffectNeverRestoresHealthAboveTheMobsMax() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFixtures.plainMob(context, 100, 3); // 10000 health
        enemy.doDamage(Damage.physical(500)); // down to 9500

        enemy.applyEffect(Effect.heal(1000, 2, DamageSink.none()));
        enemy.doTick(1);

        assertThat(enemy.getHealth()).isEqualTo(10000);
    }

    @Test
    void aHealEffectNeverAppliesToAMobAlreadyKilledThisTick() {
        GameWorld context = newContext();
        context.enemies().setCount(1);
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFixtures.plainMob(context, 1, 3); // 100 health

        enemy.applyEffect(Effect.heal(1000, 5, DamageSink.none()));
        enemy.doDamage(Damage.physical(100)); // kills it before this tick's doTick runs
        enemy.doTick(1);

        assertThat(enemy.isDead()).isTrue();
        assertThat(enemy.getHealth()).isZero();
    }

    @Test
    void aHitOnAMarkedEnemyIsAGuaranteedCritAndSpendsTheMark() {
        GameWorld context = WorldFixtures.newWorld(() -> 0.99);
        EnemyMob enemy = EnemyFixtures.plainMob(context, 100000, 3);
        enemy.applyEffect(Effect.marked(100, DamageSink.none()));

        Damage first = enemy.doDamage(Damage.physical(1000), AttackProfile.none());
        Damage second = enemy.doDamage(Damage.physical(1000), AttackProfile.none());

        assertThat(first.critical()).isTrue();
        assertThat(second.critical()).isFalse();
        assertThat(enemy.activeEffectKinds()).doesNotContain(EffectKind.MARKED);
    }

    @Test
    void anotherAttackersHitDischargesAChargeForThirtyPercentThroughTheChargersSink() {
        GameWorld context = WorldFixtures.newWorld(() -> 0.99);
        EnemyMob enemy = EnemyFixtures.plainMob(context, 100000, 3);
        AttackOrigin charger = AttackOrigin.fresh();
        List<Damage> paid = new ArrayList<>();
        enemy.applyEffect(Effect.charged(100, charger, paid::add));

        enemy.doDamage(Damage.physical(1000), AttackProfile.none().withOrigin(charger));
        enemy.doDamage(Damage.physical(1000), AttackProfile.none().withOrigin(AttackOrigin.fresh()));
        enemy.doDamage(Damage.physical(1000), AttackProfile.none().withOrigin(AttackOrigin.fresh()));

        assertThat(paid).containsExactly(Damage.magic(300));
        assertThat(enemy.activeEffectKinds()).doesNotContain(EffectKind.CHARGED);
    }

    @Test
    void aCritDischargesAChargeAtDoubleAndPeriodicDamageNeverDischargesOne() {
        GameWorld context = WorldFixtures.newWorld(() -> 0.0);
        EnemyMob enemy = EnemyFixtures.plainMob(context, 100000, 3);
        List<Damage> paid = new ArrayList<>();
        enemy.applyEffect(Effect.charged(100, AttackOrigin.fresh(), paid::add));

        enemy.doDamage(Damage.physical(1000), AttackProfile.critChance(1f).asPeriodic());
        Damage crit = enemy.doDamage(Damage.physical(1000), AttackProfile.critChance(1f));

        assertThat(paid).containsExactly(Damage.magic(Math.round(crit.amount() * 0.6f)));
    }

    @Test
    void periodicDamageNeitherCritsNorSpendsAMark() {
        GameWorld context = WorldFixtures.newWorld(() -> 0.0);
        EnemyMob enemy = EnemyFixtures.plainMob(context, 100000, 3);
        enemy.applyEffect(Effect.marked(100, DamageSink.none()));

        Damage landed = enemy.doDamage(Damage.physical(1000), AttackProfile.critChance(1f).asPeriodic());

        assertThat(landed.critical()).isFalse();
        assertThat(enemy.activeEffectKinds()).contains(EffectKind.MARKED);
    }

    @Test
    void aMarkWaitsOnACritImmuneEnemyUntilItsResilienceDrops() {
        GameWorld context = WorldFixtures.newWorld(() -> 0.99);
        EnemyMob critImmune = EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("critImmune").withResilience(100f));
        critImmune.applyEffect(Effect.marked(100, DamageSink.none()));

        Damage blocked = critImmune.doDamage(Damage.physical(1000), AttackProfile.none());
        critImmune.applyEffect(Effect.fractured(5, DamageSink.none()));
        Damage opened = critImmune.doDamage(Damage.physical(1000), AttackProfile.none());

        assertThat(blocked.critical()).isFalse();
        assertThat(opened.critical()).isTrue();
        assertThat(critImmune.activeEffectKinds()).doesNotContain(EffectKind.MARKED);
    }

    @Test
    void aRevealedEnemyIsExposedAndHidesAgainWhenItIsShroudedAgain() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 100000, 3);
        enemy.applyEffect(Effect.invisible(100, DamageSink.none()));
        enemy.applyEffect(Effect.revealed(100, DamageSink.none()));
        boolean visibleWhileRevealed = enemy.canBeTargeted();

        enemy.applyEffect(Effect.invisible(100, DamageSink.none()));

        assertThat(visibleWhileRevealed).isTrue();
        assertThat(enemy.canBeTargeted()).isFalse();
        assertThat(enemy.activeEffectKinds()).doesNotContain(EffectKind.REVEALED);
    }

    @Test
    void anInvisibleEnemyIsHiddenFromAimedTowersButStillCaughtByAreaDamage() {
        DefinedEnemyMob enemy = EnemyFixtures.plainMob(newContext(), 100, 3);

        enemy.applyEffect(Effect.invisible(100, DamageSink.none()));

        assertThat(enemy.canBeTargeted()).isFalse();
        assertThat(enemy.isHidden()).isTrue();
        assertThat(enemy.validTarget()).isTrue();
    }

    /** Stands in for a reveal: sets stealth to zero, which beats invisibility's one. */
    private record RevealTrait() implements Trait {
        @Override
        public StatModifiers<EnemyStat> modifiers(TraitContext context) {
            return StatModifiers.of(EnemyStat.STEALTH, StatModifier.setTo(0f));
        }

        @Override
        public TraitEntry entry() {
            return TraitEntry.of(TraitTerm.ADRENALINE, TermText.of("reveals it"));
        }
    }

    @Test
    void aTraitThatRevealsBeatsAnInvisibilityThatKeepsRunning() {
        DefinedEnemyMob enemy = EnemyFixtures.liveMob(newContext(),
                EnemyFixtures.simpleDefinition("revealed").withTraits(List.of(new RevealTrait())));

        enemy.applyEffect(Effect.invisible(100, DamageSink.none()));

        assertThat(enemy.activeEffectKinds()).contains(EffectKind.INVISIBLE);
        assertThat(enemy.isHidden()).isFalse();
        assertThat(enemy.canBeTargeted()).isTrue();
    }
}
