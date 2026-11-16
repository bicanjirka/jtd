package td.stat;

import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;

/**
 * Where Beacons' daylight stands: an enemy inside gives its allies no aura. Towers add their circle in the towers
 * phase, after it is cleared; enemies read it in the next tick's enemies phase. Neither side knows the other.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class DaylightField {

    private final List<Circle> circles = new ArrayList<>();

    public void clear() {
        this.circles.clear();
    }

    public void add(double x, double y, double radius) {
        this.circles.add(new Circle(x, y, radius * radius));
    }

    /** Whether {@code (x, y)} stands in any circle. */
    public boolean covers(double x, double y) {
        for (Circle circle : this.circles) {
            double dx = circle.x() - x;
            double dy = circle.y() - y;
            if (dx * dx + dy * dy < circle.radius2()) {
                return true;
            }
        }
        return false;
    }

    private record Circle(double x, double y, double radius2) {
    }
}
