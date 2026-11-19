package td.tower.cinder;

import td.wave.Vec2;

/** A Flashover from a hot enemy at {@code from} to one beside it at {@code to}, in board pixels. */
public record Flash(Vec2 from, Vec2 to) {
}
