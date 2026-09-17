package td.tower.targeting;

import td.enemy.EnemyMob;
import td.util.RandomSource;

import java.util.List;
import java.util.Optional;

/**
 * Picks a uniformly random candidate from its {@link RandomSource}, so a seeded run replays
 * the same choices. A test can inject a fixed source and assert on the exact pick rather than
 * only on "it returned one of the candidates".
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
