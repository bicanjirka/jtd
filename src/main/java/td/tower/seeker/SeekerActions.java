package td.tower.seeker;

import td.enemy.EnemyMob;

import java.util.List;

/** What a perk may make the Seeker do once a missile has landed, beyond what a payload may. */
public interface SeekerActions extends PayloadActions {

    /**
     * Hits every other enemy within {@code radiusCells} of {@code center} for {@code share} of the
     * Seeker's damage, as magic.
     *
     * @return the enemies it hit
     */
    List<EnemyMob> burst(EnemyMob center, float share, float radiusCells);

    /** Launches a missile from the tower at once, at whom it would pick now; its freeze rearms nothing. */
    void rearm();
}
