package td.tower.sniper;

import td.wave.Vec2;

/**
 * Where a shot went and what it was.
 *
 * @param to       where its line ends, in pixels
 * @param critical whether it crit its target
 * @param shot     what it was
 * @param landedAt the tick it arrived
 */
public record ShotTrace(Vec2 to, boolean critical, SniperShot shot, int landedAt) {

    public float toX() {
        return (float) this.to.x();
    }

    public float toY() {
        return (float) this.to.y();
    }
}
