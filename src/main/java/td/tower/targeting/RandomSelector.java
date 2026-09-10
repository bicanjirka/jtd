package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.List;
import java.util.Optional;

/**
 * Picks a uniformly random candidate. Uses {@link Math#random()} rather than an injected
 * source, so a test asserts over the distribution or over "it returned one of the candidates",
 * not over a fixed sequence.
 */
public final class RandomSelector implements TargetSelector {

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(candidates.get((int) (Math.random() * candidates.size())));
    }
}
