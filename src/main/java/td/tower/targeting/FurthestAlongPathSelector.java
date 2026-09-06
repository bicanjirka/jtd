package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class FurthestAlongPathSelector implements TargetSelector {

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        return candidates.stream().max(Comparator.comparingInt(EnemyMob::getProgression));
    }
}
