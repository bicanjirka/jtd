package td.ui;

import td.ui.render.EnemyBarLook;
import td.ui.render.EnemyDotLook;
import td.ui.render.EnemyLook;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
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

    /** How closely a curve is followed when measuring a shape's reach at scale one; read while the table builds. */
    private static final double REACH_FLATNESS = 0.001;
    private static final Map<EnemyLook, Row> ROWS = checkedComplete(rows());
    private static final Color SHIELD_BAR = new Color(0xFF, 0xDC, 0x78);
    private static final Color HEALTH_BAR = new Color(0xE8, 0xE4, 0xDA);
    private static final Color DEBUFF_DOT = new Color(0xE8, 0xA0, 0xC8);
    private static final Color SPOTTED_DOT = new Color(0xF0, 0xD0, 0x60);
    private static final Color BUFF_DOT = new Color(0xA8, 0xC8, 0xFF);

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

    /**
     * How a body stands on the board, every measure a share of its scale: how tall its side is, how far it hovers above
     * the ground, the smaller copy of itself it carries as a cap and how far that rises, how far its detail rises, and
     * whether it shines or glows.
     */
    record Form(float height, float hover, float capShare, float capHeight, float detailHeight, boolean glossy, boolean glowing) {

        private static final float USUAL_HEIGHT = 0.26f;
        private static final float FLYER_HEIGHT = 0.12f;

        private static Form standing() {
            return new Form(USUAL_HEIGHT, 0f, 0f, 0f, 0f, false, false);
        }

        /** How far the art rises above the body's own outline. */
        float rise() {
            return this.hover + this.capHeight + this.detailHeight;
        }
    }

    /**
     * One look: its colour as the light falls on it, its body, any detail over that body, how it bursts, how far its
     * art reaches from its centre at scale one, whichever way it turns, and how it stands.
     */
    record Row(Tones tones, ScaledShape body, Optional<Detail> detail, FadeOutlines fade, float reach, Form form) {

        private static Row of(Color color, ScaledShape body) {
            // A fade grows through scales no body stays at, so it asks the plain shape.
            return new Row(Tones.of(color), new RememberedShape(body), Optional.empty(), (scale, grown) -> List.of(body.at(grown)),
                    reachOf(body), Form.standing());
        }

        Color color() {
            return this.tones.body();
        }

        private Row withDetail(Color detailColor, ScaledShape detailShape) {
            return new Row(this.tones, this.body, Optional.of(new Detail(detailColor, new RememberedShape(detailShape))), this.fade,
                    Math.max(this.reach, reachOf(detailShape)), this.form);
        }

        private Row withFade(FadeOutlines outlines) {
            return new Row(this.tones, this.body, this.detail, outlines, this.reach, this.form);
        }

        private Row withForm(Form shaped) {
            return new Row(this.tones, this.body, this.detail, this.fade, this.reach, shaped);
        }

        /** Stands this tall. */
        private Row standing(float height) {
            return this.withForm(new Form(height, this.form.hover, this.form.capShare, this.form.capHeight, this.form.detailHeight,
                    this.form.glossy, this.form.glowing));
        }

        /** Flies this far above the ground, a thin body over its shadow. */
        private Row hovering(float hover) {
            return this.withForm(new Form(Form.FLYER_HEIGHT, hover, this.form.capShare, this.form.capHeight, this.form.detailHeight,
                    this.form.glossy, this.form.glowing));
        }

        /** Carries a copy of its body this much smaller, risen this far; its detail sits on the copy. */
        private Row capped(float share, float capHeight) {
            return this.withForm(new Form(this.form.height, this.form.hover, share, capHeight, this.form.detailHeight, this.form.glossy,
                    this.form.glowing));
        }

        /** Its detail stands this far off the body, a part of its own. */
        private Row raised(float detailHeight) {
            return this.withForm(new Form(this.form.height, this.form.hover, this.form.capShare, this.form.capHeight, detailHeight,
                    this.form.glossy, this.form.glowing));
        }

        private Row glossy() {
            return this.withForm(new Form(this.form.height, this.form.hover, this.form.capShare, this.form.capHeight,
                    this.form.detailHeight, true, this.form.glowing));
        }

        private Row glowing() {
            return this.withForm(new Form(this.form.height, this.form.hover, this.form.capShare, this.form.capHeight,
                    this.form.detailHeight, this.form.glossy, true));
        }
    }

    static Row of(EnemyLook look) {
        return ROWS.get(look);
    }

    static Color barColor(EnemyBarLook look) {
        return switch (look) {
            case SHIELD -> SHIELD_BAR;
            case HEALTH -> HEALTH_BAR;
        };
    }

    static Color dotColor(EnemyDotLook look) {
        return switch (look) {
            case DEBUFF -> DEBUFF_DOT;
            case SPOTTED -> SPOTTED_DOT;
            case BUFF -> BUFF_DOT;
        };
    }

    private static float reachOf(ScaledShape shape) {
        double reach = 0;
        double[] point = new double[6];
        for (PathIterator path = shape.at(1f).getPathIterator(null, REACH_FLATNESS); !path.isDone(); path.next()) {
            if (path.currentSegment(point) != PathIterator.SEG_CLOSE) {
                reach = Math.max(reach, Math.hypot(point[0], point[1]));
            }
        }
        return (float) reach;
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
        rows.put(EnemyLook.CIRCLE, Row.of(new Color(72, 200, 212), EnemyLooks::circle)
                .withDetail(new Color(190, 240, 240), s -> circle(s * 0.42f)).capped(0.66f, 0.22f).glossy());
        rows.put(EnemyLook.GHOST, Row.of(Color.LIGHT_GRAY, EnemyShapes::ghostShape)
                .withDetail(new Color(70, 70, 95), s -> EnemyShapes.eyesShape(s, -s * 0.2f, s * 0.16f)).hovering(0.45f));
        rows.put(EnemyLook.SQUARE, Row.of(new Color(232, 165, 170), EnemyLooks::square)
                .withDetail(new Color(165, 95, 105), s -> Java2DFrameRenderer.diamondShape(s * 0.62f))
                .withFade(EnemyLooks::crossingSquares).capped(0.72f, 0.2f));
        rows.put(EnemyLook.TRIANGLE, Row.of(new Color(226, 212, 80), EnemyLooks::triangle)
                .withDetail(new Color(135, 120, 30), s -> Java2DFrameRenderer.triangleShape(s * 0.42f, true))
                .withFade(EnemyLooks::crossingTriangles).capped(0.62f, 0.18f));
        rows.put(EnemyLook.WARDEN, Row.of(new Color(173, 60, 52), s -> star(8, s, s * 0.55f))
                .withDetail(new Color(240, 190, 60), s -> circle(s * 0.28f)).standing(0.3f).raised(0.2f).glowing());
        rows.put(EnemyLook.WARDEN_EGG, Row.of(new Color(173, 60, 52), EnemyShapes::wardenEggShape).standing(0.3f).glossy());
        rows.put(EnemyLook.MENDER, Row.of(new Color(120, 220, 150), EnemyShapes::crossShape).capped(0.55f, 0.16f));
        rows.put(EnemyLook.JUGGERNAUT, Row.of(new Color(120, 135, 160), EnemyLooks::square)
                .withDetail(new Color(85, 95, 115), s -> new Rectangle2D.Float(-s * 0.55f, -s * 0.55f, s * 1.1f, s * 1.1f))
                .withFade(EnemyLooks::crossingSquares).standing(0.4f).capped(0.62f, 0.3f));
        rows.put(EnemyLook.MITE, Row.of(new Color(190, 140, 80), EnemyLooks::circle)
                .withDetail(new Color(95, 62, 30), s -> new Rectangle2D.Float(-s * 0.2f, -s * 0.95f, s * 0.4f, s * 1.9f))
                .standing(0.2f).glossy());
        rows.put(EnemyLook.COURIER, Row.of(new Color(189, 247, 139), EnemyLooks::triangle).withFade(EnemyLooks::crossingTriangles)
                .standing(0.14f));
        rows.put(EnemyLook.SKIRMISHER, Row.of(new Color(80, 220, 200), Java2DFrameRenderer::diamondShape).capped(0.5f, 0.18f));
        rows.put(EnemyLook.SALAMANDER, Row.of(new Color(239, 132, 73), EnemyShapes::lizardShape)
                .withDetail(new Color(255, 215, 60), s -> new Ellipse2D.Float(-s * 0.6f, -s * 0.1f, s * 1.2f, s * 0.2f))
                .standing(0.14f).raised(0.08f));
        rows.put(EnemyLook.YETI, Row.of(new Color(200, 235, 255), s -> star(16, s, s * 0.84f))
                .withDetail(new Color(50, 80, 120), s -> EnemyShapes.eyesShape(s, -s * 0.15f, s * 0.13f))
                .standing(0.32f).capped(0.74f, 0.22f));
        rows.put(EnemyLook.MAGMA, Row.of(new Color(196, 81, 68), EnemyShapes::magmaShape)
                .withDetail(new Color(255, 170, 60), s -> circle(s * 0.4f)).standing(0.3f).raised(0.1f).glowing());
        rows.put(EnemyLook.GOLEM, Row.of(new Color(150, 140, 125), s -> star(6, s, s * 0.87f))
                .withDetail(new Color(85, 76, 66), EnemyShapes::crackShape).standing(0.38f).capped(0.7f, 0.24f));
        rows.put(EnemyLook.RELAY, Row.of(new Color(40, 185, 170), EnemyShapes::relayShape)
                .withDetail(new Color(215, 250, 245), s -> circle(s * 0.3f)).hovering(0.3f).glowing());
        rows.put(EnemyLook.TEMPLAR, Row.of(new Color(236, 230, 214), EnemyShapes::heaterShieldShape)
                .withDetail(new Color(125, 85, 205), s -> Java2DFrameRenderer.diamondShape(s * 0.42f))
                .standing(0.2f).raised(0.12f));
        rows.put(EnemyLook.REAVER, Row.of(new Color(205, 150, 70), EnemyShapes::barbShape).standing(0.2f));
        rows.put(EnemyLook.COLOSSUS, Row.of(new Color(95, 105, 128), s -> star(8, s, s * 0.93f))
                .withDetail(new Color(255, 140, 40), s -> new RoundRectangle2D.Float(-s * 0.6f, -s * 0.16f, s * 1.2f, s * 0.32f,
                        s * 0.2f, s * 0.2f)).standing(0.45f).capped(0.76f, 0.3f).raised(0.06f));
        rows.put(EnemyLook.MOLTEN_CORE, Row.of(new Color(235, 126, 74), s -> star(10, s, s * 0.7f))
                .withDetail(new Color(255, 235, 120), s -> circle(s * 0.4f)).standing(0.3f).raised(0.12f).glowing());
        rows.put(EnemyLook.EMBER, Row.of(new Color(251, 177, 63), EnemyShapes::emberShape).hovering(0.35f).glowing());
        rows.put(EnemyLook.MONOLITH, Row.of(new Color(118, 115, 152), EnemyShapes::slabShape)
                .withDetail(new Color(175, 120, 255), Java2DFrameRenderer::runeShape).standing(0.7f).glowing());
        rows.put(EnemyLook.ECLIPSE, Row.of(new Color(120, 82, 180), EnemyShapes::crescentShape).hovering(0.3f));
        rows.put(EnemyLook.MIRAGE, Row.of(new Color(185, 155, 255), EnemyShapes::twinDiamondShape).hovering(0.25f));
        rows.put(EnemyLook.MIRROR_IMAGE, Row.of(new Color(220, 210, 255), Java2DFrameRenderer::diamondShape).hovering(0.25f));
        rows.put(EnemyLook.HERALD, Row.of(new Color(252, 206, 78), EnemyShapes::sweptArrowShape).standing(0.16f));
        rows.put(EnemyLook.TALOS, Row.of(new Color(205, 135, 60), EnemyShapes::giantShape)
                .withDetail(new Color(200, 25, 30), s -> new Rectangle2D.Float(-s * 0.07f, -s * 0.35f, s * 0.14f, s * 1.2f))
                .standing(0.5f));
        rows.put(EnemyLook.MINOTAUR, Row.of(new Color(160, 92, 55), EnemyShapes::bullHeadShape)
                .withDetail(new Color(235, 225, 200), EnemyShapes::hornsShape).standing(0.34f).raised(0.14f));
        rows.put(EnemyLook.BRONZE_BULL, Row.of(new Color(190, 120, 50), EnemyShapes::bullHeadShape)
                .withDetail(new Color(110, 65, 25), EnemyShapes::hornsShape).standing(0.34f).raised(0.14f));
        rows.put(EnemyLook.GLASS_SENTINEL, Row.of(new Color(170, 228, 240), Java2DFrameRenderer::crystalShape)
                .withDetail(new Color(245, 255, 255), s -> Java2DFrameRenderer.shardShape(s * 0.5f, 0))
                .standing(0.4f).raised(0.1f).glossy());
        rows.put(EnemyLook.SANDLING, Row.of(new Color(225, 190, 120), EnemyShapes::sandGrainShape).standing(0.16f));
        rows.put(EnemyLook.TIMEKEEPER, Row.of(new Color(205, 170, 95), EnemyLooks::circle)
                .withDetail(new Color(55, 40, 20), EnemyShapes::clockHandsShape).standing(0.3f).capped(0.8f, 0.1f));
        rows.put(EnemyLook.KEEPER, Row.of(new Color(165, 210, 225), EnemyShapes::hourglassShape)
                .withDetail(new Color(235, 190, 100), EnemyShapes::hourglassSandShape).standing(0.5f));
        rows.put(EnemyLook.GALE, Row.of(new Color(180, 232, 222), EnemyShapes::galeShape)
                .withDetail(new Color(90, 160, 150), s -> circle(s * 0.22f)).hovering(0.4f));
        rows.put(EnemyLook.RAINDROP, Row.of(new Color(110, 170, 255), EnemyShapes::raindropShape).hovering(0.4f).glossy());
        rows.put(EnemyLook.RAINCLOUD, Row.of(new Color(150, 165, 192), EnemyShapes::cloudShape)
                .withDetail(new Color(95, 150, 240), EnemyShapes::rainStreaksShape).hovering(0.6f));
        rows.put(EnemyLook.LIGHTNING, Row.of(new Color(255, 235, 120), EnemyLooks::sidewaysBolt)
                .withDetail(Color.WHITE, s -> sidewaysBolt(s * 0.45f)).hovering(0.4f));
        rows.put(EnemyLook.THUNDER, Row.of(new Color(108, 120, 172), EnemyShapes::thunderheadShape)
                .withDetail(new Color(255, 225, 80), s -> AffineTransform.getTranslateInstance(0, s * 0.55f)
                        .createTransformedShape(EnemyShapes.boltShape(s * 0.6f))).hovering(0.6f));
        rows.put(EnemyLook.HORNET, Row.of(new Color(226, 122, 58), EnemyShapes::hornetShape)
                .withDetail(new Color(35, 22, 10), EnemyShapes::hornetStripesShape).hovering(0.45f));
        rows.put(EnemyLook.COCOON, Row.of(new Color(220, 210, 185), s -> new Ellipse2D.Float(-s * 0.7f, -s * 1.15f, s * 1.4f, s * 2.3f))
                .withDetail(new Color(150, 130, 95), EnemyShapes::cocoonBandsShape).standing(0.3f).glossy());
        rows.put(EnemyLook.LARVA, Row.of(new Color(240, 232, 205), EnemyShapes::grubShape)
                .withDetail(new Color(130, 85, 40), s -> new Ellipse2D.Float(s * 0.62f, -s * 0.18f, s * 0.36f, s * 0.36f))
                .standing(0.2f).glossy());
        rows.put(EnemyLook.NURSE, Row.of(new Color(240, 220, 150), s -> star(6, s, s * 0.87f))
                .withDetail(new Color(205, 120, 30), s -> EnemyShapes.crossShape(s * 0.45f)).capped(0.6f, 0.16f));
        rows.put(EnemyLook.HIVE_GUARD, Row.of(new Color(170, 105, 35), EnemyShapes::beetleShape)
                .withDetail(new Color(70, 40, 12), s -> new Rectangle2D.Float(-s * 0.85f, -s * 0.05f, s * 1.4f, s * 0.1f))
                .standing(0.34f).glossy());
        rows.put(EnemyLook.DRONE, Row.of(new Color(242, 191, 62), EnemyShapes::beeShape)
                .withDetail(new Color(70, 40, 10), EnemyShapes::beeStripesShape).hovering(0.45f));
        return rows;
    }
}
