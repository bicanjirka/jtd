package td.enemy;

/**
 * A body that swells and settles back, with no facing of its own: one swell lasts {@code pulseTicks}, then it rests
 * at its normal size for {@code pauseTicks} before the next.
 *
 * @param scaleGain  how much larger it is at the peak of a swell, as a fraction of its size; above -1
 * @param pulseTicks the length of one swell
 * @param pauseTicks the rest between swells; {@code 0} swells back to back
 */
public record PulseMovement(float scaleGain, int pulseTicks, int pauseTicks) implements MovementBehavior {

    private static final float DEFAULT_SCALE_GAIN = 0.3f;
    private static final int DEFAULT_PULSE_TICKS = 30;

    public PulseMovement {
        if (scaleGain <= -1f || pulseTicks < 1 || pauseTicks < 0) {
            throw new IllegalArgumentException("A pulse needs a gain above -1, at least one tick to swell and no negative rest");
        }
    }

    /** A gentle swell with no rest between. */
    public PulseMovement() {
        this(DEFAULT_SCALE_GAIN, DEFAULT_PULSE_TICKS, 0);
    }

    /** The size multiplier {@code ticks} after the first swell began. */
    public float scaleAt(int ticks) {
        int phase = ticks % (this.pulseTicks + this.pauseTicks);
        if (phase >= this.pulseTicks) {
            return 1f;
        }
        return 1f + this.scaleGain * (float) Math.sin(Math.PI * phase / this.pulseTicks);
    }
}
