package td.tower.seeker;

import td.enemy.EnemyMob;
import td.tower.targeting.HighestRankSelector;
import td.util.ThreadConfined;

/**
 * Hunter's Mark: each consecutive hit on the same enemy lands a fifth harder, up to twice as hard,
 * and the Seeker aims at the highest rank. A salvo is a string of consecutive hits.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class HuntersMarkPerk implements SeekerPerk {

    private static final float BONUS_PER_HIT = 0.2f;
    private static final float MAX_BONUS = 1f;

    private EnemyMob marked;
    private int streak;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withAim(new SeekerAim(new HighestRankSelector(), "highest rank"));
    }

    @Override
    public float damageFactor(EnemyMob target) {
        this.streak = target == this.marked ? this.streak + 1 : 0;
        this.marked = target;
        return 1f + Math.min(MAX_BONUS, BONUS_PER_HIT * this.streak);
    }
}
