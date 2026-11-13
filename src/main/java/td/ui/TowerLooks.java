package td.ui;

import td.ui.render.TowerLook;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** The table of tower looks: one row per {@link TowerLook}, checked complete when this class loads. */
final class TowerLooks {

    private static final Map<TowerLook, Row> ROWS = checkedComplete(rows());
    // Some bodies are constructive-area operations far too slow to repeat for every tower on every paint. Sizes
    // follow the cell size, so the maps stay small.
    private static final Map<SizedLook, Shape> BODIES = new ConcurrentHashMap<>();
    // Stroking a body's outline on every paint cost more than building it did.
    private static final Map<SizedLook, Shape> BODY_OUTLINES = new ConcurrentHashMap<>();

    private TowerLooks() {
    }

    /** A shape centred on the origin, at a given size. */
    @FunctionalInterface
    interface SizedShape {
        Shape at(float size);
    }

    /**
     * A tower's colour, its body (one flat closed symbol naming what it does) and its turret head. A head is drawn
     * pointing along {@code +X}, so rotating the graphics aims it; a spinning head must reach past its body's outline,
     * or the spin is invisible.
     */
    record Row(Color color, SizedShape body, SizedShape head) {
    }

    private record SizedLook(TowerLook look, float size) {
    }

    static Row of(TowerLook look) {
        return ROWS.get(look);
    }

    static Color color(TowerLook look) {
        return of(look).color();
    }

    static Shape body(TowerLook look, float size) {
        return BODIES.computeIfAbsent(new SizedLook(look, size), key -> of(key.look()).body().at(key.size()));
    }

    /** The body's outline at the default stroke, as an area to fill. */
    static Shape bodyOutline(TowerLook look, float size) {
        return BODY_OUTLINES.computeIfAbsent(new SizedLook(look, size),
                key -> new BasicStroke().createStrokedShape(body(key.look(), key.size())));
    }

    static Shape head(TowerLook look, float size) {
        return of(look).head().at(size);
    }

    static Map<TowerLook, Row> checkedComplete(Map<TowerLook, Row> rows) {
        Set<TowerLook> missing = EnumSet.allOf(TowerLook.class);
        missing.removeAll(rows.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Tower looks without a row: " + missing);
        }
        return rows;
    }

    private static Map<TowerLook, Row> rows() {
        Map<TowerLook, Row> rows = new EnumMap<>(TowerLook.class);
        rows.put(TowerLook.SNIPER, new Row(Color.GREEN, s -> Java2DFrameRenderer.triangleShape(s, true),
                s -> new Rectangle2D.Float(0, -s * 0.22f, s * 1.3f, s * 0.44f)));
        rows.put(TowerLook.SPLASH, new Row(Color.RED, TowerLooks::splashBody, TowerLooks::splashHead));
        rows.put(TowerLook.STORMCALLER, new Row(new Color(80, 150, 255), TowerLooks::splashBody, TowerLooks::splashHead));
        rows.put(TowerLook.HEXER, new Row(new Color(170, 80, 230), TowerLooks::splashBody, TowerLooks::splashHead));
        rows.put(TowerLook.PULSE, new Row(Color.ORANGE, s -> Java2DFrameRenderer.starShape(5, s, s * 0.45f),
                TowerLooks::orbitingStar));
        rows.put(TowerLook.AURA, new Row(Color.WHITE, TowerLooks::pulsar, Java2DFrameRenderer::circleShape));
        rows.put(TowerLook.MORTAR, new Row(new Color(139, 90, 43), Java2DFrameRenderer::diamondShape,
                Java2DFrameRenderer::headArrowShape));
        rows.put(TowerLook.SEEKER, new Row(new Color(80, 180, 255), TowerLooks::kite, Java2DFrameRenderer::headArrowShape));
        rows.put(TowerLook.CINDER, new Row(new Color(255, 90, 30), TowerLooks::flame, Java2DFrameRenderer::headArrowShape));
        return rows;
    }

    private static Shape splashBody(float size) {
        return Java2DFrameRenderer.ringShape(size, 0.55f);
    }

    private static Shape splashHead(float size) {
        return new Rectangle2D.Float(0, -size * 0.42f, size * 0.95f, size * 0.84f);
    }

    /** A star held out from the centre, so it circles the body as the head spins. */
    private static Shape orbitingStar(float size) {
        Shape moon = Java2DFrameRenderer.starShape(5, size * 0.9f, size * 0.9f * 0.45f);
        return AffineTransform.getTranslateInstance(size * 2.0, 0).createTransformedShape(moon);
    }

    /** A bright core inside a detached ring. */
    private static Shape pulsar(float size) {
        Area pulsar = new Area(Java2DFrameRenderer.ringShape(size, 0.72f));
        pulsar.add(new Area(Java2DFrameRenderer.circleShape(size * 0.36f)));
        return pulsar;
    }

    /** A concave arrowhead. */
    private static Shape kite(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(size, 0);
        p.lineTo(-size * 0.6f, -size * 0.7f);
        p.lineTo(-size * 0.2f, 0);
        p.lineTo(-size * 0.6f, size * 0.7f);
        p.closePath();
        return p;
    }

    /** A teardrop. */
    private static Shape flame(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, -size);
        p.curveTo(size * 0.9f, -size * 0.2f, size * 0.6f, size * 0.5f, 0, size);
        p.curveTo(-size * 0.6f, size * 0.5f, -size * 0.9f, -size * 0.2f, 0, -size);
        p.closePath();
        return p;
    }
}
