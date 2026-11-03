package td.stat;

import java.util.Optional;

/**
 * A stat's value as the player reads it.
 *
 * @param number  {@code "4.5"}, {@code "40%"}
 * @param unit    the dim word after it; empty for none
 * @param meaning what the value does, when its stat says it inline
 */
public record StatReading(String number, String unit, Optional<String> meaning) {

    /** The number and its unit as one run of text: {@code "4.5 cells"}. */
    public String text() {
        return this.unit.isEmpty() ? this.number : this.number + " " + this.unit;
    }
}
