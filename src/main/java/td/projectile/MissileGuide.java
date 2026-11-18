package td.projectile;

import td.enemy.EnemyMob;

import java.util.Optional;

/** What the tower that launched a missile decides about it: whom it keeps hunting, whom it turns to, and what it does on arrival. */
public interface MissileGuide {

    /** Whether the missile keeps hunting {@code target}, its pick until now. */
    boolean keeps(EnemyMob target);

    /** Whom a missile at ({@code x}, {@code y}) with nothing worth hunting turns to; empty to fly home. */
    Optional<EnemyMob> retarget(double x, double y);

    /** The missile reached {@code target}. */
    void land(EnemyMob target, int gameTime);

    /** The missile flew home with nothing to hunt. */
    void arriveHome();
}
