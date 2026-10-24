package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.damage.Delivery;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.HealTemplate;
import td.fixtures.BoardFixtures;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class ObservedEnemyTest {

    private static final AttackProfile PERIODIC = AttackProfile.none().asPeriodic();

    private final GameWorld world = WorldFixtures.newWorldOnStraightPath();
    private final RecordingMobEvents events = new RecordingMobEvents();

    @Test
    void aHitReportsEachStepOfTheFormulaAndWhatLanded() {
        DefinedEnemyMob mob = this.observed(this.world, plain().withArmor(100f).withPlating(2f));

        mob.takeHit(Damage.physical(1000), AttackProfile.none());

        MobEvent.Hit hit = this.events.of(MobEvent.Hit.class).getFirst();
        assertThat(hit.steps().steps()).containsExactly(HitStep.mitigation(100f, 100f, 500f),
                HitStep.flatReduction(200f, 200f, 300f), HitStep.damageTaken(1f, 300f));
        assertThat(hit.outcome()).isEqualTo(new MobEvent.HitOutcome(0, Damage.physical(300), 10_000, 9_700));
    }

    @Test
    void aRolledCritIsTheHitsFirstStep() {
        DefinedEnemyMob mob = this.observed(this.world, plain());

        mob.takeHit(Damage.physical(1000), AttackProfile.critChance(1f));

        MobEvent.Hit hit = this.events.of(MobEvent.Hit.class).getFirst();
        float factor = AttackProfile.DEFAULT_CRIT_MULTIPLIER;
        assertThat(hit.steps().steps().getFirst()).isEqualTo(HitStep.crit(factor, 1000 * factor));
        assertThat(hit.steps().result().critical()).isTrue();
    }

    @Test
    void aDodgedHitIsReportedAsADodgeAndNoHit() {
        GameWorld everyRollSucceeds = WorldFixtures.newWorldOnStraightPath(() -> 0.0);
        DefinedEnemyMob mob = this.observed(everyRollSucceeds, plain().withAgility(50f));

        mob.takeHit(Damage.physical(1000), AttackProfile.none().asAimed());

        assertThat(this.events.of(MobEvent.Dodged.class)).containsExactly(new MobEvent.Dodged("-", Damage.physical(1000)));
        assertThat(this.events.of(MobEvent.Hit.class)).isEmpty();
    }

    @Test
    void periodicDamageNamesTheEffectThatDealtIt() {
        DefinedEnemyMob mob = this.observed(this.world, plain());
        mob.applyEffect(Effect.burn(Damage.magic(100), 40, damage -> mob.takeHit(damage, PERIODIC)));

        mob.doTick(1);

        MobEvent.Hit hit = this.events.of(MobEvent.Hit.class).getFirst();
        assertThat(hit.steps().delivery()).isEqualTo(Delivery.PERIODIC);
        assertThat(hit.cause().source()).contains(EffectKind.BURN);
    }

    @Test
    void periodicDamageFromOutsideTheEffectsTickNamesNoEffect() {
        DefinedEnemyMob mob = this.observed(this.world, plain());
        mob.applyEffect(Effect.burn(Damage.magic(100), 40, damage -> mob.takeHit(damage, PERIODIC)));
        mob.doTick(1);

        mob.takeHit(Damage.magic(100), PERIODIC);

        assertThat(this.events.of(MobEvent.Hit.class).getLast().cause().source()).isEmpty();
    }

    @Test
    void resistanceShortensAnEffectAndTheTraceSaysByHowMuch() {
        DefinedEnemyMob mob = this.observed(this.world,
                plain().withTraits(List.of(EffectResistTrait.resisting(EffectKind.FREEZE, 0.5f))));

        mob.applyEffect(Effect.freeze(100, DamageSink.none()));

        assertThat(this.events.of(MobEvent.EffectRequested.class))
                .containsExactly(new MobEvent.EffectRequested(EffectKind.FREEZE, "-", 100));
        assertThat(this.events.of(MobEvent.EffectShortened.class))
                .containsExactly(new MobEvent.EffectShortened(EffectKind.FREEZE, 0.5f, 1f, 50));
        assertThat(this.events.changes(MobEvent.Change.GAINED).getFirst().after().remaining()).isEqualTo(OptionalInt.of(50));
    }

    @Test
    void anEffectThatWouldLastUnderATickIsReportedBlocked() {
        DefinedEnemyMob mob = this.observed(this.world,
                plain().withTraits(List.of(EffectResistTrait.resisting(EffectKind.FREEZE, 0.99f))));

        mob.applyEffect(Effect.freeze(1, DamageSink.none()));

        assertThat(this.events.of(MobEvent.EffectBlocked.class)).hasSize(1);
        assertThat(this.events.changes(MobEvent.Change.GAINED)).isEmpty();
    }

    @Test
    void anEffectKeptOutIsReportedUnchangedWithWhatKeepsItOut() {
        DefinedEnemyMob mob = this.observed(this.world, plain());
        mob.applyEffect(Effect.freeze(100, DamageSink.none()));

        mob.applyEffect(Effect.burn(Damage.magic(100), 40, DamageSink.none()));

        MobEvent.EffectChanged unchanged = this.events.changes(MobEvent.Change.UNCHANGED).getFirst();
        assertThat(unchanged.kind()).isEqualTo(EffectKind.BURN);
        assertThat(unchanged.cause()).isEqualTo("kept-out-by:" + EffectKind.FREEZE.title());
    }

    @Test
    void anEffectThatRunsOutEndsAsExpired() {
        DefinedEnemyMob mob = this.observed(this.world, plain());
        mob.applyEffect(Effect.revealed(2, DamageSink.none()));

        EnemyFixtures.tickEnemies(this.world, 1, 3);

        MobEvent.EffectChanged ended = this.events.changes(MobEvent.Change.ENDED).getFirst();
        assertThat(ended.kind()).isEqualTo(EffectKind.REVEALED);
        assertThat(ended.cause()).isEqualTo("expired");
    }

    @Test
    void reapplyingAnEffectRefreshesItsClock() {
        DefinedEnemyMob mob = this.observed(this.world, plain());
        mob.applyEffect(Effect.revealed(10, DamageSink.none()));
        EnemyFixtures.tickEnemies(this.world, 1, 5);

        mob.applyEffect(Effect.revealed(10, DamageSink.none()));

        MobEvent.EffectChanged refreshed = this.events.changes(MobEvent.Change.REFRESHED).getFirst();
        assertThat(refreshed.before().remaining()).isEqualTo(OptionalInt.of(5));
        assertThat(refreshed.after().remaining()).isEqualTo(OptionalInt.of(10));
    }

    @Test
    void anotherStackIsReportedAsTheCountChanging() {
        DefinedEnemyMob mob = this.observed(this.world, plain());
        mob.applyEffect(Effect.vulnerable(1, 100, DamageSink.none()));

        mob.applyEffect(Effect.vulnerable(1, 100, DamageSink.none()));

        MobEvent.EffectChanged stacks = this.events.changes(MobEvent.Change.STACKS).getFirst();
        assertThat(stacks.before().stacks()).isEqualTo(1);
        assertThat(stacks.after().stacks()).isEqualTo(2);
    }

    @Test
    void breakingAShieldReportsItWeakened() {
        DefinedEnemyMob mob = this.observed(this.world, plain());
        mob.applyEffect(Effect.shield(1000, 100, DamageSink.none()));

        mob.breakShield(0.5f);

        MobEvent.EffectChanged weakened = this.events.changes(MobEvent.Change.WEAKENED).getFirst();
        assertThat(weakened.before().level().getAsDouble()).isEqualTo(DamageUnits.inPoints(1000));
        assertThat(weakened.after().level().getAsDouble()).isEqualTo(DamageUnits.inPoints(500));
    }

    @Test
    void aShieldAHitDrainsShowsInTheHitNotAsAWeakenedShield() {
        DefinedEnemyMob mob = this.observed(this.world, plain());
        mob.applyEffect(Effect.shield(1000, 100, DamageSink.none()));

        mob.takeHit(Damage.physical(400), AttackProfile.none());

        assertThat(this.events.of(MobEvent.Hit.class).getFirst().outcome().absorbed()).isEqualTo(400);
        assertThat(this.events.changes(MobEvent.Change.WEAKENED)).isEmpty();
    }

    @Test
    void regenerationThatChangesHealthIsReported() {
        float pointsPerSecond = 20f;
        DefinedEnemyMob mob = this.observed(this.world, plain().withTraits(List.of(new RegenerationTrait(pointsPerSecond))));
        mob.takeHit(Damage.physical(1000), AttackProfile.none());

        mob.doTick(1);

        MobEvent.HealthChanged healed = this.events.of(MobEvent.HealthChanged.class).getFirst();
        assertThat(healed.cause()).isEqualTo("regen");
        assertThat(healed.after() - healed.before()).isEqualTo(DamageUnits.ofPoints(pointsPerSecond / TickRate.TICKS_PER_SECOND));
    }

    @Test
    void aKillingHitReportsWhatHealthCouldNotTake() {
        DefinedEnemyMob mob = this.observed(this.world, plain());

        mob.takeHit(Damage.physical(10_500), AttackProfile.none());

        assertThat(this.events.of(MobEvent.Killed.class)).containsExactly(new MobEvent.Killed("-", 500));
    }

    @Test
    void theTraceEndsOnlyAfterWhatItsDeathSetsOff() {
        this.world.getEnemyCatalog().register(EnemyFixtures.simpleDefinition("child"));
        DefinedEnemyMob mob = this.observed(this.world, EnemyFixtures.definitionThatSpawns("parent", "child"));
        mob.takeHit(Damage.physical(10_000), AttackProfile.none());

        mob.doTick(1);

        List<MobEvent> all = this.events.all();
        assertThat(all.get(all.size() - 2)).isInstanceOf(MobEvent.Spawned.class);
        assertThat(all.getLast()).isInstanceOfSatisfying(MobEvent.Ended.class, ended -> assertThat(ended.how()).isEqualTo("killed"));
    }

    @Test
    void whatAnObservedEnemySpawnsIsObservedToo() {
        this.world.getEnemyCatalog().register(EnemyFixtures.simpleDefinition("child"));
        DefinedEnemyMob mob = this.observed(this.world, EnemyFixtures.definitionThatSpawns("parent", "child"));
        mob.takeHit(Damage.physical(10_000), AttackProfile.none());

        mob.doTick(1);

        DefinedEnemyMob child = Arrays.stream(this.world.enemies().getEnemies()).map(m -> m.accept(defined -> defined))
                .filter(m -> m != mob).findFirst().orElseThrow();
        assertThat(child.isObserved()).isTrue();
        assertThat(this.events.of(MobEvent.Spawned.class)).containsExactly(new MobEvent.Spawned(List.of(child.id()), false));
    }

    @Test
    void aCastReportsItsRadiusInCellsAndHowManyItReached() {
        float radiusCells = 2f;
        DefinedEnemyMob caster = this.observed(this.world, plain().withAbilities(List.of(healing(radiusCells))));
        EnemyFixtures.plainMob(this.world, 100, 1);
        EnemyFixtures.plainMob(this.world, 100, 1);

        caster.doTick(1);

        assertThat(this.events.of(MobEvent.Cast.class)).containsExactly(new MobEvent.Cast(EffectKind.HEAL, radiusCells, 2));
    }

    @Test
    void anEffectAnotherEnemyCastsNamesThatEnemy() {
        DefinedEnemyMob caster = EnemyFixtures.liveMob(this.world, plain().withAbilities(List.of(healing(2f))));
        this.observed(this.world, plain());

        caster.doTick(1);

        assertThat(this.events.of(MobEvent.EffectRequested.class).getFirst().from()).isEqualTo("#" + caster.id());
    }

    @Test
    void aStoppedTraceEndsAndReportsNothingMore() {
        DefinedEnemyMob mob = this.observed(this.world, plain());

        this.world.enemyObservation().end(mob, "stopped");
        mob.takeHit(Damage.physical(1000), AttackProfile.none());

        assertThat(mob.isObserved()).isFalse();
        assertThat(this.events.all().getLast()).isInstanceOfSatisfying(MobEvent.Ended.class,
                ended -> assertThat(ended.how()).isEqualTo("stopped"));
    }

    @Test
    void theEndAddsUpWhatLandedBySourceAndType() {
        DefinedEnemyMob mob = this.observed(this.world, plain());
        mob.takeHit(Damage.physical(100), AttackProfile.none());
        mob.takeHit(Damage.physical(200), AttackProfile.none());
        mob.takeHit(Damage.magic(50), AttackProfile.none());

        this.world.enemyObservation().end(mob, "stopped");

        MobEvent.Ended ended = this.events.of(MobEvent.Ended.class).getFirst();
        assertThat(ended.totals().landed()).containsExactly(Map.entry("- physical", 300),
                Map.entry("- magic", 50));
    }

    private DefinedEnemyMob observed(GameWorld in, EnemyDefinition definition) {
        DefinedEnemyMob mob = EnemyFixtures.liveMob(in, definition);
        in.enemyObservation().observe(mob, this.events, "test");
        return mob;
    }

    /** Still, so a mob spawned beside it stays there. */
    private static EnemyDefinition plain() {
        return EnemyDefinition.of("plain", "Plain", 100, 1, 0f, BodyArchetype.CIRCLE);
    }

    /** Heals every other enemy within {@code radiusCells} every tick. */
    private static Ability healing(float radiusCells) {
        return new Ability(AbilityTerm.MEND, new PeriodicTrigger(1),
                new ApplyEffectAction(new HealTemplate(1f, 10), new RadiusTarget(radiusCells * BoardFixtures.SCALE)));
    }
}
