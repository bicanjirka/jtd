package td.ui;

import td.ui.render.ZoneArcDraw;
import td.ui.render.ZoneDraw;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Finds the stretches of each disc's rim that no other disc covers: the outline around a set of overlapping discs. */
final class ZoneOutlineTracer {

    private ZoneOutlineTracer() {
    }

    static List<ZoneArcDraw> trace(List<ZoneDraw> discs) {
        List<ZoneArcDraw> arcs = new ArrayList<>();
        for (int i = 0; i < discs.size(); i++) {
            if (!isSwallowed(discs, i)) {
                addOpenArcs(discs.get(i), coveredStretches(discs, i), arcs);
            }
        }
        return arcs;
    }

    private static boolean isSwallowed(List<ZoneDraw> discs, int index) {
        ZoneDraw disc = discs.get(index);
        for (int other = 0; other < discs.size(); other++) {
            if (other != index && isInside(disc, discs.get(other), other < index)) {
                return true;
            }
        }
        return false;
    }

    /** Of two identical discs each is inside the other, so only the later gives way and one rim is left. */
    private static boolean isInside(ZoneDraw disc, ZoneDraw other, boolean otherIsEarlier) {
        double distance = distanceBetween(disc, other);
        boolean identical = distance == 0 && disc.radius() == other.radius();
        return identical ? otherIsEarlier : other.radius() >= distance + disc.radius();
    }

    /** The parts of the rim's angle range, from 0 to a full turn, that the other discs cover, in order. */
    private static List<Stretch> coveredStretches(List<ZoneDraw> discs, int index) {
        ZoneDraw disc = discs.get(index);
        List<Stretch> covered = new ArrayList<>();
        for (int other = 0; other < discs.size(); other++) {
            if (other != index && crossRims(disc, discs.get(other))) {
                addCovered(disc, discs.get(other), covered);
            }
        }
        covered.sort(Comparator.comparingDouble(Stretch::from));
        return covered;
    }

    private static boolean crossRims(ZoneDraw disc, ZoneDraw other) {
        double distance = distanceBetween(disc, other);
        return distance < disc.radius() + other.radius() && distance > Math.abs(disc.radius() - other.radius());
    }

    /** A covered stretch that runs over the zero angle goes in as two, one at each end of the range. */
    private static void addCovered(ZoneDraw disc, ZoneDraw other, List<Stretch> covered) {
        double distance = distanceBetween(disc, other);
        double cosine = (disc.radius() * disc.radius() + distance * distance - other.radius() * other.radius())
                / (2 * disc.radius() * distance);
        double halfAngle = Math.acos(Math.clamp(cosine, -1.0, 1.0));
        double toward = Math.atan2(other.centerY() - disc.centerY(), other.centerX() - disc.centerX());
        double from = wrapped(toward - halfAngle);
        double to = from + 2 * halfAngle;
        if (to > Math.TAU) {
            covered.add(new Stretch(from, Math.TAU));
            covered.add(new Stretch(0, to - Math.TAU));
        } else {
            covered.add(new Stretch(from, to));
        }
    }

    private static void addOpenArcs(ZoneDraw disc, List<Stretch> covered, List<ZoneArcDraw> arcs) {
        double reached = 0;
        for (Stretch stretch : covered) {
            if (stretch.from() > reached) {
                arcs.add(arc(disc, reached, stretch.from()));
            }
            reached = Math.max(reached, stretch.to());
        }
        if (reached < Math.TAU) {
            arcs.add(arc(disc, reached, Math.TAU));
        }
    }

    private static ZoneArcDraw arc(ZoneDraw disc, double from, double to) {
        return new ZoneArcDraw(disc.centerX(), disc.centerY(), disc.radius(), (float) from, (float) (to - from));
    }

    private static double distanceBetween(ZoneDraw one, ZoneDraw other) {
        return Math.hypot(other.centerX() - one.centerX(), other.centerY() - one.centerY());
    }

    private static double wrapped(double radians) {
        return radians < 0 ? radians + Math.TAU : radians;
    }

    private record Stretch(double from, double to) {
    }
}
