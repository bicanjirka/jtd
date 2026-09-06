package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.List;
import java.util.Optional;

public final class RandomSelector implements TargetSelector {

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(candidates.get((int) (Math.random() * candidates.size())));
    }
}
