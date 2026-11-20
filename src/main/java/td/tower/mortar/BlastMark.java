package td.tower.mortar;

import td.projectile.ShellLook;
import td.wave.Vec2;

/**
 * Where a blast just went off, for the detonation the board draws: its full-damage {@code core}, its {@code edge},
 * and whether it threw {@code fragments}.
 */
public record BlastMark(Vec2 at, float core, float edge, int startedAtTick, ShellLook look, boolean fragments) {

    /** A blast of {@code look} that throws no fragments. */
    public static BlastMark of(Vec2 at, float core, float edge, int startedAtTick, ShellLook look) {
        return new BlastMark(at, core, edge, startedAtTick, look, false);
    }

    /** The same blast throwing fragments out over it when {@code thrown}. */
    public BlastMark withFragments(boolean thrown) {
        return new BlastMark(this.at, this.core, this.edge, this.startedAtTick, this.look, thrown);
    }
}
