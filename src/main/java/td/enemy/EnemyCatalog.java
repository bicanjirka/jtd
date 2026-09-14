package td.enemy;

import td.util.GameStartupException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * The open, string-keyed source of buildable enemy types - replaces the closed
 * {@code EnemyFactory.Enemy} enum. Built-ins are pre-registered globally under their existing
 * single-letter wave-script ids; a level can register its own new {@link EnemyDefinition}s, or
 * clone-and-adjust an existing one under a new id, scoped to that catalog instance only. Named
 * after the existing {@code LevelCatalog}/{@code BuiltInLevelCatalog} precedent rather than
 * "Registry", since {@link EnemyRegistry} already names the live per-wave roster's read
 * interface and reusing the word would collide.
 */
public final class EnemyCatalog {

    private final Map<String, EnemyDefinition> definitions = new HashMap<>();

    /**
     * Registers {@code definition} under its own id. Throws {@link GameStartupException} for a
     * duplicate id, or if this definition's own spawn chain - or any chain it completes by
     * being registered - turns out to be cyclical (see {@link #checkAcyclic}).
     */
    public void register(EnemyDefinition definition) {
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

    public EnemyDefinition get(String id) {
        EnemyDefinition definition = this.definitions.get(id);
        if (definition == null) {
            throw new GameStartupException("No enemy definition registered for id '" + id + "'");
        }
        return definition;
    }

    private static EnemyDefinition withId(EnemyDefinition source, String newId) {
        return new EnemyDefinition(newId, source.displayName(), source.description(), source.baseSpeed(),
                source.healthDivisor(), source.mobType(), source.archetype(), source.movement(),
                source.traits(), source.abilities());
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
