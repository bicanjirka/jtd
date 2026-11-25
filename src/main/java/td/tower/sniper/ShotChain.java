package td.tower.sniper;

import td.enemy.EnemyMob;

import java.util.ArrayList;
import java.util.List;

/**
 * One shot and the hops it led to. Every leg is the same shot as fired, before any mechanic shaped it, shaped anew for
 * the enemy that leg flies to, so a mechanic that singles out its target judges each one on its own.
 *
 * @param unshaped the shot as fired, before the owned mechanics shaped it
 * @param struck   the enemies its legs have landed on so far, in order
 */
public record ShotChain(SniperShot unshaped, List<EnemyMob> struck) {

    public ShotChain {
        struck = List.copyOf(struck);
    }

    /** A shot about to be fired, which has struck no one yet. */
    public static ShotChain of(SniperShot unshaped) {
        return new ShotChain(unshaped, List.of());
    }

    /** The shot shaped for {@code target} by each of {@code owned}, in order. */
    public SniperShot shapedFor(EnemyMob target, List<SniperMechanic> owned) {
        SniperShot shot = this.unshaped;
        for (SniperMechanic mechanic : owned) {
            shot = mechanic.shape(shot, target);
        }
        return shot;
    }

    /** The same chain once a leg has landed on {@code target}. */
    public ShotChain landedOn(EnemyMob target) {
        List<EnemyMob> after = new ArrayList<>(this.struck);
        after.add(target);
        return new ShotChain(this.unshaped, after);
    }

    /** How many times it has hopped on from the enemy it was fired at. */
    public int hops() {
        return Math.max(0, this.struck.size() - 1);
    }

    /** Of {@code near}, the enemies a further hop may fly to: none it already struck, and none once it has hopped {@code maxHops} times. */
    public List<EnemyMob> hopTargets(List<EnemyMob> near, int maxHops) {
        if (this.hops() >= maxHops) {
            return List.of();
        }
        return near.stream().filter(enemy -> !this.struck.contains(enemy)).toList();
    }
}
