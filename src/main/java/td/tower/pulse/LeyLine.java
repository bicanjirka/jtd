package td.tower.pulse;

import td.wave.Path;
import td.wave.Vec2;

import java.util.Comparator;
import java.util.List;

/**
 * A beam from one field's centre to another's, in board pixels. Pure geometry: how often it crosses the paths, how much
 * of them it covers, and whether a point stands on it.
 *
 * @param fromX where it starts, the casting field's centre
 * @param fromY where it starts
 * @param toX   where it ends, the hosting field's centre
 * @param toY   where it ends
 */
public record LeyLine(float fromX, float fromY, float toX, float toY) {

    /** The better of two beams: more crossings, then more path covered, then shorter. */
    public static final Comparator<Scored> BETTER_FIRST = Comparator.comparingInt(Scored::crossings).reversed()
            .thenComparing(Comparator.comparingDouble(Scored::covered).reversed())
            .thenComparingDouble(scored -> scored.line().length());

    /** A beam with what it does to the paths. */
    public record Scored(LeyLine line, int crossings, double covered) {

        /** Whether the beam reaches the paths at all. */
        public boolean touchesAPath() {
            return this.crossings > 0 || this.covered > 0;
        }
    }

    public double length() {
        return Math.hypot(this.toX - this.fromX, this.toY - this.fromY);
    }

    /** How often this beam crosses {@code paths}, and how much of their length lies within {@code band} of it. */
    public Scored scoredOn(List<Path> paths, double band) {
        int crossings = 0;
        double covered = 0;
        for (Path path : paths) {
            List<Vec2> points = path.points();
            for (int i = 1; i < points.size(); i++) {
                Vec2 a = points.get(i - 1);
                Vec2 b = points.get(i);
                if (this.crosses(a, b)) {
                    crossings++;
                }
                if (this.distanceTo((a.x() + b.x()) / 2, (a.y() + b.y()) / 2) <= band) {
                    covered += Math.hypot(b.x() - a.x(), b.y() - a.y());
                }
            }
        }
        return new Scored(this, crossings, covered);
    }

    /** How far ({@code x}, {@code y}) is from the nearest point of the beam. */
    public double distanceTo(double x, double y) {
        double dx = this.toX - this.fromX;
        double dy = this.toY - this.fromY;
        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared == 0 ? 0 : Math.clamp(((x - this.fromX) * dx + (y - this.fromY) * dy) / lengthSquared, 0, 1);
        return Math.hypot(x - (this.fromX + t * dx), y - (this.fromY + t * dy));
    }

    private boolean crosses(Vec2 a, Vec2 b) {
        double d1 = side(a.x(), a.y());
        double d2 = side(b.x(), b.y());
        double d3 = cross(b.x() - a.x(), b.y() - a.y(), this.fromX - a.x(), this.fromY - a.y());
        double d4 = cross(b.x() - a.x(), b.y() - a.y(), this.toX - a.x(), this.toY - a.y());
        return (d1 > 0) != (d2 > 0) && (d3 > 0) != (d4 > 0);
    }

    private double side(double x, double y) {
        return cross(this.toX - this.fromX, this.toY - this.fromY, x - this.fromX, y - this.fromY);
    }

    private static double cross(double ax, double ay, double bx, double by) {
        return ax * by - ay * bx;
    }
}
