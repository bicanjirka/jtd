package td.enemy;

/**
 * Fires once, {@code afterTicks} after the last living {@code kinId} fell, if this mob still stands then. It arms only
 * once it has seen its kin alive, so a kin that has not come yet is not a fallen one, and a kin that comes back in time
 * stops the count.
 */
public record KinFallenTrigger(String kinId, int afterTicks) implements AbilityTrigger {

    public KinFallenTrigger {
        if (afterTicks < 1) {
            throw new IllegalArgumentException("A kin must be down at least one tick, was " + afterTicks);
        }
    }
}
