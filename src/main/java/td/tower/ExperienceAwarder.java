package td.tower;

import td.enemy.EnemyWalk;
import td.enemy.WalkEndListener;
import td.tower.buff.TowerBuff;

import java.util.List;

/**
 * Pays each finished walk's bounty as XP to every tower the mob reached, and once to every tower
 * buffing one of those, so an aura earns with the towers it serves. Kill or leak pays the same,
 * and nobody needs the kill.
 */
public final class ExperienceAwarder implements WalkEndListener {

    private final TowerRoster towers;

    public ExperienceAwarder(TowerRoster towers) {
        this.towers = towers;
    }

    @Override
    public void walkEnded(EnemyWalk walk) {
        List<Tower> all = this.towers.all();
        List<Tower> reached = all.stream().filter(tower -> tower.reached(walk)).toList();
        if (reached.isEmpty()) {
            return;
        }
        for (Tower tower : all) {
            if (reached.contains(tower) || buffsAny(tower, reached)) {
                tower.earnXp(walk.getBounty());
            }
        }
    }

    private static boolean buffsAny(Tower tower, List<Tower> earners) {
        return earners.stream().anyMatch(earner -> !tower.buffFor(earner).equals(TowerBuff.none()));
    }
}
