package td.tower.mortar;

import td.projectile.ShellLook;
import td.wave.Vec2;

/**
 * Where a blast just went off, for the detonation the board draws: its full-damage {@code core}, its {@code edge},
 * and what a Heavy's pick added to it ({@link HeavyRider#NONE} for every other shell).
 */
public record BlastMark(Vec2 at, float core, float edge, int startedAtTick, ShellLook look, HeavyRider rider) {

    /** A blast of {@code look} with nothing a Heavy's pick adds. */
    public static BlastMark of(Vec2 at, float core, float edge, int startedAtTick, ShellLook look) {
        return new BlastMark(at, core, edge, startedAtTick, look, HeavyRider.NONE);
    }

    /** The same blast as {@code added} marks it. */
    public BlastMark withRider(HeavyRider added) {
        return new BlastMark(this.at, this.core, this.edge, this.startedAtTick, this.look, added);
    }
}
