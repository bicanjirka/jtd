package td.tower.splash;

import td.wave.Vec2;

/** A bolt that struck. */
public record Bolt(Vec2 from, Vec2 to, int tick, BoltKind kind) {
}
