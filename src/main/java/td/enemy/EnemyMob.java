package td.enemy;

/**
 * One enemy walking a path. Consumers that only aim or only hit take {@link EnemyTarget} or
 * {@link HitReceiver}. Concrete types are reached only through {@link EnemyMobVisitor}.
 */
public interface EnemyMob extends EnemyTarget, HitReceiver, EnemyWalk {
    void doTick(int gameTime);

    <R> R accept(EnemyMobVisitor<R> visitor);

    int getHealth();

    /** Health left as a share of the maximum, from {@code 0} to {@code 1}. */
    float getHealthFraction();

    Rank getRank();

    float getSpeed();
}
