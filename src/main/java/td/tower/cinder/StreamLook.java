package td.tower.cinder;

/**
 * How its stream is drawn: in Soulfire, as Dragon's Breath, and the bands its nodes give it, each sized as it pours.
 */
public record StreamLook(boolean soulfire, boolean dragonsBreath, FlameBand core, FlameBand middle) {

    /** How many times the cone's Flame its target, on the heading, takes: every band holds it. */
    public float coreFactor() {
        return 1f + this.core.moreAt(0, 1) + this.middle.moreAt(0, 1);
    }
}
