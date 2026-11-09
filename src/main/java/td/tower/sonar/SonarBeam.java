package td.tower.sonar;

import td.util.TickRate;

import java.util.OptionalDouble;

/**
 * A beam that crosses the enemies around the Sonar, however it moves. Pure geometry: it knows
 * bearings, never enemies.
 */
public interface SonarBeam {

    /**
     * Moves the beam one tick. A beam that holds on one enemy turns to {@code focusBearing}; one that
     * spins ignores it.
     */
    void advance(OptionalDouble focusBearing);

    /**
     * How much of its damage the beam deals to an enemy at {@code bearing} this tick: {@code 1} for
     * the main beam, a share for a second one, {@code 0} when no beam crosses it.
     */
    float strikeShare(double bearing);

    /** How fast a beam turns to its rest pose once a wave is over. */
    double SETTLE_RADIANS_PER_TICK = Math.PI / TickRate.TICKS_PER_SECOND;

    /** Turns one tick toward {@code restRadians}, hitting nothing: what a beam does between waves. */
    void settle(double restRadians);

    /** Whether the last tick finished a revolution, or a held beam's ping period. */
    boolean completedRevolution();

    /** The main beam's heading between two ticks. */
    double headingAt(double interpolationAlpha);

    /** This beam as {@code spec} describes it, carrying on from where this one is. */
    SonarBeam reshaped(BeamSpec spec);
}
