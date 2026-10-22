package td.enemy;

import td.effect.EffectTemplate;

/**
 * What {@link AbilityEvaluator} needs from one mob on one tick - a narrow seam rather than the mob,
 * so evaluation is testable against a fake.
 */
public interface AbilityContext {

    /** Current health over max health, in {@code [0, 1]}. */
    float healthFraction();

    /** {@code 0} on the tick a hit landed. */
    int ticksSinceLastHit();

    /** True only on the tick this mob's death is first observed; reaching the end of the path is not a death. */
    boolean justDied();

    /**
     * True only on the tick a survived critical hit is first observed. Captured on the mob's next
     * tick, since the hit can land in another phase of the tick.
     */
    boolean justTookCriticalHit();

    /** Like {@link #justTookCriticalHit()}, for a hit it dodged. */
    boolean justDodged();

    /** Like {@link #justTookCriticalHit()}, for any damage. */
    boolean justTookDamage();

    void applyEffect(EffectTemplate template, EffectTarget target);

    /** Spawns what {@code spawn} names, optionally consuming this mob. */
    void spawnEnemies(SpawnEnemiesAction spawn);

    /** Leaves the zone {@code zone} describes where this mob stands. */
    void leaveZone(LeaveZoneAction zone);
}
