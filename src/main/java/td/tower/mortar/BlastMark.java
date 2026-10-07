package td.tower.mortar;

import td.projectile.ShellLook;
import td.wave.Vec2;

/** Where a blast just went off, for the fading flash the board draws. */
public record BlastMark(Vec2 at, float radius, int startedAtTick, ShellLook look) {
}
