package td.enemy;

/**
 * Turns health back to what it was {@code windowTicks} ago, on the caster or on the allies around it; health only
 * ever comes back, never goes. It counts as a heal: what keeps heals out keeps it out too, and under Inversion what it
 * would give back is dealt as damage instead.
 */
public record RewindAction(int windowTicks, EffectTarget target) implements AbilityAction {

    /** The furthest back a rewind can reach: a mob remembers its health this far. */
    public static final int LONGEST_TICKS = 200;

    public RewindAction {
        if (windowTicks < 1 || windowTicks > LONGEST_TICKS) {
            throw new IllegalArgumentException("A rewind reaches between 1 and " + LONGEST_TICKS + " ticks back, was "
                    + windowTicks);
        }
    }

    /** It restores, so a hex that turns restoring against its target picks it. */
    @Override
    public boolean appliesEffects() {
        return true;
    }
}
