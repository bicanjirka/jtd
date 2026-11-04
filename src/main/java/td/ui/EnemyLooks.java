package td.ui;

import td.ui.render.EnemyLook;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** The table of enemy looks: one row per {@link EnemyLook}, checked complete when this class loads. */
final class EnemyLooks {

    private static final Map<EnemyLook, Row> ROWS = checkedComplete(rows());

    private EnemyLooks() {
    }

    /** A shape centred on the origin, at a given scale. */
    @FunctionalInterface
    interface ScaledShape {
        Shape at(float scale);
    }

    /** The outlines a dying body bursts into, once its scale has grown to {@code grown}. */
    @FunctionalInterface
    interface FadeOutlines {
        List<Shape> at(float scale, float grown);
    }

    /** A second shape over a body in its own colour, such as eyes, horns or a glowing core. */
    record Detail(Color color, ScaledShape shape) {
    }

    /** One look: its colour, its body, any detail over that body, and how it bursts. */
    record Row(Color color, ScaledShape body, Optional<Detail> detail, FadeOutlines fade) {

        private static Row of(Color color, ScaledShape body) {
            return new Row(color, body, Optional.empty(), (scale, grown) -> List.of(body.at(grown)));
        }

        private Row withDetail(Color detailColor, ScaledShape detailShape) {
            return new Row(this.color, this.body, Optional.of(new Detail(detailColor, detailShape)), this.fade);
        }

        private Row withFade(FadeOutlines outlines) {
            return new Row(this.color, this.body, this.detail, outlines);
        }
    }

    static Row of(EnemyLook look) {
        return ROWS.get(look);
    }

    static Map<EnemyLook, Row> checkedComplete(Map<EnemyLook, Row> rows) {
        Set<EnemyLook> missing = EnumSet.allOf(EnemyLook.class);
        missing.removeAll(rows.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Enemy looks without a row: " + missing);
        }
        return rows;
    }

    private static List<Shape> crossingSquares(float scale, float grown) {
        return List.of(new Rectangle2D.Float(-grown, -scale, grown * 2, scale * 2),
                new Rectangle2D.Float(-scale, -grown, scale * 2, grown * 2));
    }

    private static List<Shape> crossingTriangles(float scale, float grown) {
        return List.of(Java2DFrameRenderer.triangleShape(grown, false), Java2DFrameRenderer.triangleShape(grown, true));
    }

    private static Shape square(float scale) {
        return new Rectangle2D.Float(-scale, -scale, scale * 2, scale * 2);
    }

    private static Shape triangle(float scale) {
        return Java2DFrameRenderer.triangleShape(scale, true);
    }

    private static Shape star(int points, float outer, float inner) {
        return Java2DFrameRenderer.starShape(points, outer, inner);
    }

    private static Shape circle(float scale) {
        return Java2DFrameRenderer.circleShape(scale);
    }

    /** A bolt that strikes downward, turned to strike along a body's facing. */
    private static Shape sidewaysBolt(float scale) {
        return AffineTransform.getRotateInstance(-Math.PI / 2).createTransformedShape(EnemyShapes.boltShape(scale));
    }

    private static Map<EnemyLook, Row> rows() {
        Map<EnemyLook, Row> rows = new EnumMap<>(EnemyLook.class);
        rows.put(EnemyLook.CIRCLE, Row.of(Color.CYAN, EnemyLooks::circle));
        rows.put(EnemyLook.GHOST, Row.of(Color.LIGHT_GRAY, EnemyShapes::ghostShape)
                .withDetail(new Color(70, 70, 95), s -> EnemyShapes.eyesShape(s, -s * 0.2f, s * 0.16f)));
        rows.put(EnemyLook.SQUARE, Row.of(Color.PINK, EnemyLooks::square).withFade(EnemyLooks::crossingSquares));
        rows.put(EnemyLook.TRIANGLE, Row.of(Color.YELLOW, EnemyLooks::triangle).withFade(EnemyLooks::crossingTriangles));
        rows.put(EnemyLook.WARDEN, Row.of(new Color(139, 0, 0), s -> star(8, s, s * 0.55f))
                .withDetail(new Color(240, 190, 60), s -> circle(s * 0.28f)));
        rows.put(EnemyLook.WARDEN_EGG, Row.of(new Color(139, 0, 0), EnemyShapes::wardenEggShape));
        rows.put(EnemyLook.MENDER, Row.of(new Color(120, 220, 150), EnemyShapes::crossShape));
        rows.put(EnemyLook.JUGGERNAUT, Row.of(new Color(120, 135, 160), EnemyLooks::square)
                .withDetail(new Color(85, 95, 115), s -> new Rectangle2D.Float(-s * 0.55f, -s * 0.55f, s * 1.1f, s * 1.1f))
                .withFade(EnemyLooks::crossingSquares));
        rows.put(EnemyLook.MITE, Row.of(new Color(190, 140, 80), EnemyLooks::circle));
        rows.put(EnemyLook.COURIER, Row.of(new Color(170, 255, 80), EnemyLooks::triangle).withFade(EnemyLooks::crossingTriangles));
        rows.put(EnemyLook.SKIRMISHER, Row.of(new Color(80, 220, 200), Java2DFrameRenderer::diamondShape));
        rows.put(EnemyLook.SHIELDBEARER, Row.of(new Color(90, 150, 255), EnemyShapes::crossShape));
        rows.put(EnemyLook.SALAMANDER, Row.of(new Color(255, 120, 30), EnemyShapes::lizardShape)
                .withDetail(new Color(255, 215, 60), s -> new Ellipse2D.Float(-s * 0.6f, -s * 0.1f, s * 1.2f, s * 0.2f)));
        rows.put(EnemyLook.YETI, Row.of(new Color(200, 235, 255), s -> star(16, s, s * 0.84f))
                .withDetail(new Color(50, 80, 120), s -> EnemyShapes.eyesShape(s, -s * 0.15f, s * 0.13f)));
        rows.put(EnemyLook.MAGMA, Row.of(new Color(220, 45, 35), EnemyShapes::magmaShape)
                .withDetail(new Color(255, 170, 60), s -> circle(s * 0.4f)));
        rows.put(EnemyLook.GOLEM, Row.of(new Color(150, 140, 125), s -> star(6, s, s * 0.87f)));
        rows.put(EnemyLook.JAMMER, Row.of(new Color(230, 70, 190), EnemyShapes::jammerShape)
                .withDetail(new Color(255, 225, 250), s -> circle(s * 0.3f)));
        rows.put(EnemyLook.REAVER, Row.of(new Color(205, 150, 70), EnemyShapes::barbShape));
        rows.put(EnemyLook.COLOSSUS, Row.of(new Color(95, 105, 128), s -> star(8, s, s * 0.93f))
                .withDetail(new Color(255, 140, 40), s -> new RoundRectangle2D.Float(-s * 0.6f, -s * 0.16f, s * 1.2f, s * 0.32f,
                        s * 0.2f, s * 0.2f)));
        rows.put(EnemyLook.MOLTEN_CORE, Row.of(new Color(255, 110, 20), s -> star(10, s, s * 0.7f))
                .withDetail(new Color(255, 235, 120), s -> circle(s * 0.4f)));
        rows.put(EnemyLook.EMBER, Row.of(new Color(255, 175, 40), EnemyShapes::emberShape));
        rows.put(EnemyLook.MONOLITH, Row.of(new Color(80, 78, 105), EnemyShapes::slabShape)
                .withDetail(new Color(175, 120, 255), Java2DFrameRenderer::runeShape));
        rows.put(EnemyLook.ECLIPSE, Row.of(new Color(125, 70, 200), EnemyShapes::crescentShape));
        rows.put(EnemyLook.MIRAGE, Row.of(new Color(185, 155, 255), EnemyShapes::twinDiamondShape));
        rows.put(EnemyLook.MIRROR_IMAGE, Row.of(new Color(220, 210, 255), Java2DFrameRenderer::diamondShape));
        rows.put(EnemyLook.HERALD, Row.of(new Color(255, 205, 60), EnemyShapes::sweptArrowShape));
        rows.put(EnemyLook.TALOS, Row.of(new Color(205, 135, 60), EnemyShapes::giantShape)
                .withDetail(new Color(200, 25, 30), s -> new Rectangle2D.Float(-s * 0.07f, -s * 0.35f, s * 0.14f, s * 1.2f)));
        rows.put(EnemyLook.MINOTAUR, Row.of(new Color(115, 60, 35), EnemyShapes::bullHeadShape)
                .withDetail(new Color(235, 225, 200), EnemyShapes::hornsShape));
        rows.put(EnemyLook.BRONZE_BULL, Row.of(new Color(190, 120, 50), EnemyShapes::bullHeadShape)
                .withDetail(new Color(110, 65, 25), EnemyShapes::hornsShape));
        rows.put(EnemyLook.GLASS_SENTINEL, Row.of(new Color(170, 228, 240), Java2DFrameRenderer::crystalShape)
                .withDetail(new Color(245, 255, 255), s -> Java2DFrameRenderer.shardShape(s * 0.5f, 0)));
        rows.put(EnemyLook.SANDLING, Row.of(new Color(225, 190, 120), EnemyShapes::sandGrainShape));
        rows.put(EnemyLook.TIMEKEEPER, Row.of(new Color(205, 170, 95), EnemyLooks::circle)
                .withDetail(new Color(55, 40, 20), EnemyShapes::clockHandsShape));
        rows.put(EnemyLook.KEEPER, Row.of(new Color(165, 210, 225), EnemyShapes::hourglassShape)
                .withDetail(new Color(235, 190, 100), EnemyShapes::hourglassSandShape));
        rows.put(EnemyLook.GALE, Row.of(new Color(180, 232, 222), EnemyShapes::galeShape)
                .withDetail(new Color(90, 160, 150), s -> circle(s * 0.22f)));
        rows.put(EnemyLook.RAINDROP, Row.of(new Color(110, 170, 255), EnemyShapes::raindropShape));
        rows.put(EnemyLook.RAINCLOUD, Row.of(new Color(150, 165, 192), EnemyShapes::cloudShape)
                .withDetail(new Color(95, 150, 240), EnemyShapes::rainStreaksShape));
        rows.put(EnemyLook.LIGHTNING, Row.of(new Color(255, 235, 120), EnemyLooks::sidewaysBolt)
                .withDetail(Color.WHITE, s -> sidewaysBolt(s * 0.45f)));
        rows.put(EnemyLook.THUNDER, Row.of(new Color(70, 80, 118), EnemyShapes::thunderheadShape)
                .withDetail(new Color(255, 225, 80), s -> AffineTransform.getTranslateInstance(0, s * 0.55f)
                        .createTransformedShape(EnemyShapes.boltShape(s * 0.6f))));
        rows.put(EnemyLook.HORNET, Row.of(new Color(235, 115, 30), EnemyShapes::hornetShape)
                .withDetail(new Color(35, 22, 10), EnemyShapes::hornetStripesShape));
        rows.put(EnemyLook.COCOON, Row.of(new Color(220, 210, 185), s -> new Ellipse2D.Float(-s * 0.7f, -s * 1.15f, s * 1.4f, s * 2.3f))
                .withDetail(new Color(150, 130, 95), EnemyShapes::cocoonBandsShape));
        rows.put(EnemyLook.LARVA, Row.of(new Color(240, 232, 205), EnemyShapes::grubShape)
                .withDetail(new Color(130, 85, 40), s -> new Ellipse2D.Float(s * 0.62f, -s * 0.18f, s * 0.36f, s * 0.36f)));
        rows.put(EnemyLook.NURSE, Row.of(new Color(240, 220, 150), s -> star(6, s, s * 0.87f))
                .withDetail(new Color(205, 120, 30), s -> EnemyShapes.crossShape(s * 0.45f)));
        rows.put(EnemyLook.HIVE_GUARD, Row.of(new Color(170, 105, 35), EnemyShapes::beetleShape)
                .withDetail(new Color(70, 40, 12), s -> new Rectangle2D.Float(-s * 0.85f, -s * 0.05f, s * 1.4f, s * 0.1f)));
        rows.put(EnemyLook.DRONE, Row.of(new Color(245, 190, 45), EnemyShapes::beeShape)
                .withDetail(new Color(70, 40, 10), EnemyShapes::beeStripesShape));
        return rows;
    }
}
