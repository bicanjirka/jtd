package td.tower.mortar;

/**
 * Where the last nuke landed, how wide its blast was and the tick it did, for the white flash and
 * expanding ring the board draws.
 */
public record NukeFlash(double x, double y, float radius, int startedAtTick) {
}
