package td.tower.sniper;

import td.enemy.Rank;

/**
 * Executioner: a shot that leaves a non-boss enemy under a sliver of health kills it, and counts
 * as a crit for what triggers off one. A boss takes more instead while it is low.
 */
public final class ExecutionerPerk implements SniperPerk {

    private static final float EXECUTE_BELOW = 0.15f;
    private static final float BOSS_WOUNDED_BELOW = 0.25f;
    private static final float BOSS_WOUNDED_DAMAGE = 1.5f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        boolean woundedBoss = context.target().getRank() == Rank.BOSS
                && context.target().getHealthFraction() < BOSS_WOUNDED_BELOW;
        return woundedBoss ? shot.scaledBy(BOSS_WOUNDED_DAMAGE) : shot;
    }

    @Override
    public ShotResult settle(ShotResult result, ShotActions actions) {
        if (result.killed() || result.target().getRank() == Rank.BOSS
                || result.target().getHealthFraction() >= EXECUTE_BELOW) {
            return result;
        }
        return new ShotResult(result.target(), true, actions.execute(result.target()));
    }
}
