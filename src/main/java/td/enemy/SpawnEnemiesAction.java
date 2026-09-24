package td.enemy;

import java.util.Optional;

/**
 * Spawns {@code shape.members()} of {@code definitionId}. With {@code consumesSelf}, the caster is
 * replaced in the same step - a transformation, not a kill, so no bounty, score or kill credit.
 * Only one-member consuming spawns are meaningful.
 */
public record SpawnEnemiesAction(String definitionId, AbilitySpawnShape shape, boolean consumesSelf)
        implements AbilityAction {

    public SpawnEnemiesAction(String definitionId, int count, boolean consumesSelf) {
        this(definitionId, new AbilitySpawnShape(count, 1f, 1f, 1f, 0.0, Optional.empty()), consumesSelf);
    }
}
