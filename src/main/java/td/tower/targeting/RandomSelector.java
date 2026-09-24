package td.tower.targeting;

import td.enemy.EnemyMob;
import td.util.RandomSource;

import java.util.List;
import java.util.Optional;

/**
 * Picks uniformly from the injected {@link RandomSource}, so a seeded run replays the same choices.
 */
public final class RandomSelector implements TargetSelector {

    private final RandomSource random;

    public RandomSelector(RandomSource random) {
        this.random = random;
    }

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(candidates.get(this.random.nextIndex(candidates.size())));
    }
}
