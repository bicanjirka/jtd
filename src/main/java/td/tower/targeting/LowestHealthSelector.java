package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Picks the enemy closest to dying. */
public final class LowestHealthSelector implements TargetSelector {

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        return candidates.stream().min(Comparator.comparingInt(EnemyMob::getHealth));
    }
}
