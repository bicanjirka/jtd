package td.enemy;

import java.util.Optional;

/**
 * Spawns {@code shape.members()} of {@code definitionId}. With {@code consumesSelf}, the caster is
 * replaced in the same step - a transformation, not a kill, so no bounty, score or kill credit.
 * Only one-member consuming spawns are meaningful.
 *
 * @param rank the rank they come at; empty for the caster's own
 */
public record SpawnEnemiesAction(String definitionId, AbilitySpawnShape shape, boolean consumesSelf,
                                 Optional<Rank> rank) implements AbilityAction {

    /** At the caster's rank. */
    public SpawnEnemiesAction(String definitionId, AbilitySpawnShape shape, boolean consumesSelf) {
        this(definitionId, shape, consumesSelf, Optional.empty());
    }

    public SpawnEnemiesAction(String definitionId, int count, boolean consumesSelf) {
        this(definitionId, new AbilitySpawnShape(count, 1f, 1f, 1f, 0.0, Optional.empty()), consumesSelf);
    }

    /** These spawns at {@code fixed}, whatever the caster's rank. */
    public SpawnEnemiesAction atRank(Rank fixed) {
        return new SpawnEnemiesAction(this.definitionId, this.shape, this.consumesSelf, Optional.of(fixed));
    }

    @Override
    public boolean appliesEffects() {
        return false;
    }
}
