package td;

import java.util.Optional;

/**
 * One number the dev panel asks for before it puts something on a unit: what it is called, what it starts at and
 * the range it accepts. Every field takes a decimal; one that counts whole things rounds it.
 *
 * @param label   a word or two, as the panel shows it before the field
 * @param initial what the field holds until it is changed
 * @param min     the least it accepts
 * @param max     the most it accepts
 */
public record DevField(String label, float initial, float min, float max) {

    /** The most fields one entry asks for: the panel keeps that many slots. */
    public static final int MOST_PER_ENTRY = 3;

    private static final DevField SECONDS = new DevField("Secs", 5f, 0.05f, 600f);
    private static final DevField SHOTS = new DevField("Shots", 1f, 1f, 100f);

    /** How long it lasts. */
    public static DevField seconds() {
        return SECONDS;
    }

    /** How many shots it lasts. */
    public static DevField shots() {
        return SHOTS;
    }

    /** {@code initial} written as the field shows it: no decimals for a whole number. */
    public String initialText() {
        return format(this.initial);
    }

    /** What is wrong with {@code value} for this field; empty when it fits. */
    public Optional<String> problemWith(float value) {
        if (value >= this.min && value <= this.max) {
            return Optional.empty();
        }
        return Optional.of(this.label + " must be from " + format(this.min) + " to " + format(this.max));
    }

    private static String format(float value) {
        return value == Math.rint(value) ? String.valueOf(Math.round(value)) : String.valueOf(value);
    }
}
