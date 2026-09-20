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
 * The open, string-keyed source of buildable enemy types - replaces the closed
 * {@code EnemyFactory.Enemy} enum. {@link #builtIn()} returns a fresh catalog with the five
 * built-in definitions pre-registered under their existing single-letter wave-script ids; a
 * level can additionally register its own new {@link RankedEnemy}s, or clone-and-adjust an
 * existing one under a new id, scoped to that catalog instance only - though no level does yet
 * (see {@code td/enemy/CLAUDE.md}). Named after the existing {@code LevelCatalog}/
 * {@code BuiltInLevelCatalog} precedent rather than "Registry", since {@link EnemyRegistry}
 * already names the live per-wave roster's read interface and reusing the word would collide.
 * <p>
 * Every registered id is a {@link RankedEnemy}, even one that only ever authors
 * {@link Rank#GRUNT} - {@link #register(EnemyDefinition)} is the convenience for exactly that
 * case, so a plain, non-ranked definition registers exactly as it always has.
 */
public final class EnemyCatalog {

    private final Map<String, RankedEnemy> rankedEnemies = new LinkedHashMap<>();

    /**
     * A fresh catalog with the five basic built-ins and the Warden boss chain pre-registered
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
        catalog.register(BuiltInEnemies.MENDER);
        catalog.register(BuiltInEnemies.WARDEN_EGG_3);
        catalog.register(BuiltInEnemies.WARDEN_3);
        catalog.register(BuiltInEnemies.WARDEN_EGG_2);
        catalog.register(BuiltInEnemies.WARDEN_2);
        catalog.register(BuiltInEnemies.WARDEN_EGG_1);
        catalog.register(BuiltInEnemies.WARDEN_1);
        return catalog;
    }

    /**
     * Registers a single-rank ladder - {@code definition} becomes this id's {@link Rank#GRUNT}
     * (and only) definition, so any requested rank resolves to it (the fallback rule). The
     * ordinary shape for an enemy this feature doesn't give a real ladder to.
     */
    public void register(EnemyDefinition definition) {
        this.register(RankedEnemy.startingAt(definition).build());
    }

    /**
     * Registers {@code rankedEnemy} under its own id. Throws {@link GameStartupException} for a
     * duplicate id, for an id colliding with one of {@link WaveScript#RESERVED_TOKENS} (the
     * spacer, a spawn-type keyword, or a rank name - those are recognized before any catalog
     * lookup, so a registered id under one of them could never be reached), or if this enemy's
     * own spawn chain - or any chain it completes by being registered - turns out to be
     * cyclical (see {@link #checkAcyclic}).
     */
    public void register(RankedEnemy rankedEnemy) {
        String id = rankedEnemy.id();
        if (WaveScript.RESERVED_TOKENS.contains(id)) {
            throw new GameStartupException(
                    "Enemy id '" + id + "' collides with a reserved wave-script token " + WaveScript.RESERVED_TOKENS);
        }
        if (this.rankedEnemies.containsKey(id)) {
            throw new GameStartupException("Duplicate enemy definition id '" + id + "'");
        }
        this.rankedEnemies.put(id, rankedEnemy);
        this.checkAcyclic(id);
    }

    /**
     * Registers a copy of the whole ladder already registered under {@code baseId}, with its id
     * replaced by {@code newId} and {@code adjust} run over every rank the original authored -
     * e.g. "a Square with double the usual resistance for this one level", applied consistently
     * whether the original defines one rank or five, without touching the original. See
     * {@link RankedEnemy#cloneAs(String, UnaryOperator)}.
     */
    public RankedEnemy cloneAndAdjust(String baseId, String newId, UnaryOperator<EnemyDefinition> adjust) {
        RankedEnemy cloned = this.ranked(baseId).cloneAs(newId, adjust);
        this.register(cloned);
        return cloned;
    }

    /**
     * The rank-aware counterpart to {@link #cloneAndAdjust(String, String, UnaryOperator)} -
     * {@code adjust} sees each rank alongside its own definition, so it can change only some
     * ranks and leave others alone - e.g. "give Veteran and up a gold shield". See
     * {@link RankedEnemy#cloneAs(String, BiFunction)}.
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

    /**
     * Every registered id, in registration order - what a debug/tooling caller cycles through
     * (see {@code GameEngine.debugSpawnNextCatalogEnemy}). {@code builtIn()} registers the four
     * basics first, then the Warden chain innermost-first (each egg before the Warden stage that
     * spawns it, since a forward reference is only walked once its target is itself registered).
     */
    public List<String> ids() {
        return List.copyOf(this.rankedEnemies.keySet());
    }

    /**
     * The full rank ladder registered under {@code id}.
     */
    public RankedEnemy ranked(String id) {
        RankedEnemy rankedEnemy = this.rankedEnemies.get(id);
        if (rankedEnemy == null) {
            throw new GameStartupException("No enemy definition registered for id '" + id + "'");
        }
        return rankedEnemy;
    }

    /**
     * {@code id}'s {@link Rank#GRUNT} definition - what every rank-unaware caller (
     * {@code EnemyFactory}, an ability-spawn resolved before this feature) gets.
     */
    public EnemyDefinition get(String id) {
        return this.ranked(id).definitionFor(Rank.GRUNT);
    }

    /**
     * {@code id}'s definition at {@code rank}, or its own highest authored rank if it doesn't go
     * that high - the silent fallback rule (see {@link RankedEnemy}).
     */
    public EnemyDefinition get(String id, Rank rank) {
        return this.ranked(id).definitionFor(rank);
    }

    /**
     * Builds a live mob from the definition registered under {@code id} at {@code rank} - what {@code Wave}/{@code WaveScript} spawn through.
     */
    public EnemyMob spawn(String id, GameWorld gameWorld, int delay, int health, int price, Rank rank) {
        EnemyDefinition definition = this.get(id, rank);
        SpawnParameters spawnParameters = SpawnParameters.atSlot(delay, definition.baseSpeed(), health, price);
        return new DefinedEnemyMob(definition, gameWorld, spawnParameters, rank);
    }

    /**
     * Walks the directed graph formed by every registered enemy's own authored ranks' own
     * {@link SpawnEnemiesAction} references, starting from {@code startId}, and rejects a
     * definition that, directly or transitively, could spawn itself. A finite, strictly linear
     * chain (the Warden/egg's 6 stages) passes; an actual cycle does not. A reference to an id
     * not yet registered is skipped rather than failing - registration order isn't fixed, so a
     * forward reference is valid and simply isn't walked until that id is itself registered.
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
