package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.EffectTemplate;
import td.effect.HealTemplate;
import td.effect.InvisibleTemplate;
import td.effect.ShieldTemplate;
import td.effect.SprintTemplate;
import td.fixtures.BoardFixtures;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abilities cast by a live mob: when it sees the moment each trigger waits for, and where each action lands.
 * Whether a trigger fires for a given moment is {@code AbilityEvaluatorTest}'s.
 */
class DefinedEnemyMobAbilityTest {

    private static final float RADIUS = 80f;
    private static final int CAST_TICK = 3;
    private static final int HATCH_TICK = 40;
    private static final Damage LETHAL = Damage.physical(1_000_000_000);
    private static final EnemyDefinition ALLY = EnemyFixtures.simpleDefinition("ally");

    private final GameWorld world = WorldFixtures.newWorldOnStraightPath();

    private static EnemyDefinition casting(String id, AbilityTrigger trigger, AbilityAction action) {
        return EnemyFixtures.simpleDefinition(id).withAbilities(List.of(new Ability(AbilityTerm.GUARD, trigger, action)));
    }

    private static ApplyEffectAction healAround() {
        return new ApplyEffectAction(new HealTemplate(1f, 20), new RadiusTarget(RADIUS));
    }

    private static ApplyEffectAction onItself(EffectTemplate template) {
        return new ApplyEffectAction(template, new SelfTarget());
    }

    private static DefinedEnemyMob wounded(DefinedEnemyMob mob) {
        mob.doDamage(Damage.physical(mob.getHealth() / 2));
        return mob;
    }

    @Test
    void aRadiusAbilityReachesTheAlliesWithinItsRadiusAndNoOneBeyond() {
        EnemyFixtures.liveMob(this.world, casting("healer", new PeriodicTrigger(CAST_TICK), healAround()));
        DefinedEnemyMob near = wounded(EnemyFixtures.liveMob(this.world, ALLY));
        DefinedEnemyMob far = EnemyFixtures.liveMob(this.world, ALLY);
        far.jumpToDistance(2 * RADIUS);
        wounded(far);
        int nearWounded = near.getHealth();
        int farWounded = far.getHealth();

        EnemyFixtures.tickEnemies(this.world, 1, CAST_TICK + 1);

        assertThat(near.getHealth()).isGreaterThan(nearWounded);
        assertThat(far.getHealth()).isEqualTo(farWounded);
    }

    @Test
    void aRadiusAbilityNeverReachesItsCaster() {
        DefinedEnemyMob healer = wounded(
                EnemyFixtures.liveMob(this.world, casting("healer", new PeriodicTrigger(CAST_TICK), healAround())));
        int woundedHealth = healer.getHealth();

        EnemyFixtures.tickEnemies(this.world, 1, CAST_TICK + 1);

        assertThat(healer.getHealth()).isEqualTo(woundedHealth);
    }

    @Test
    void aCastIsRecordedWithItsKindRadiusAndTickAndASelfCastHasNoRadius() {
        DefinedEnemyMob aroundCaster = EnemyFixtures.liveMob(this.world,
                casting("warder", new PeriodicTrigger(CAST_TICK), new ApplyEffectAction(new ShieldTemplate(10f, 100),
                        new RadiusTarget(RADIUS))));
        DefinedEnemyMob selfCaster = EnemyFixtures.liveMob(this.world,
                casting("vanisher", new PeriodicTrigger(CAST_TICK), onItself(new InvisibleTemplate(100))));
        boolean recordedBeforeCasting = aroundCaster.lastAbilityCast().isPresent();

        EnemyFixtures.tickEnemies(this.world, 1, CAST_TICK);

        assertThat(recordedBeforeCasting).isFalse();
        assertThat(aroundCaster.lastAbilityCast()).contains(new AbilityCast(EffectKind.SHIELD, RADIUS, CAST_TICK));
        assertThat(selfCaster.lastAbilityCast()).contains(new AbilityCast(EffectKind.INVISIBLE, 0f, CAST_TICK));
    }

    @Test
    void aFirstHitAbilityIsCastOnTheMobsTickAfterTheHit() {
        DefinedEnemyMob mob = EnemyFixtures.liveMob(this.world,
                casting("vanisher", new OnFirstDamageTakenTrigger(), onItself(new InvisibleTemplate(100))));
        mob.doDamage(Damage.physical(10));
        boolean castOnTheHit = mob.hasEffect(EffectKind.INVISIBLE);

        mob.doTick(1);

        assertThat(castOnTheHit).isFalse();
        assertThat(mob.hasEffect(EffectKind.INVISIBLE)).isTrue();
    }

    @Test
    void aCritTakenAbilityIsCastOnceTheMobSurvivesACrit() {
        DefinedEnemyMob mob = EnemyFixtures.liveMob(this.world,
                casting("bracer", new OnCriticalHitTakenTrigger(), onItself(new ShieldTemplate(10f, 100))));

        mob.doDamage(Damage.physical(100), AttackProfile.critChance(1f));
        mob.doTick(1);

        assertThat(mob.hasEffect(EffectKind.SHIELD)).isTrue();
    }

    @Test
    void aDodgeAbilityIsCastOnceTheMobDodges() {
        GameWorld everyRollSucceeds = WorldFixtures.newWorldOnStraightPath(() -> 0.0);
        DefinedEnemyMob mob = EnemyFixtures.liveMob(everyRollSucceeds,
                casting("skirmisher", new OnDodgeTrigger(), onItself(new SprintTemplate(20))).withAgility(50f));

        boolean dodged = mob.takeHit(Damage.physical(100), AttackProfile.none().asAimed()).dodged();
        mob.doTick(1);

        assertThat(dodged).isTrue();
        assertThat(mob.hasEffect(EffectKind.SPRINTING)).isTrue();
    }

    @Test
    void aThresholdAbilityIsCastOnceHealthFallsBelowItsShare() {
        DefinedEnemyMob mob = EnemyFixtures.liveMob(this.world,
                casting("bracer", new HealthThresholdTrigger(0.5f), onItself(new ShieldTemplate(10f, 100))));
        int full = mob.getHealth();
        mob.doDamage(Damage.physical(full * 4 / 10));
        mob.doTick(1);
        boolean castAboveHalf = mob.hasEffect(EffectKind.SHIELD);

        mob.doDamage(Damage.physical(full * 2 / 10));
        mob.doTick(2);

        assertThat(castAboveHalf).isFalse();
        assertThat(mob.hasEffect(EffectKind.SHIELD)).isTrue();
    }

    @Test
    void aDeathAbilityIsCastOnTheTickTheMobDies() {
        DefinedEnemyMob martyr = EnemyFixtures.liveMob(this.world, casting("martyr", new OnDeathTrigger(), healAround()));
        DefinedEnemyMob ally = wounded(EnemyFixtures.liveMob(this.world, ALLY));

        martyr.doDamage(LETHAL);
        martyr.doTick(1);

        assertThat(ally.hasEffect(EffectKind.HEAL)).isTrue();
    }

    @Test
    void aMobThatLeaksCastsNoDeathAbility() {
        this.world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(40, 0))));
        this.world.getEnemyCatalog().register(EnemyFixtures.simpleDefinition("child"));
        DefinedEnemyMob parent = EnemyFixtures.liveMob(this.world, EnemyFixtures.definitionThatSpawns("parent", "child"));

        EnemyFixtures.tickEnemies(this.world, 1, 200);

        assertThat(parent.fate()).isEqualTo(EnemyInspection.Fate.LEAKED);
        assertThat(this.world.enemies().getEnemies()).containsExactly(parent);
    }

    @Test
    void whatADeathAbilitySpawnsAppearsWhereTheMobDied() {
        this.world.getEnemyCatalog().register(EnemyFixtures.simpleDefinition("child"));
        DefinedEnemyMob parent = EnemyFixtures.liveMob(this.world, EnemyFixtures.definitionThatSpawns("parent", "child"));
        EnemyFixtures.tickEnemies(this.world, 1, 10);

        parent.doDamage(LETHAL);
        parent.doTick(11);

        assertThat(this.world.enemies().getEnemies()).filteredOn(mob -> mob != parent).singleElement()
                .satisfies(child -> {
                    assertThat(child.getX()).isEqualTo(parent.getX());
                    assertThat(child.getY()).isEqualTo(parent.getY());
                });
    }

    @Test
    void aSpawnThatConsumesItsCasterReplacesItWhereItStood() {
        DefinedEnemyMob egg = this.eggOf("hatchling");

        EnemyFixtures.tickEnemies(this.world, 1, HATCH_TICK);

        assertThat(this.world.enemies().getEnemies()).singleElement()
                .satisfies(hatched -> {
                    assertThat(((DefinedEnemyMob) hatched).definition().id()).isEqualTo("hatchling");
                    assertThat(hatched.getX()).isEqualTo(egg.getX());
                });
    }

    @Test
    void aCasterKilledBeforeItsSpawnIsDueNeverSpawns() {
        DefinedEnemyMob egg = this.eggOf("hatchling");
        EnemyFixtures.tickEnemies(this.world, 1, HATCH_TICK / 2);

        egg.doDamage(LETHAL);
        EnemyFixtures.tickEnemies(this.world, HATCH_TICK / 2 + 1, 2 * HATCH_TICK);

        assertThat(this.world.enemies().getEnemies()).containsExactly(egg);
    }

    @Test
    void aSelectedCasterThatIsReplacedKeepsItsSnapshotMarkedHatched() {
        DefinedEnemyMob egg = this.eggOf("hatchling");
        EnemySelection selection = new EnemySelection();
        selection.requestAt((int) egg.getX(), (int) egg.getY());
        selection.resolve(this.world.enemies());

        EnemyFixtures.tickEnemies(this.world, 1, HATCH_TICK);

        assertThat(selection.resolve(this.world.enemies())).map(EnemyInspection::fate)
                .contains(EnemyInspection.Fate.HATCHED);
        assertThat(selection.selectedAlive()).isEmpty();
    }

    @Test
    void aSpawnAtAFixedRankComesAtThatRankWhateverTheCastersRank() {
        this.world.getEnemyCatalog().register(EnemyFixtures.simpleDefinition("guard"));
        DefinedEnemyMob caller = EnemyFixtures.liveMob(this.world,
                casting("caller", new OnceTrigger(1), new SpawnEnemiesAction("guard", 1, false).atRank(Rank.ELITE)));

        caller.doTick(1);

        assertThat(this.world.enemies().getEnemies()).filteredOn(mob -> mob != caller)
                .extracting(EnemyMob::getRank).containsExactly(Rank.ELITE);
    }

    @Test
    void aZoneAbilityLeavesItsZoneWhereTheCasterStandsAsWideAsItsCells() {
        DefinedEnemyMob blinder = EnemyFixtures.liveMob(this.world,
                casting("blinder", new OnDeathTrigger(), LeaveZoneAction.blindingField(2f, 0.5f)));
        blinder.jumpToDistance(300);
        float cell = BoardFixtures.SCALE;

        blinder.doDamage(LETHAL);
        blinder.doTick(1);

        assertThat(this.world.zones().disruptionAt(blinder.getX() + 1.9 * cell, blinder.getY()).missChance())
                .isEqualTo(0.5f);
        assertThat(this.world.zones().disruptionAt(blinder.getX() + 2.1 * cell, blinder.getY()).isNone()).isTrue();
    }

    @Test
    void aHealingGroundAbilityLeavesGroundThatHealsByItsPower() {
        DefinedEnemyMob healer = EnemyFixtures.liveMob(this.world,
                casting("pooler", new OnceTrigger(1), LeaveZoneAction.healingGround(1f, 100, 0.5f)));
        DefinedEnemyMob ally = wounded(EnemyFixtures.liveMob(this.world, ALLY));

        healer.doTick(1);
        this.world.zones().doTick(2);

        assertThat(ally.activeEffects()).filteredOn(effect -> effect.kind() == EffectKind.HEAL)
                .extracting(Effect::healPerTick).containsExactly(DamageUnits.ofPoints(0.5f));
    }

    /** An egg that hatches into {@code hatchlingId} at {@link #HATCH_TICK}, replacing itself. */
    private DefinedEnemyMob eggOf(String hatchlingId) {
        this.world.getEnemyCatalog().register(EnemyFixtures.simpleDefinition(hatchlingId));
        return EnemyFixtures.liveMob(this.world,
                casting("egg", new OnceTrigger(HATCH_TICK), new SpawnEnemiesAction(hatchlingId, 1, true)));
    }
}
