package td.tower.sonar;

/**
 * What the beam is like.
 *
 * @param secondsPerRevolution the time a full turn, or a ping period, takes
 * @param twinShare            the damage share of a second beam opposite the first; {@code 0} for none
 * @param phased               whether the beam stops spinning and holds on one enemy
 */
public record BeamSpec(float secondsPerRevolution, float twinShare, boolean phased) {

    public static BeamSpec spinning(float secondsPerRevolution) {
        return new BeamSpec(secondsPerRevolution, 0f, false);
    }

    public BeamSpec withSecondsPerRevolution(float secondsPerRevolution) {
        return new BeamSpec(secondsPerRevolution, this.twinShare, this.phased);
    }

    public BeamSpec withTwinShare(float twinShare) {
        return new BeamSpec(this.secondsPerRevolution, twinShare, this.phased);
    }

    public boolean hasTwinBeam() {
        return this.twinShare > 0f;
    }

    public BeamSpec thatIsPhased() {
        return new BeamSpec(this.secondsPerRevolution, this.twinShare, true);
    }
}
