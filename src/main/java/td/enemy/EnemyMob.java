package td.enemy;

/**
 * One enemy walking a path. Consumers that only aim or only hit take {@link EnemyTarget} or
 * {@link HitReceiver}. Concrete types are reached only through {@link EnemyMobVisitor}.
 */
public interface EnemyMob extends EnemyTarget, HitReceiver {
    void doTick(int gameTime);

    <R> R accept(EnemyMobVisitor<R> visitor);

    int getHealth();

    float getSpeed();

    /**
     * What a tower can see. {@code INVISIBLE} is reachable only by area damage; a mob reports it
     * while an invisibility effect is active. {@code FLYING} is unused.
     */
    enum Type {
        NORMAL,
        FLYING,
        INVISIBLE
    }
}
