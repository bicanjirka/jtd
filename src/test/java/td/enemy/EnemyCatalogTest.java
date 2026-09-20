package td.enemy;

import org.junit.jupiter.api.Test;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameStartupException;
import td.util.GameWorld;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnemyCatalogTest {

    @Test
    void aRegisteredDefinitionIsRetrievableByItsId() {
        EnemyCatalog catalog = new EnemyCatalog();
        EnemyDefinition definition = EnemyFixtures.simpleDefinition("c");

        catalog.register(definition);

        assertThat(catalog.contains("c")).isTrue();
        assertThat(catalog.get("c")).isEqualTo(definition);
    }

    @Test
    void registeringADuplicateIdThrows() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(EnemyFixtures.simpleDefinition("c"));

        assertThatThrownBy(() -> catalog.register(EnemyFixtures.simpleDefinition("c")))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void gettingAnUnregisteredIdThrows() {
        EnemyCatalog catalog = new EnemyCatalog();

        assertThatThrownBy(() -> catalog.get("nope")).isInstanceOf(GameStartupException.class);
    }

    @Test
    void cloneAndAdjustRegistersAnIndependentSingleRankLadderUnderTheNewIdWithoutMutatingTheOriginal() {
        EnemyCatalog catalog = new EnemyCatalog();
        EnemyDefinition square = EnemyDefinition.of("s", "Square", 100, 5, 1.28f, BodyArchetype.SQUARE)
                .withMovement(new RotorMovement(0.1f));
        catalog.register(square);

        RankedEnemy tankySquare = catalog.cloneAndAdjust("s", "tankySquare",
                d -> d.withHealthDivisor(d.healthDivisor() / 2f));

        assertThat(catalog.get("s").healthDivisor()).isEqualTo(1f);
        assertThat(catalog.get("tankySquare")).isEqualTo(tankySquare.definitionFor(Rank.GRUNT));
        assertThat(tankySquare.id()).isEqualTo("tankySquare");
        assertThat(tankySquare.definitionFor(Rank.GRUNT).healthDivisor()).isEqualTo(0.5f);
    }

    @Test
    void cloneAndAdjustAppliesTheAdjustmentToEveryRankTheOriginalLadderAuthored() {
        EnemyCatalog catalog = new EnemyCatalog();
        RankedEnemy square = RankedEnemy.startingAt(EnemyDefinition.of("s", "Square", 100, 5, 1.28f, BodyArchetype.SQUARE))
                .thenAt(Rank.SOLDIER, d -> d.withHealthAndPrice(200, 8))
                .build();
        catalog.register(square);

        RankedEnemy tankySquare = catalog.cloneAndAdjust("s", "tankySquare",
                d -> d.withHealthAndPrice(d.baseHealth() * 2, d.price()));

        assertThat(tankySquare.definitionFor(Rank.GRUNT).baseHealth()).isEqualTo(200);
        assertThat(tankySquare.definitionFor(Rank.SOLDIER).baseHealth()).isEqualTo(400);
        // the original ladder's own two ranks are untouched
        assertThat(catalog.get("s", Rank.GRUNT).baseHealth()).isEqualTo(100);
        assertThat(catalog.get("s", Rank.SOLDIER).baseHealth()).isEqualTo(200);
    }

    @Test
    void cloneAndAdjustOfABuiltInFiveRankLadderProducesADistinctAdjustedValuePerRank() {
        EnemyCatalog catalog = EnemyCatalog.builtIn();

        RankedEnemy toughCircle = catalog.cloneAndAdjust("c", "toughCircle",
                d -> d.withHealthAndPrice(d.baseHealth() + 50, d.price() + 5));

        // "c"'s own five ranks (50/2, 100/5, 200/13, 400/33, 800/83 - see BuiltInEnemies.SIMPLE)
        // each pick up the same +50/+5 adjustment, so the clone's own five ranks land on five
        // different values too - the adjustment is uniform, but what it's applied to isn't.
        assertThat(toughCircle.definitionFor(Rank.GRUNT).baseHealth()).isEqualTo(100);
        assertThat(toughCircle.definitionFor(Rank.SOLDIER).baseHealth()).isEqualTo(150);
        assertThat(toughCircle.definitionFor(Rank.VETERAN).baseHealth()).isEqualTo(250);
        assertThat(toughCircle.definitionFor(Rank.ELITE).baseHealth()).isEqualTo(450);
        assertThat(toughCircle.definitionFor(Rank.BOSS).baseHealth()).isEqualTo(850);
        assertThat(toughCircle.definitionFor(Rank.GRUNT).price()).isEqualTo(7);
        assertThat(toughCircle.definitionFor(Rank.BOSS).price()).isEqualTo(88);

        // every rank's own id is renamed, and every rank of the original "c" is untouched
        for (Rank rank : Rank.values()) {
            assertThat(toughCircle.definitionFor(rank).id()).isEqualTo("toughCircle");
        }
        assertThat(catalog.get("c", Rank.GRUNT).baseHealth()).isEqualTo(50);
        assertThat(catalog.get("c", Rank.BOSS).baseHealth()).isEqualTo(800);

        // Elite/Boss's own "armor" trait (added, then strengthened, on top of the ladder's
        // health/price progression) survives the clone unchanged - cloneAs copies each rank's
        // whole definition, traits included, not just the two stats adjust touches here.
        assertThat(toughCircle.definitionFor(Rank.SOLDIER).traits()).isEmpty();
        assertThat(toughCircle.definitionFor(Rank.ELITE).traits()).hasSize(1);
        assertThat(toughCircle.definitionFor(Rank.BOSS).traits()).hasSize(1);
    }

    @Test
    void theRankAwareOverloadCanSetHealthCompletelyPersistAddAndReplaceTraitsPerRank() {
        // ARMORED ("s") carries two named traits from the start - "resist" (PercentResistTrait)
        // and "criticalImmune" (CriticalImmunityTrait) - see BuiltInEnemies.ARMORED.
        EnemyCatalog catalog = EnemyCatalog.builtIn();

        RankedEnemy toughenedSquare = catalog.cloneAndAdjust("s", "toughenedSquare", (rank, d) -> switch (rank) {
            // Health/price set outright, not derived from d - proves each rank's adjustment is
            // a real replacement, not a delta on whatever that rank already had.
            case GRUNT -> d.withHealthAndPrice(1000, 100);
            // Untouched: the old "resist" (already strengthened to 0.75 at this rank - see
            // BuiltInEnemies.ARMORED)/"criticalImmune" traits must survive as-is.
            case SOLDIER -> d;
            // A new trait added alongside the two existing ones.
            case VETERAN -> d.withAdditionalTraits(
                    List.of(IdentifiedTrait.named("goldShield", new PercentResistTrait(0.5f))));
            // The old "resist" trait replaced by a stronger one under the same id -
            // "criticalImmune" and "flatResist" (added at this rank - see BuiltInEnemies.ARMORED)
            // are left alone.
            case ELITE -> d.withAdditionalTraits(
                    List.of(IdentifiedTrait.named("resist", new PercentResistTrait(0.3f))));
            case BOSS -> d;
        });

        EnemyDefinition grunt = toughenedSquare.definitionFor(Rank.GRUNT);
        assertThat(grunt.baseHealth()).isEqualTo(1000);
        assertThat(grunt.price()).isEqualTo(100);

        EnemyDefinition soldier = toughenedSquare.definitionFor(Rank.SOLDIER);
        assertThat(traitNamed(soldier, "resist")).contains(new PercentResistTrait(0.75f));
        assertThat(traitNamed(soldier, "criticalImmune")).contains(new CriticalImmunityTrait());

        EnemyDefinition veteran = toughenedSquare.definitionFor(Rank.VETERAN);
        assertThat(veteran.traitSlots()).hasSize(3);
        assertThat(traitNamed(veteran, "resist")).contains(new PercentResistTrait(0.7f));
        assertThat(traitNamed(veteran, "criticalImmune")).contains(new CriticalImmunityTrait());
        assertThat(traitNamed(veteran, "goldShield")).contains(new PercentResistTrait(0.5f));

        EnemyDefinition elite = toughenedSquare.definitionFor(Rank.ELITE);
        assertThat(elite.traitSlots()).hasSize(3);
        assertThat(traitNamed(elite, "resist")).contains(new PercentResistTrait(0.3f));
        assertThat(traitNamed(elite, "criticalImmune")).contains(new CriticalImmunityTrait());
        assertThat(traitNamed(elite, "flatResist")).contains(new FlatResistTrait(80));
    }

    private static Optional<Trait> traitNamed(EnemyDefinition definition, String name) {
        return definition.traitSlots().stream()
                .filter(slot -> slot.id().equals(TraitId.named(name)))
                .map(IdentifiedTrait::trait)
                .findFirst();
    }

    @Test
    void aFiniteMultiHopSpawnChainRegistersSuccessfully() {
        EnemyCatalog catalog = new EnemyCatalog();
        // warden1 -> egg1 -> warden2 -> egg2 -> warden3 (terminal, no spawn ability of its own)
        catalog.register(EnemyFixtures.simpleDefinition("warden3"));
        catalog.register(EnemyFixtures.definitionThatSpawns("egg2", "warden3"));
        catalog.register(EnemyFixtures.definitionThatSpawns("warden2", "egg2"));
        catalog.register(EnemyFixtures.definitionThatSpawns("egg1", "warden2"));

        assertThatCode(() -> catalog.register(EnemyFixtures.definitionThatSpawns("warden1", "egg1")))
                .doesNotThrowAnyException();
    }

    @Test
    void aDefinitionThatDirectlySpawnsItselfIsRejected() {
        EnemyCatalog catalog = new EnemyCatalog();

        assertThatThrownBy(() -> catalog.register(EnemyFixtures.definitionThatSpawns("selfSpawner", "selfSpawner")))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aDefinitionThatTransitivelySpawnsItselfIsRejected() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(EnemyFixtures.definitionThatSpawns("a", "b")); // fine on its own - "b" doesn't exist yet

        assertThatThrownBy(() -> catalog.register(EnemyFixtures.definitionThatSpawns("b", "a")))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void spawnBuildsALiveMobFromTheRegisteredDefinition() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(EnemyFixtures.simpleDefinition("c"));
        GameWorld gameWorld = WorldFixtures.newWorld();

        EnemyMob enemy = catalog.spawn("c", gameWorld, 0, 50, 3, Rank.GRUNT);

        assertThat(enemy).isInstanceOf(DefinedEnemyMob.class);
        assertThat(enemy.getHealth()).isEqualTo(5000);
    }

    @Test
    void builtInPreRegistersTheFourBasicBuiltInsUnderTheirWaveScriptLetters() {
        EnemyCatalog catalog = EnemyCatalog.builtIn();

        assertThat(catalog.contains("c")).isTrue();
        assertThat(catalog.contains("s")).isTrue();
        assertThat(catalog.contains("t")).isTrue();
        assertThat(catalog.contains("g")).isTrue();
    }

    @Test
    void builtInPreRegistersTheFullWardenBossChain() {
        EnemyCatalog catalog = EnemyCatalog.builtIn();

        assertThat(catalog.contains("warden1")).isTrue();
        assertThat(catalog.contains("wardenEgg1")).isTrue();
        assertThat(catalog.contains("warden2")).isTrue();
        assertThat(catalog.contains("wardenEgg2")).isTrue();
        assertThat(catalog.contains("warden3")).isTrue();
        assertThat(catalog.contains("wardenEgg3")).isTrue();
    }

    @Test
    void builtInPreRegistersTheFrenziedBossSSpawnlingEvenThoughNoWaveScriptSpawnsItDirectly() {
        EnemyCatalog catalog = EnemyCatalog.builtIn();

        assertThat(catalog.contains("tSpawn")).isTrue();
        assertThat(catalog.get("tSpawn")).isNotNull();
    }

    @Test
    void idsListsEveryRegisteredIdInRegistrationOrder() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(EnemyFixtures.simpleDefinition("first"));
        catalog.register(EnemyFixtures.simpleDefinition("second"));
        catalog.register(EnemyFixtures.simpleDefinition("third"));

        assertThat(catalog.ids()).containsExactly("first", "second", "third");
    }

    @Test
    void idsIncludesALaterRegistrationAddedAfterBuiltIn() {
        EnemyCatalog catalog = EnemyCatalog.builtIn();

        catalog.register(EnemyFixtures.simpleDefinition("custom"));

        assertThat(catalog.ids()).contains("c", "s", "t", "g", "warden1").endsWith("custom");
    }

    @Test
    void builtInReturnsAFreshCatalogEachCallNotASharedSingleton() {
        EnemyCatalog first = EnemyCatalog.builtIn();
        first.register(EnemyFixtures.simpleDefinition("custom"));

        EnemyCatalog second = EnemyCatalog.builtIn();

        assertThat(second.contains("custom")).isFalse();
    }

    @Test
    void registeringAnIdThatCollidesWithAReservedWaveScriptTokenFails() {
        EnemyCatalog catalog = new EnemyCatalog();

        assertThatThrownBy(() -> catalog.register(EnemyFixtures.simpleDefinition("swarm")))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("swarm");
    }
}
