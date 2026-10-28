package td.enemy;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
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
        assertThat(catalog.get("s", Rank.GRUNT).baseHealth()).isEqualTo(100);
        assertThat(catalog.get("s", Rank.SOLDIER).baseHealth()).isEqualTo(200);
    }

    @Test
    void theRankAwareOverloadCanSetHealthAndStatsCompletelyPersistAddAndReplaceTraitsPerRank() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(RankedEnemy.startingAt(EnemyFixtures.simpleDefinition("square").withArmor(10f)
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.named("freeze", EffectResistTrait.resisting(EffectKind.FREEZE, 0.2f)),
                                IdentifiedTrait.named("chill", EffectResistTrait.resisting(EffectKind.CHILL, 0.25f)))))
                .thenAt(Rank.SOLDIER, d -> d)
                .thenAt(Rank.VETERAN, d -> d)
                .thenAt(Rank.ELITE, d -> d)
                .build());

        RankedEnemy toughenedSquare = catalog.cloneAndAdjust("square", "toughenedSquare", (rank, d) -> switch (rank) {
            // set outright, proving a replacement rather than a delta
            case GRUNT -> d.withHealthAndPrice(1000, 100);
            case SOLDIER, BOSS -> d;
            case VETERAN -> d.withAdditionalTraits(
                    List.of(IdentifiedTrait.named("regrowth", new RegenerationTrait(1f))));
            // replaces "freeze" under the same id; other traits stay
            case ELITE -> d.withArmor(233f).withAdditionalTraits(
                    List.of(IdentifiedTrait.named("freeze", EffectResistTrait.immuneTo(EffectKind.FREEZE))));
        });

        EnemyDefinition grunt = toughenedSquare.definitionFor(Rank.GRUNT);
        assertThat(grunt.baseHealth()).isEqualTo(1000);
        assertThat(grunt.price()).isEqualTo(100);

        EnemyDefinition soldier = toughenedSquare.definitionFor(Rank.SOLDIER);
        assertThat(soldier.baseStats().value(EnemyStat.ARMOR)).isEqualTo(10f);
        assertThat(traitNamed(soldier, "freeze")).contains(EffectResistTrait.resisting(EffectKind.FREEZE, 0.2f));

        EnemyDefinition veteran = toughenedSquare.definitionFor(Rank.VETERAN);
        assertThat(veteran.traitSlots()).hasSize(3);
        assertThat(traitNamed(veteran, "regrowth")).contains(new RegenerationTrait(1f));

        EnemyDefinition elite = toughenedSquare.definitionFor(Rank.ELITE);
        assertThat(elite.baseStats().value(EnemyStat.ARMOR)).isEqualTo(233f);
        assertThat(elite.traitSlots()).hasSize(2);
        assertThat(traitNamed(elite, "freeze")).contains(EffectResistTrait.immuneTo(EffectKind.FREEZE));
        assertThat(traitNamed(elite, "chill")).contains(EffectResistTrait.resisting(EffectKind.CHILL, 0.25f));
    }

    private static Optional<TraitTemplate> traitNamed(EnemyDefinition definition, String name) {
        return definition.traitSlots().stream()
                .filter(slot -> slot.id().equals(TraitId.named(name)))
                .map(IdentifiedTrait::template)
                .findFirst();
    }

    @Test
    void aFiniteMultiHopSpawnChainRegistersSuccessfully() {
        EnemyCatalog catalog = new EnemyCatalog();
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
    void aUniqueEnemySpawnsAsABossWhateverRankIsAskedFor() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.registerUnique(EnemyFixtures.simpleDefinition("warlord"));

        EnemyMob spawned = catalog.spawn("warlord", WorldFixtures.newWorld(), 0, 100, 5, Rank.GRUNT);

        assertThat(spawned.getRank()).isEqualTo(Rank.BOSS);
    }

    @Test
    void aUniqueEnemyThatLeaksTakesEveryLife() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.registerUnique(EnemyFixtures.simpleDefinition("warlord"));

        assertThat(catalog.ranked("warlord").definitionFor(Rank.BOSS).livesOnLeak()).isEqualTo(EnemyDefinition.ALL_LIVES);
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
    void builtInReturnsAFreshCatalogEachCallNotASharedSingleton() {
        EnemyCatalog first = EnemyCatalog.builtIn();
        first.register(EnemyFixtures.simpleDefinition("custom"));

        EnemyCatalog second = EnemyCatalog.builtIn();

        assertThat(second.contains("custom")).isFalse();
    }

    @Test
    void registeringAnIdShapedLikeASpacingTokenFails() {
        EnemyCatalog catalog = new EnemyCatalog();

        assertThatThrownBy(() -> catalog.register(EnemyFixtures.simpleDefinition("w30")))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("w30");
    }

    @Test
    void registeringAnIdThatCollidesWithAReservedWaveScriptTokenFails() {
        EnemyCatalog catalog = new EnemyCatalog();

        assertThatThrownBy(() -> catalog.register(EnemyFixtures.simpleDefinition("swarm")))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("swarm");
    }
}
