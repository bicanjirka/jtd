package td.tower;

import td.enemy.EnemyWalk;
import td.enemy.WalkEndListener;

import java.util.ArrayList;
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
        List<Tower> reached = new ArrayList<>();
        // Only an emitter buffs, adds a bonus or shares, and a board holds few next to the towers they serve.
        List<Tower> emitters = new ArrayList<>();
        for (Tower tower : all) {
            if (tower.reached(walk)) {
                reached.add(tower);
            }
            if (tower.emitsBuffs()) {
                emitters.add(tower);
            }
        }
        if (reached.isEmpty()) {
            return;
        }
        Set<Tower> earning = Collections.newSetFromMap(new IdentityHashMap<>());
        earning.addAll(reached);
        for (Tower emitter : emitters) {
            if (buffsAny(emitter, reached)) {
                earning.add(emitter);
            }
            earning.addAll(emitter.xpSharedWith(reached));
        }
        for (Tower tower : all) {
            if (earning.contains(tower)) {
                tower.earnXp(walk.getBounty(), bonusFor(emitters, tower));
            }
        }
    }

    /** What every emitter adds to the XP {@code earner} gets from a bounty. */
    private static float bonusFor(List<Tower> emitters, Tower earner) {
        return (float) emitters.stream().mapToDouble(tower -> tower.xpBonusFor(earner)).sum();
    }


    private static boolean buffsAny(Tower tower, List<Tower> earners) {
        return earners.stream().anyMatch(earner -> !tower.buffFor(earner).isNone());
    }
}
