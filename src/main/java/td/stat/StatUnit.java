package td.stat;

import td.damage.DamageUnits;
import td.term.TermNumbers;
import td.util.TickRate;

/** How a stored stat value reads: the conversion to what the player sees, then its number and its unit word. */
public enum StatUnit {
    CELLS("cells"),
    /** Stored in ticks. */
    TICKS("s"),
    SECONDS("s"),
    /** A fraction, read whole: {@code "40%"}. */
    PERCENT(""),
    /** A fraction added on top, read with its sign: {@code "+10%"}. */
    SIGNED_PERCENT(""),
    POINTS(""),
    STACKS("stacks"),
    MISSILES("missiles"),
    COUNT(""),
    /** Stored in radians. */
    DEGREES(""),
    /** In hundredths of a point. */
    DAMAGE_PER_HIT("/hit"),
    /** In points. */
    DAMAGE_PER_SECOND("/s"),
    PER_SECOND("/s"),
    PER_TARGET("/target"),
    /** Hundredths of a point every hit loses, read as the loss: {@code "-2"}. */
    LOSS_PER_HIT("dmg/hit"),
    /** Hundredths of a point a tick. */
    HP_PER_SECOND("hp/s"),
    CELLS_PER_SECOND("cells/s"),
    /** Pixels a tick, read in cells a second. */
    PIXELS_PER_TICK("cells/s");

    private final String word;

    StatUnit(String word) {
        this.word = word;
    }

    /** The dim word after the number; empty when the number carries its unit itself. */
    public String word() {
        return this.word;
    }

    /** {@code stored} in the unit the player reads; {@code cellPixels} is the board's cell size. */
    public float shown(float stored, float cellPixels) {
        return switch (this) {
            case CELLS, SECONDS, PERCENT, SIGNED_PERCENT, POINTS, STACKS, MISSILES, COUNT, DAMAGE_PER_SECOND,
                 PER_SECOND, PER_TARGET, CELLS_PER_SECOND -> stored;
            case TICKS -> stored / TickRate.TICKS_PER_SECOND;
            case DEGREES -> (float) Math.toDegrees(stored);
            case DAMAGE_PER_HIT -> DamageUnits.inPoints(stored);
            case LOSS_PER_HIT -> -DamageUnits.inPoints(stored);
            case HP_PER_SECOND -> DamageUnits.inPoints(stored) * TickRate.TICKS_PER_SECOND;
            case PIXELS_PER_TICK -> stored * TickRate.TICKS_PER_SECOND / cellPixels;
        };
    }

    /** A shown value's number: {@code "4.5"}, {@code "40%"}, {@code "90°"}. */
    String number(float shown) {
        return switch (this) {
            case PERCENT -> TermNumbers.percent(shown);
            case SIGNED_PERCENT -> TermNumbers.signedPercent(shown);
            case POINTS, STACKS, MISSILES, COUNT -> TermNumbers.number(Math.round(shown));
            case DEGREES -> Math.round(shown) + "°";
            case CELLS, TICKS, SECONDS, DAMAGE_PER_HIT, DAMAGE_PER_SECOND, PER_SECOND, PER_TARGET, LOSS_PER_HIT,
                 HP_PER_SECOND, CELLS_PER_SECOND, PIXELS_PER_TICK -> TermNumbers.number(shown);
        };
    }

    /** A shown change's number, with its sign: {@code "+0.5"}, {@code "-10%"}, {@code "+12°"}. */
    String signed(float shown) {
        return switch (this) {
            case PERCENT, SIGNED_PERCENT -> TermNumbers.signedPercent(shown);
            case POINTS, STACKS, MISSILES, COUNT -> TermNumbers.signed(Math.round(shown));
            case DEGREES -> TermNumbers.signed(Math.round(shown)) + "°";
            case CELLS, TICKS, SECONDS, DAMAGE_PER_HIT, DAMAGE_PER_SECOND, PER_SECOND, PER_TARGET, LOSS_PER_HIT,
                 HP_PER_SECOND, CELLS_PER_SECOND, PIXELS_PER_TICK -> TermNumbers.signed(shown);
        };
    }
}
