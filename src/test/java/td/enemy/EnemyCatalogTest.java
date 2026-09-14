package td.enemy;

import org.junit.jupiter.api.Test;
import td.util.GameStartupException;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnemyCatalogTest {

    private static EnemyDefinition simpleDefinition(String id) {
        return new EnemyDefinition(id, id, "", 100, 5, 1.28f, 1f, EnemyMob.type.Normal,
                BodyArchetype.CIRCLE, new FixedMovement(), List.of(), List.of());
    }

    /** A minimal definition whose only ability spawns {@code spawnedId} on death. */
    private static EnemyDefinition definitionThatSpawns(String id, String spawnedId) {
        Ability spawnOnDeath = new Ability(new OnDeathTrigger(), new SpawnEnemiesAction(spawnedId, 1, false));
        return new EnemyDefinition(id, id, "", 100, 5, 1.28f, 1f, EnemyMob.type.Normal,
                BodyArchetype.CIRCLE, new FixedMovement(), List.of(), List.of(spawnOnDeath));
    }

    @Test
    void aRegisteredDefinitionIsRetrievableByItsId() {
        EnemyCatalog catalog = new EnemyCatalog();
        EnemyDefinition circle = simpleDefinition("c");

        catalog.register(circle);

        assertThat(catalog.contains("c")).isTrue();
        assertThat(catalog.get("c")).isEqualTo(circle);
    }

    @Test
    void registeringADuplicateIdThrows() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(simpleDefinition("c"));

        assertThatThrownBy(() -> catalog.register(simpleDefinition("c")))
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
        EnemyDefinition square = new EnemyDefinition("s", "Square", "", 100, 5, 1.28f, 1f, EnemyMob.type.Normal,
                BodyArchetype.SQUARE, new RotorMovement(0.1f), List.of(), List.of());
        catalog.register(square);

        // "a Square with double the usual resistance for this one level" - the feature request's own example
        EnemyDefinition tankySquare = catalog.cloneAndAdjust("s", "tankySquare",
                d -> new EnemyDefinition(d.id(), d.displayName(), d.description(), d.baseHealth(), d.price(),
                        d.baseSpeed(), d.healthDivisor() / 2f,
                        d.mobType(), d.archetype(), d.movement(), d.traits(), d.abilities()));

        assertThat(catalog.get("s").healthDivisor()).isEqualTo(1f);
        assertThat(catalog.get("tankySquare")).isEqualTo(tankySquare);
        assertThat(tankySquare.id()).isEqualTo("tankySquare");
        assertThat(tankySquare.healthDivisor()).isEqualTo(0.5f);
    }

    @Test
    void aFiniteMultiHopSpawnChainRegistersSuccessfully() {
        EnemyCatalog catalog = new EnemyCatalog();
        // warden1 -> egg1 -> warden2 -> egg2 -> warden3 (terminal, no spawn ability of its own)
        catalog.register(simpleDefinition("warden3"));
        catalog.register(definitionThatSpawns("egg2", "warden3"));
        catalog.register(definitionThatSpawns("warden2", "egg2"));
        catalog.register(definitionThatSpawns("egg1", "warden2"));

        assertThatCode(() -> catalog.register(definitionThatSpawns("warden1", "egg1"))).doesNotThrowAnyException();
    }

    @Test
    void aDefinitionThatDirectlySpawnsItselfIsRejected() {
        EnemyCatalog catalog = new EnemyCatalog();

        assertThatThrownBy(() -> catalog.register(definitionThatSpawns("selfSpawner", "selfSpawner")))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aDefinitionThatTransitivelySpawnsItselfIsRejected() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(definitionThatSpawns("a", "b")); // fine on its own - "b" doesn't exist yet

        assertThatThrownBy(() -> catalog.register(definitionThatSpawns("b", "a")))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void spawnBuildsALiveMobFromTheRegisteredDefinition() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(simpleDefinition("c"));
        GameWorld gameWorld = new GameWorld(new RecordingGameHost());

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
    void builtInReturnsAFreshCatalogEachCallNotASharedSingleton() {
        EnemyCatalog first = EnemyCatalog.builtIn();
        first.register(simpleDefinition("custom"));

        EnemyCatalog second = EnemyCatalog.builtIn();

        assertThat(second.contains("custom")).isFalse();
    }
}
