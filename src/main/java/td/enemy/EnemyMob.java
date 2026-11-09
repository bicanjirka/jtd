package td.enemy;

/**
 * One enemy walking a path. Consumers that only aim or only hit take {@link EnemyTarget} or
 * {@link HitReceiver}. Concrete types are reached only through {@link EnemyMobVisitor}.
 */
public interface EnemyMob extends EnemyTarget, HitReceiver, EnemyWalk {
    void doTick(int gameTime);

    <R> R accept(EnemyMobVisitor<R> visitor);

    int getHealth();

    /** Whether it died to damage: a leak or a hatch also ends a walk, but neither is a kill. */
    default boolean wasKilled() {
        return this.fate() == EnemyInspection.Fate.KILLED;
    }

    /** Whether it is still waiting out its spawn delay, not yet on the board. */
    boolean isInactive();

    /** Health left as a share of the maximum, from {@code 0} to {@code 1}. */
    float getHealthFraction();

    float getSpeed();

    /** Its body's radius in pixels. */
    float getBodyScale();

    /** Whether one of its abilities puts an effect on itself or on others: a heal, a shield, a vanishing. */
    boolean appliesEffects();

    /** Whether a tick would bring it health, from a trait or a heal. */
    boolean regenerates();

    /** A snapshot of it for display, taken on the thread that owns it. */
    EnemyInspection inspect();
}
