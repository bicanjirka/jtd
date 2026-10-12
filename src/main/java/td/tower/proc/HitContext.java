package td.tower.proc;

import td.enemy.EnemyMob;
import td.tower.TowerStats;

/**
 * One hit as the tower is about to land it, with the stats of the attack it belongs to.
 *
 * @param target      whom it hits
 * @param kind        what kind of hit it is
 * @param rangeShare  how far out the target stands, as a share of the tower's range
 * @param focus       how many attacks in a row the tower has landed on the target
 * @param freshTarget whether this is the first attack at a new target
 * @param shot        the stats of the attack this hit belongs to
 */
public record HitContext(EnemyMob target, HitKind kind, float rangeShare, int focus, boolean freshTarget,
                         TowerStats shot) {

    /** A hit with no focus, as most towers land them. */
    public static HitContext of(EnemyMob target, HitKind kind, float rangeShare, TowerStats shot) {
        return new HitContext(target, kind, rangeShare, 0, false, shot);
    }

    public HitContext withFocus(int focus, boolean freshTarget) {
        return new HitContext(this.target, this.kind, this.rangeShare, focus, freshTarget, this.shot);
    }
}
