package td.ui;

import td.ui.render.TowerLook;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static td.ui.TowerFoundations.at;
import static td.ui.TowerFoundations.box;
import static td.ui.TowerFoundations.circ;
import static td.ui.TowerFoundations.cos;
import static td.ui.TowerFoundations.minus;
import static td.ui.TowerFoundations.mv;
import static td.ui.TowerFoundations.ngon;
import static td.ui.TowerFoundations.poly;
import static td.ui.TowerFoundations.rot;
import static td.ui.TowerFoundations.sin;

/**
 * The table of tower icons: one flat mark per {@link TowerLook}, checked complete when this class loads. A fork or an
 * identity wears its tower's mark in its own colour.
 */
final class TowerIcons {

    /** How far the moving part's tone stands toward white from the tower's colour. */
    private static final double MOVING_LIGHTENING = 0.62;
    /** A seam takes the colour of the face an icon sits on, so it reads as a gap. */
    private static final Color SEAM = new Color(0x0d0e10);
    private static final Map<TowerLook, Icon> ICONS = checkedComplete(icons());

    private TowerIcons() {
    }

    /**
     * A mark in cells, centred on the origin: a silhouette in the tower's colour, the part of the tower that moves in
     * a lighter tone of it, and a seam cut where the two would run together.
     */
    record Icon(Shape body, Shape moving, Shape seam) {

        static Icon of(Shape body, Shape moving) {
            return new Icon(body, moving, new Path2D.Double());
        }

        Icon withSeam(Shape cut) {
            return new Icon(this.body, this.moving, cut);
        }
    }

    static Icon of(TowerLook look) {
        return ICONS.get(look);
    }

    /** {@code look}'s mark centred on the pen, a cell {@code cell} wide. */
    static void paint(Graphics2D g2, TowerLook look, double cell) {
        Icon icon = of(look);
        Color body = TowerLooks.color(look);
        AffineTransform pen = g2.getTransform();
        g2.scale(cell, cell);
        g2.setColor(body);
        g2.fill(icon.body());
        g2.setColor(TowerFoundations.mix(body, Color.WHITE, MOVING_LIGHTENING));
        g2.fill(icon.moving());
        g2.setColor(SEAM);
        g2.fill(icon.seam());
        g2.setTransform(pen);
    }

    static Map<TowerLook, Icon> checkedComplete(Map<TowerLook, Icon> icons) {
        Set<TowerLook> missing = EnumSet.allOf(TowerLook.class);
        missing.removeAll(icons.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Tower looks without an icon: " + missing);
        }
        return icons;
    }

    private static Map<TowerLook, Icon> icons() {
        Map<TowerLook, Icon> icons = new EnumMap<>(TowerLook.class);
        icons.put(TowerLook.SNIPER, sniper());
        Icon burst = burst();
        icons.put(TowerLook.SPLASH, burst);
        icons.put(TowerLook.STORMCALLER, burst);
        icons.put(TowerLook.HEXER, burst);
        icons.put(TowerLook.MORTAR, mortar());
        Icon scorcher = scorcher();
        icons.put(TowerLook.CINDER, scorcher);
        icons.put(TowerLook.SOULFIRE, scorcher);
        Icon aerie = aerie();
        icons.put(TowerLook.SEEKER, aerie);
        icons.put(TowerLook.RUBY, aerie);
        icons.put(TowerLook.SHADE, aerie);
        icons.put(TowerLook.WHITE, aerie);
        Icon obelisk = obelisk();
        icons.put(TowerLook.PULSE, obelisk);
        icons.put(TowerLook.BURDEN, obelisk);
        icons.put(TowerLook.GRAVITY, obelisk);
        icons.put(TowerLook.UMBRA, obelisk);
        icons.put(TowerLook.AURA, beacon());
        return icons;
    }

    private static Shape both(Shape... shapes) {
        Path2D.Double all = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (Shape shape : shapes) {
            all.append(shape, false);
        }
        return all;
    }

    /** A hollow triangle and the rifle across it. */
    private static Icon sniper() {
        return Icon.of(minus(ngon(0, 0.06, 3, 0.40, -90), ngon(0, 0.06, 3, 0.20, -90)),
                both(rot(box(0.14, 0, 0.62, 0.075), -40), circ(0, 0, 0.085)));
    }

    /** An onager from above, set to throw to the north-east: two rails and the frames across them, and the arm with its stone. */
    private static Icon burst() {
        Shape frame = both(box(0, -0.20, 0.80, 0.12), box(0, 0.20, 0.80, 0.12), box(0.30, 0, 0.12, 0.52),
                box(-0.30, 0, 0.12, 0.52));
        Shape arm = both(box(-0.08, 0, 0.46, 0.10), circ(-0.30, 0, 0.13));
        return Icon.of(rot(frame, -40), rot(arm, -40)).withSeam(rot(circ(-0.30, 0, 0.055), -40));
    }

    /**
     * The strider from above: four jointed legs, each bent at its knee to a pointed foot, and the fat barrel seen down
     * its bore with a shell in it.
     */
    private static Icon mortar() {
        Shape thigh = both(box(0.24, 0, 0.22, 0.09), circ(0.35, 0, 0.065));
        Shape shin = mv(rot(poly(0, -0.045, 0.15, 0, 0, 0.045), 35), 0.35, 0);
        Shape leg = both(thigh, shin);
        Shape legs = both(rot(leg, 45), rot(leg, 135), rot(leg, 225), rot(leg, 315));
        return Icon.of(legs, both(minus(circ(0, 0, 0.26), circ(0, 0, 0.14)), circ(0.02, 0.02, 0.075)))
                .withSeam(minus(circ(0, 0, 0.14), circ(0.02, 0.02, 0.075)));
    }

    /** The octagon body with its three tanks seen from above, and the flared nozzle across it. */
    private static Icon scorcher() {
        Shape nozzle = mv(poly(0, -0.045, 0.3, -0.045, 0.42, -0.11, 0.42, 0.11, 0.3, 0.045, 0, 0.045), -0.02, 0);
        return Icon.of(ngon(-0.06, 0.06, 8, 0.34, 22.5), both(rot(nozzle, -40), circ(-0.06, 0.06, 0.09)))
                .withSeam(both(circ(-0.2, 0.0, 0.055), circ(-0.2, 0.2, 0.055), circ(0.0, 0.2, 0.055)));
    }

    /** A perch in its ring and the three missiles of its nest on the ring. */
    private static Icon aerie() {
        Shape missile = rot(poly(0.12, 0, -0.08, 0.075, -0.04, 0, -0.08, -0.075), 90);
        return Icon.of(both(minus(circ(0, 0, 0.33), circ(0, 0, 0.25)), ngon(0, 0, 6, 0.16, 0)),
                both(at(missile, -90, 0.40), at(missile, 30, 0.40), at(missile, 150, 0.40), ngon(0, 0, 6, 0.07, 30)));
    }

    /** A stone seen from above, two of its faces lit. */
    private static Icon obelisk() {
        return Icon.of(box(0, 0, 0.62, 0.62), both(poly(0, 0, -0.2, -0.2, 0.2, -0.2), poly(0, 0, -0.2, -0.2, -0.2, 0.2)))
                .withSeam(minus(box(0, 0, 0.435, 0.435), box(0, 0, 0.365, 0.365)));
    }

    /** A lantern from above: every other pane lit, and its light. */
    private static Icon beacon() {
        int panes = 4;
        Shape[] lit = new Shape[panes + 1];
        for (int i = 0; i < panes; i++) {
            double from = 22.5 + i * 90;
            lit[i] = poly(cos(from) * 0.31, sin(from) * 0.31, cos(from + 45) * 0.31, sin(from + 45) * 0.31, 0, 0);
        }
        lit[panes] = ngon(0, 0, 4, 0.12, 0);
        return Icon.of(ngon(0, 0, 8, 0.40, 22.5), both(lit));
    }
}
