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
 * One enemy's rank ladder, authored progressively: each {@link Builder#thenAt} step receives the
 * previous rank's definition, so unchanged parts carry forward. Starts at {@link Rank#GRUNT} and
 * climbs without gaps; it need not reach {@link Rank#BOSS}.
 * <p>
 * Asking for an unauthored rank resolves to the highest authored rank below it;
 * {@link #effectiveRank(Rank)} gives that rank itself.
 */
public final class RankedEnemy {

    private final String id;
    // EnumMap-backed: cloneAs walks ranks in order.
    private final Map<Rank, EnemyDefinition> definitionsByRank;
    private final Rank highestDefinedRank;

    private RankedEnemy(String id, Map<Rank, EnemyDefinition> definitionsByRank, Rank highestDefinedRank) {
        this.id = id;
        this.definitionsByRank = definitionsByRank;
        this.highestDefinedRank = highestDefinedRank;
    }

    /** Starts a ladder; a one-rank ladder is complete on its own. */
    public static Builder startingAt(EnemyDefinition grunt) {
        return new Builder(grunt);
    }

    public String id() {
        return this.id;
    }

    /** The highest authored rank not above {@code requested}. */
    public Rank effectiveRank(Rank requested) {
        return requested.compareTo(this.highestDefinedRank) > 0 ? this.highestDefinedRank : requested;
    }

    public EnemyDefinition definitionFor(Rank requested) {
        return this.definitionsByRank.get(this.effectiveRank(requested));
    }

    /** Each authored definition once. */
    Collection<EnemyDefinition> authoredDefinitions() {
        return this.definitionsByRank.values();
    }

    /**
     * Clones the whole ladder under {@code newId}, running {@code adjust} over every authored rank.
     * Rank-blind; see {@link #cloneAs(String, BiFunction)} for a rank-aware change.
     */
    public RankedEnemy cloneAs(String newId, UnaryOperator<EnemyDefinition> adjust) {
        return this.cloneAs(newId, (rank, definition) -> adjust.apply(definition));
    }

    /**
     * Like {@link #cloneAs(String, UnaryOperator)}, but {@code adjust} also sees the rank. Each
     * rank is adjusted from this ladder's definition, not the previous adjusted clone.
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

    @ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
    public static final class Builder {

        private final String id;
        private final Map<Rank, EnemyDefinition> definitionsByRank = new EnumMap<>(Rank.class);
        private Rank highestSoFar = Rank.GRUNT;

        private Builder(EnemyDefinition grunt) {
            this.id = grunt.id();
            this.definitionsByRank.put(Rank.GRUNT, grunt);
        }

        /**
         * Authors {@code next} as a change to the current highest rank. Ranks must be added in
         * order, one step at a time.
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
            // Not Map.copyOf: it does not keep EnumMap's rank order.
            return new RankedEnemy(this.id, Collections.unmodifiableMap(new EnumMap<>(this.definitionsByRank)),
                    this.highestSoFar);
        }
    }
}
