package td.enemy;

import org.junit.jupiter.api.Test;
import td.util.GameStartupException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnemyCatalogTest {

    private static EnemyDefinition simpleDefinition(String id) {
        return new EnemyDefinition(id, id, 100, 1.28f, 5, BodyArchetype.CIRCLE, MovementBehavior.FIXED, List.of(), List.of());
    }

    /** A minimal definition whose only ability spawns {@code spawnedId} on death. */
    private static EnemyDefinition definitionThatSpawns(String id, String spawnedId) {
        Ability spawnOnDeath = new Ability(new OnDeathTrigger(), new SpawnEnemiesAction(spawnedId, 1, false));
        return new EnemyDefinition(id, id, 100, 1.28f, 5, BodyArchetype.CIRCLE, MovementBehavior.FIXED, List.of(), List.of(spawnOnDeath));
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
        EnemyDefinition square = new EnemyDefinition("s", "Square", 100, 1.28f, 5,
                BodyArchetype.SQUARE, MovementBehavior.ROTOR, List.of(), List.of());
        catalog.register(square);

        EnemyDefinition tankySquare = catalog.cloneAndAdjust("s", "tankySquare",
                d -> new EnemyDefinition(d.id(), d.displayName(), d.baseHealth() * 2, d.baseSpeed(),
                        d.price(), d.archetype(), d.movement(), d.traits(), d.abilities()));

        assertThat(catalog.get("s").baseHealth()).isEqualTo(100);
        assertThat(catalog.get("tankySquare")).isEqualTo(tankySquare);
        assertThat(tankySquare.id()).isEqualTo("tankySquare");
        assertThat(tankySquare.baseHealth()).isEqualTo(200);
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
}
