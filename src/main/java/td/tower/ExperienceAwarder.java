package td.tower;

import td.enemy.EnemyWalk;
import td.enemy.WalkEndListener;
import td.tower.buff.TowerBuff;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Pays each finished walk's bounty as XP to every tower the mob reached, and once to every tower
 * buffing one of those, so an aura earns with the towers it serves, and to the towers an aura makes
 * share with those. Towers add to what a tower earns ({@link Tower#xpBonusFor}). Kill or leak pays
 * the same, and nobody needs the kill.
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
        Set<Tower> shared = Collections.newSetFromMap(new IdentityHashMap<>());
        all.forEach(tower -> shared.addAll(tower.xpSharedWith(reached)));
        for (Tower tower : all) {
            if (reached.contains(tower) || buffsAny(tower, reached) || shared.contains(tower)) {
                tower.earnXp(walk.getBounty(), bonusFor(all, tower));
            }
        }
    }

    /** What every tower adds to the XP {@code earner} gets from a bounty. */
    private static float bonusFor(List<Tower> all, Tower earner) {
        return (float) all.stream().mapToDouble(tower -> tower.xpBonusFor(earner)).sum();
    }


    private static boolean buffsAny(Tower tower, List<Tower> earners) {
        return earners.stream().anyMatch(earner -> !tower.buffFor(earner).equals(TowerBuff.none()));
    }
}
