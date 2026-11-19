package td.tower.mortar;

import td.wave.Vec2;

/**
 * Where the last nuke landed, how wide its blast was and the tick it did, for the white flash and
 * expanding ring the board draws.
 */
public record NukeFlash(Vec2 at, float radius, int startedAtTick) {
}
