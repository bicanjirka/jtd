package td.enemy;

import td.util.GameStartupException;
import td.util.GameWorld;
import td.wave.WaveScript;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * The open, string-keyed source of buildable enemy types - replaces the closed
 * {@code EnemyFactory.Enemy} enum. {@link #builtIn()} returns a fresh catalog with the four
 * built-in definitions pre-registered under their existing single-letter wave-script ids; a
 * level can additionally register its own new {@link EnemyDefinition}s, or clone-and-adjust an
 * existing one under a new id, scoped to that catalog instance only - though no level does yet
 * (see {@code td/enemy/CLAUDE.md}). Named after the existing {@code LevelCatalog}/
 * {@code BuiltInLevelCatalog} precedent rather than "Registry", since {@link EnemyRegistry}
 * already names the live per-wave roster's read interface and reusing the word would collide.
 */
public final class EnemyCatalog {

    private final Map<String, EnemyDefinition> definitions = new LinkedHashMap<>();

    /**
     * A fresh catalog with the four basic built-ins and the Warden boss chain pre-registered
     * under their existing wave-script ids. Fresh, not shared/cached: {@code GameEngine.loadLevel}
     * is idempotent and re-enterable (see the root {@code CLAUDE.md}'s Levels section), so each
     * level load gets its own catalog rather than accumulating a previous level's registrations.
     */
    public static EnemyCatalog builtIn() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(BuiltInEnemies.SIMPLE);
        catalog.register(BuiltInEnemies.ARMORED);
        catalog.register(BuiltInEnemies.FRENZIED);
        catalog.register(BuiltInEnemies.GHOST);
        catalog.register(BuiltInEnemies.WARDEN_EGG_3);
        catalog.register(BuiltInEnemies.WARDEN_3);
        catalog.register(BuiltInEnemies.WARDEN_EGG_2);
        catalog.register(BuiltInEnemies.WARDEN_2);
        catalog.register(BuiltInEnemies.WARDEN_EGG_1);
        catalog.register(BuiltInEnemies.WARDEN_1);
        return catalog;
    }

    private static EnemyDefinition withId(EnemyDefinition source, String newId) {
        return new EnemyDefinition(newId, source.displayName(), source.description(),
                source.baseHealth(), source.price(), source.baseSpeed(),
                source.healthDivisor(), source.mobType(), source.archetype(), source.movement(),
                source.traitSlots(), source.abilitySlots());
    }

    /**
     * Registers {@code definition} under its own id. Throws {@link GameStartupException} for a
     * duplicate id, for an id colliding with one of {@link WaveScript#RESERVED_TOKENS} (the
     * spacer or a spawn-type keyword - those are recognized before any catalog lookup, so a
     * registered id under one of them could never be reached), or if this definition's own
     * spawn chain - or any chain it completes by being registered - turns out to be cyclical
     * (see {@link #checkAcyclic}).
     */
    public void register(EnemyDefinition definition) {
        if (WaveScript.RESERVED_TOKENS.contains(definition.id())) {
            throw new GameStartupException(
                    "Enemy id '" + definition.id() + "' collides with a reserved wave-script token "
                            + WaveScript.RESERVED_TOKENS);
        }
        if (this.definitions.containsKey(definition.id())) {
            throw new GameStartupException("Duplicate enemy definition id '" + definition.id() + "'");
        }
        this.definitions.put(definition.id(), definition);
        this.checkAcyclic(definition.id());
    }

    /**
     * Registers a copy of the definition already registered under {@code baseId}, with its id
     * replaced by {@code newId} and then run through {@code adjust} - e.g. "a Square with
     * double the usual resistance for this one level", without touching the original.
     */
    public EnemyDefinition cloneAndAdjust(String baseId, String newId, UnaryOperator<EnemyDefinition> adjust) {
        EnemyDefinition base = this.get(baseId);
        EnemyDefinition adjusted = adjust.apply(withId(base, newId));
        this.register(adjusted);
        return adjusted;
    }

    public boolean contains(String id) {
        return this.definitions.containsKey(id);
    }

    /**
     * Every registered id, in registration order - what a debug/tooling caller cycles through
     * (see {@code GameEngine.debugSpawnNextCatalogEnemy}). {@code builtIn()} registers the four
     * basics first, then the Warden chain innermost-first (each egg before the Warden stage that
     * spawns it, since a forward reference is only walked once its target is itself registered).
     */
    public List<String> ids() {
        return List.copyOf(this.definitions.keySet());
    }

    public EnemyDefinition get(String id) {
        EnemyDefinition definition = this.definitions.get(id);
        if (definition == null) {
            throw new GameStartupException("No enemy definition registered for id '" + id + "'");
        }
        return definition;
    }

    /**
     * Builds a live mob from the definition registered under {@code id} - what {@code Wave}/{@code WaveScript} spawn through.
     */
    public EnemyMob spawn(String id, GameWorld gameWorld, int delay, int health, int price, int level) {
        EnemyDefinition definition = this.get(id);
        SpawnParameters spawnParameters = SpawnParameters.atSlot(delay, definition.baseSpeed(), health, price);
        return new DefinedEnemyMob(definition, gameWorld, spawnParameters, level);
    }

    /**
     * Walks the directed graph formed by every registered definition's {@link SpawnEnemiesAction}
     * references, starting from {@code startId}, and rejects a definition that, directly or
     * transitively, could spawn itself. A finite, strictly linear chain (the Warden/egg's 6
     * stages) passes; an actual cycle does not. A reference to an id not yet registered is
     * skipped rather than failing - registration order isn't fixed, so a forward reference is
     * valid and simply isn't walked until that id is itself registered.
     */
    private void checkAcyclic(String startId) {
        this.walk(startId, new HashSet<>());
    }

    private void walk(String id, Set<String> visiting) {
        if (!visiting.add(id)) {
            throw new GameStartupException("Enemy definition '" + id + "' has a cyclical spawn chain");
        }
        EnemyDefinition definition = this.definitions.get(id);
        if (definition != null) {
            for (Ability ability : definition.abilities()) {
                this.walkAction(ability.action(), visiting);
            }
        }
        visiting.remove(id);
    }

    private void walkAction(AbilityAction action, Set<String> visiting) {
        switch (action) {
            case SpawnEnemiesAction spawn -> {
                if (this.definitions.containsKey(spawn.definitionId())) {
                    this.walk(spawn.definitionId(), visiting);
                }
            }
            case ApplyEffectAction ignored -> {
            }
        }
    }
}
