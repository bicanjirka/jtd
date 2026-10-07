package td.tower.sonar;

import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;
import td.util.TickRate;

/**
 * Whom the Sonar's beam may hit, how the beam moves, whom each revolution pings, and how deep a
 * hit fractures: the base rules reshaped by each perk it owns. Each {@code withX} is a copy.
 *
 * @param reach     the enemies the beam may hit
 * @param beam      how the beam moves
 * @param ping      whom each revolution Exposes
 * @param faultLine whether the Fractured its hits leave falls further and holds while Exposed
 */
public record SonarSpec(Reach reach, BeamSpec beam, PingRule ping, boolean faultLine) {

    /** An effect renewed every pass lasts this much longer than a revolution, so it never lapses in between. */
    private static final int PASS_MARGIN_TICKS = 2;

    /** The visible enemies in range, a beam that spins as {@code beam} says, and no ping. */
    public static SonarSpec from(Viewpoint view, BeamSpec beam) {
        return new SonarSpec(Reach.visible(view), beam, PingRule.none(), false);
    }

    public SonarSpec withReach(Reach reach) {
        return new SonarSpec(reach, this.beam, this.ping, this.faultLine);
    }

    public SonarSpec withBeam(BeamSpec beam) {
        return new SonarSpec(this.reach, beam, this.ping, this.faultLine);
    }

    public SonarSpec withPing(PingRule ping) {
        return new SonarSpec(this.reach, this.beam, ping, this.faultLine);
    }

    public SonarSpec withFaultLine() {
        return new SonarSpec(this.reach, this.beam, this.ping, true);
    }

    public Viewpoint view() {
        return this.reach.view();
    }

    /** How long a revolution, or a held beam's ping period, takes. */
    public int revolutionTicks() {
        return Math.max(1, Math.round(this.beam.secondsPerRevolution() * TickRate.TICKS_PER_SECOND));
    }

    /** How long an effect the beam renews each pass lasts: until the next pass. */
    public int untilNextPass() {
        return this.revolutionTicks() + PASS_MARGIN_TICKS;
    }

    /** How long a ping's Exposed lasts: as many passes as the ping rule says. */
    public int pingTicks() {
        return this.ping.passes() * this.revolutionTicks() + PASS_MARGIN_TICKS;
    }
}
