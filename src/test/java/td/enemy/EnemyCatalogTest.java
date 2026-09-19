package td.enemy;

import org.junit.jupiter.api.Test;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameStartupException;
import td.util.GameWorld;

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
    void cloneAndAdjustRegistersAnIndependentDefinitionUnderTheNewIdWithoutMutatingTheOriginal() {
        EnemyCatalog catalog = new EnemyCatalog();
        EnemyDefinition square = EnemyDefinition.of("s", "Square", 100, 5, 1.28f, BodyArchetype.SQUARE)
                .withMovement(new RotorMovement(0.1f));
        catalog.register(square);

        EnemyDefinition tankySquare = catalog.cloneAndAdjust("s", "tankySquare",
                d -> d.withHealthDivisor(d.healthDivisor() / 2f));

        assertThat(catalog.get("s").healthDivisor()).isEqualTo(1f);
        assertThat(catalog.get("tankySquare")).isEqualTo(tankySquare);
        assertThat(tankySquare.id()).isEqualTo("tankySquare");
        assertThat(tankySquare.healthDivisor()).isEqualTo(0.5f);
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

        EnemyMob enemy = catalog.spawn("c", gameWorld, 0, 50, 3, 1);

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
