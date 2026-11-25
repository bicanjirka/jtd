package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks an enemy this shot can execute first: one its crit of {@code critHit}, before armor, leaves under the
 * {@code threshold}. Then the one closest to leaking.
 */
public final class ExecutionerSelector implements TargetSelector {

    private final ExecuteThreshold threshold;
    private final int critHit;

    public ExecutionerSelector(ExecuteThreshold threshold, int critHit) {
        this.threshold = threshold;
        this.critHit = critHit;
    }

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        Comparator<EnemyMob> executable = Comparator.comparing(enemy -> this.threshold.leftUnderBy(enemy, this.critHit));
        return candidates.stream().max(executable
                .thenComparingInt(EnemyMob::getProgression));
    }
}
