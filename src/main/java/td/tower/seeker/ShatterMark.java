package td.tower.seeker;

import td.wave.Vec2;

/** Where a freeze's ice just burst, for the shards the board throws out over {@code radius}: of {@code kind}'s ice. */
public record ShatterMark(Vec2 at, float radius, int startedAtTick, MissileKind kind) {
}
