package td.enemy;

import td.util.GameStartupException;
import td.util.ThreadConfined;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * One enemy kind's rank ladder: a {@link Rank} to {@link EnemyDefinition} mapping, authored
 * progressively rather than as an independent flat table - each rank's {@link Builder#thenAt}
 * step receives the <em>previous</em> rank's own resulting definition and returns the next one,
 * so a rank that only changes health (say) keeps everything else - traits included - unchanged
 * from the rank before it. Every ladder starts at {@link Rank#GRUNT} and is authored
 * contiguously upward; not every enemy has to reach {@link Rank#BOSS}.
 * <p>
 * {@link #definitionFor(Rank)} is never an authoring error for an unauthored rank: a wave or
 * slot asking for a rank this enemy doesn't define silently resolves to this enemy's own highest
 * authored rank instead - see {@link #effectiveRank(Rank)} for the same resolution as a
 * {@link Rank} rather than a definition, which is what a spawned mob's own badge and internal
 * formulas (fade duration, body scale, score weight) need. {@link #cloneAs} clones a whole ladder
 * under a new id, not just one rank's definition - what {@code EnemyCatalog.cloneAndAdjust} uses.
 */
public final class RankedEnemy {

    private final String id;
    // Always built from an EnumMap (see Builder.build) and never re-wrapped through something
    // that doesn't preserve that ordering (Map.copyOf does not) - cloneAs walks this in rank
    // order, low to high.
    private final Map<Rank, EnemyDefinition> definitionsByRank;
    private final Rank highestDefinedRank;

    private RankedEnemy(String id, Map<Rank, EnemyDefinition> definitionsByRank, Rank highestDefinedRank) {
        this.id = id;
        this.definitionsByRank = definitionsByRank;
        this.highestDefinedRank = highestDefinedRank;
    }

    /**
     * Starts a ladder at {@link Rank#GRUNT}, defined from scratch by {@code grunt}. A ladder
     * that goes no further is legal on its own - {@link Rank#GRUNT} is a complete, one-rank
     * ladder, not a required prelude to calling {@link Builder#thenAt}.
     */
    public static Builder startingAt(EnemyDefinition grunt) {
        return new Builder(grunt);
    }

    public String id() {
        return this.id;
    }

    /**
     * This enemy's own highest authored rank, never higher than {@code requested} - the
     * resolution {@link #definitionFor(Rank)} applies.
     */
    public Rank effectiveRank(Rank requested) {
        return requested.compareTo(this.highestDefinedRank) > 0 ? this.highestDefinedRank : requested;
    }

    public EnemyDefinition definitionFor(Rank requested) {
        return this.definitionsByRank.get(this.effectiveRank(requested));
    }

    /**
     * Every rank this enemy actually authored, each definition once - what {@code EnemyCatalog}'s
     * spawn-cycle check walks, rather than {@link #definitionFor} for every {@link Rank} value,
     * which would walk a fallback rank's definition redundantly once per unauthored rank above it.
     */
    Collection<EnemyDefinition> authoredDefinitions() {
        return this.definitionsByRank.values();
    }

    /**
     * Clones this whole ladder under {@code newId}, running {@code adjust} over every rank this
     * enemy actually authored - e.g. "a Square with double the usual resistance for this one
     * level," applied consistently across whatever ranks the original defines, not just
     * {@link Rank#GRUNT}. Rank-blind: {@code adjust} cannot tell which rank it's being run
     * against, so it can only express a change uniform across every rank (the result still
     * differs per rank, since each rank's own input already did). {@link #cloneAs(String,
     * BiFunction)} is the rank-aware counterpart for a change that only starts at some rank -
     * "give Veteran and up a gold shield," say.
     */
    public RankedEnemy cloneAs(String newId, UnaryOperator<EnemyDefinition> adjust) {
        return this.cloneAs(newId, (rank, definition) -> adjust.apply(definition));
    }

    /**
     * The rank-aware counterpart to {@link #cloneAs(String, UnaryOperator)} - {@code adjust}
     * receives each rank alongside its own definition, so it can branch on rank (e.g. {@code
     * rank.compareTo(Rank.VETERAN) >= 0} to change Veteran and every rank above it, leaving Grunt
     * and Soldier untouched). Each rank is still cloned from *this* ladder's own definition for
     * that rank, not from the previous rank's already-adjusted clone - {@code adjust} sees the
     * same starting point {@link #cloneAs(String, UnaryOperator)} does, just with its rank named.
     */
    public RankedEnemy cloneAs(String newId, BiFunction<Rank, EnemyDefinition, EnemyDefinition> adjust) {
        Iterator<Map.Entry<Rank, EnemyDefinition>> ranks = this.definitionsByRank.entrySet().iterator();
        Map.Entry<Rank, EnemyDefinition> grunt = ranks.next();
        Builder builder = RankedEnemy.startingAt(adjust.apply(grunt.getKey(), withId(grunt.getValue(), newId)));
        while (ranks.hasNext()) {
            Map.Entry<Rank, EnemyDefinition> entry = ranks.next();
            Rank rank = entry.getKey();
            EnemyDefinition renamed = withId(entry.getValue(), newId);
            builder.thenAt(rank, ignored -> adjust.apply(rank, renamed));
        }
        return builder.build();
    }

    private static EnemyDefinition withId(EnemyDefinition source, String newId) {
        return new EnemyDefinition(newId, source.displayName(), source.description(), source.baseHealth(),
                source.price(), source.baseSpeed(), source.healthDivisor(), source.mobType(), source.archetype(),
                source.movement(), source.traitSlots(), source.abilitySlots());
    }

    /**
     * Builds a {@link RankedEnemy} one rank at a time, each step describing only what changed
     * from the rank before it - see the class doc comment.
     */
    @ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
    // a short-lived authoring helper (a static initializer, a level's own registration code),
    // never shared beyond whichever thread builds it
    public static final class Builder {

        private final String id;
        private final Map<Rank, EnemyDefinition> definitionsByRank = new EnumMap<>(Rank.class);
        private Rank highestSoFar = Rank.GRUNT;

        private Builder(EnemyDefinition grunt) {
            this.id = grunt.id();
            this.definitionsByRank.put(Rank.GRUNT, grunt);
        }

        /**
         * Authors {@code next} as a change from this ladder's own current highest rank -
         * {@code change} receives that rank's full definition and returns {@code next}'s. Ranks
         * must be authored strictly in ladder order, one step at a time, from {@link Rank#GRUNT};
         * skipping one, repeating one, or going backward is an authoring error.
         */
        public Builder thenAt(Rank next, UnaryOperator<EnemyDefinition> change) {
            if (next.ordinal() != this.highestSoFar.ordinal() + 1) {
                throw new GameStartupException("Enemy '" + this.id + "' must author " + next
                        + " immediately after " + this.highestSoFar + " - ranks are authored contiguously"
                        + " from GRUNT, one step at a time");
            }
            EnemyDefinition previous = this.definitionsByRank.get(this.highestSoFar);
            EnemyDefinition definition = change.apply(previous);
            if (!definition.id().equals(this.id)) {
                throw new GameStartupException("Enemy '" + this.id + "'s rank step for " + next
                        + " changed its id to '" + definition.id() + "' - a rank step may not change identity");
            }
            this.definitionsByRank.put(next, definition);
            this.highestSoFar = next;
            return this;
        }

        public RankedEnemy build() {
            // Not Map.copyOf: it does not promise to preserve an EnumMap's own natural (ordinal)
            // iteration order, which RankedEnemy.cloneAs relies on to walk ranks low-to-high.
            // EnumMap always iterates in enum order, by its own contract - wrapping a private
            // copy of it keeps that guarantee while still being unmodifiable from the outside.
            return new RankedEnemy(this.id, Collections.unmodifiableMap(new EnumMap<>(this.definitionsByRank)),
                    this.highestSoFar);
        }
    }
}
