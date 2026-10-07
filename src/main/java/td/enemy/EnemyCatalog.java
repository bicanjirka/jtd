package td.enemy;

import td.util.GameStartupException;
import td.util.GameWorld;
import td.wave.WaveScript;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * The string-keyed source of buildable enemy types. {@link #builtIn()} returns a fresh catalog of
 * the built-ins; a level may register its own enemies or clone-and-adjust existing ones, scoped to
 * its catalog. Every id is a {@link RankedEnemy}; {@link #register(EnemyDefinition)} registers a
 * one-rank ladder.
 */
public final class EnemyCatalog {

    private final Map<String, RankedEnemy> rankedEnemies = new LinkedHashMap<>();

    /** A fresh catalog, never shared, so one level's registrations never leak into the next. */
    public static EnemyCatalog builtIn() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(BuiltInEnemies.SIMPLE);
        catalog.register(BuiltInEnemies.ARMORED);
        catalog.register(BuiltInEnemies.S_SPAWN);
        catalog.register(BuiltInEnemies.FRENZIED);
        catalog.register(BuiltInEnemies.T_SPAWN);
        catalog.register(BuiltInEnemies.GHOST);
        catalog.register(BuiltInEnemies.MENDER);
        catalog.register(BuiltInEnemies.JAMMER);
        catalog.register(BuiltInEnemies.WARDEN_EGG_3);
        catalog.register(BuiltInEnemies.WARDEN_3);
        catalog.register(BuiltInEnemies.WARDEN_EGG_2);
        catalog.register(BuiltInEnemies.WARDEN_2);
        catalog.register(BuiltInEnemies.WARDEN_EGG_1);
        catalog.register(BuiltInEnemies.WARDEN_1);
        return catalog;
    }

    /**
     * Registers {@code definition} as a one-rank ladder, so every requested rank resolves to it.
     */
    public void register(EnemyDefinition definition) {
        this.register(RankedEnemy.startingAt(definition).build());
    }

    /**
     * @throws GameStartupException for a duplicate id, an id that is a
     * {@link WaveScript#RESERVED_TOKENS reserved token}, or a spawn chain that becomes cyclic
     */
    public void register(RankedEnemy rankedEnemy) {
        String id = rankedEnemy.id();
        if (WaveScript.isReserved(id)) {
            throw new GameStartupException(
                    "Enemy id '" + id + "' collides with a reserved wave-script token " + WaveScript.RESERVED_TOKENS
                            + " or the w<number> spacing token");
        }
        if (this.rankedEnemies.containsKey(id)) {
            throw new GameStartupException("Duplicate enemy definition id '" + id + "'");
        }
        this.rankedEnemies.put(id, rankedEnemy);
        this.checkAcyclic(id);
    }

    /**
     * Registers a copy of {@code baseId}'s whole ladder under {@code newId}, with {@code adjust}
     * run over every authored rank. The original is untouched.
     */
    public RankedEnemy cloneAndAdjust(String baseId, String newId, UnaryOperator<EnemyDefinition> adjust) {
        RankedEnemy cloned = this.ranked(baseId).cloneAs(newId, adjust);
        this.register(cloned);
        return cloned;
    }

    /**
     * Like {@link #cloneAndAdjust(String, String, UnaryOperator)}, but {@code adjust} also sees
     * each rank.
     */
    public RankedEnemy cloneAndAdjust(String baseId, String newId,
            BiFunction<Rank, EnemyDefinition, EnemyDefinition> adjust) {
        RankedEnemy cloned = this.ranked(baseId).cloneAs(newId, adjust);
        this.register(cloned);
        return cloned;
    }

    public boolean contains(String id) {
        return this.rankedEnemies.containsKey(id);
    }

    /** Every registered id, in registration order. */
    public List<String> ids() {
        return List.copyOf(this.rankedEnemies.keySet());
    }

    public RankedEnemy ranked(String id) {
        RankedEnemy rankedEnemy = this.rankedEnemies.get(id);
        if (rankedEnemy == null) {
            throw new GameStartupException("No enemy definition registered for id '" + id + "'");
        }
        return rankedEnemy;
    }

    /** {@code id}'s {@link Rank#GRUNT} definition. */
    public EnemyDefinition get(String id) {
        return this.ranked(id).definitionFor(Rank.GRUNT);
    }

    /** {@code id}'s definition at {@code rank}, or at its highest authored rank below that. */
    public EnemyDefinition get(String id, Rank rank) {
        return this.ranked(id).definitionFor(rank);
    }

    /** Builds a live mob from {@code id}'s definition at {@code rank}. */
    public EnemyMob spawn(String id, GameWorld gameWorld, int delay, int health, int price, Rank rank) {
        EnemyDefinition definition = this.get(id, rank);
        SpawnParameters spawnParameters = SpawnParameters.atSlot(delay, definition.baseSpeed(), health, price);
        return new DefinedEnemyMob(definition, gameWorld, spawnParameters, rank);
    }

    /**
     * Rejects {@code startId} if its spawn references can lead back to it. A reference to an
     * unregistered id is skipped: it is checked once that id registers.
     */
    private void checkAcyclic(String startId) {
        this.walk(startId, new HashSet<>());
    }

    private void walk(String id, Set<String> visiting) {
        if (!visiting.add(id)) {
            throw new GameStartupException("Enemy definition '" + id + "' has a cyclical spawn chain");
        }
        RankedEnemy rankedEnemy = this.rankedEnemies.get(id);
        if (rankedEnemy != null) {
            for (EnemyDefinition definition : rankedEnemy.authoredDefinitions()) {
                for (Ability ability : definition.abilities()) {
                    this.walkAction(ability.action(), visiting);
                }
            }
        }
        visiting.remove(id);
    }

    private void walkAction(AbilityAction action, Set<String> visiting) {
        switch (action) {
            case SpawnEnemiesAction spawn -> {
                if (this.rankedEnemies.containsKey(spawn.definitionId())) {
                    this.walk(spawn.definitionId(), visiting);
                }
            }
            case ApplyEffectAction ignored -> {
            }
        }
    }
}
