package td.ui;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/** The vector art only enemy bodies use, each centred on the origin and sized by its scale. */
final class EnemyShapes {

    private EnemyShapes() {
    }

    /** Two small eyes side by side at height {@code y}. */
    /** A jagged split across stone, thin enough to stay a crack at a small size. */
    static Shape crackShape(float scale) {
        Path2D.Float crack = new Path2D.Float();
        crack.moveTo(-scale * 0.2f, -scale * 0.72f);
        crack.lineTo(scale * 0.2f, -scale * 0.22f);
        crack.lineTo(-scale * 0.08f, scale * 0.08f);
        crack.lineTo(scale * 0.24f, scale * 0.68f);
        crack.lineTo(-scale * 0.08f, scale * 0.28f);
        crack.lineTo(-scale * 0.26f, -scale * 0.02f);
        crack.lineTo(scale * 0.02f, -scale * 0.26f);
        crack.closePath();
        return crack;
    }

    static Shape eyesShape(float scale, float y, float radius) {
        Area eyes = new Area(new Ellipse2D.Float(-scale * 0.35f - radius, y - radius * 1.3f, radius * 2, radius * 2.6f));
        eyes.add(new Area(new Ellipse2D.Float(scale * 0.35f - radius, y - radius * 1.3f, radius * 2, radius * 2.6f)));
        return eyes;
    }

    /** A sheet with a domed head and a scalloped hem. */
    static Shape ghostShape(float scale) {
        GeneralPath p = new GeneralPath();
        p.moveTo(-scale, 0);
        p.curveTo(-scale, -scale * 1.35f, scale, -scale * 1.35f, scale, 0);
        p.lineTo(scale, scale * 0.85f);
        float step = scale * 2f / 3f;
        for (int i = 0; i < 3; i++) {
            float right = scale - step * i;
            p.quadTo(right - step / 2f, scale * 0.35f, right - step, scale * 0.85f);
        }
        p.closePath();
        return p;
    }

    /** A lizard seen from above, nose forward: body, head, tail and four legs. */
    static Shape lizardShape(float scale) {
        Area lizard = new Area(new Ellipse2D.Float(-scale * 0.75f, -scale * 0.38f, scale * 1.5f, scale * 0.76f));
        lizard.add(new Area(new Ellipse2D.Float(scale * 0.45f, -scale * 0.3f, scale * 0.7f, scale * 0.6f)));
        GeneralPath tail = new GeneralPath();
        tail.moveTo(-scale * 0.6f, -scale * 0.22f);
        tail.lineTo(-scale * 1.45f, 0);
        tail.lineTo(-scale * 0.6f, scale * 0.22f);
        tail.closePath();
        lizard.add(new Area(tail));
        for (float legX : new float[] {-scale * 0.4f, scale * 0.3f}) {
            for (float legY : new float[] {-scale * 0.62f, scale * 0.38f}) {
                lizard.add(new Area(new Ellipse2D.Float(legX, legY, scale * 0.24f, scale * 0.24f)));
            }
        }
        return lizard;
    }

    /** A disc with four antennae, broadcasting. */
    static Shape jammerShape(float scale) {
        Area jammer = new Area(Java2DFrameRenderer.circleShape(scale * 0.68f));
        jammer.add(new Area(Java2DFrameRenderer.starShape(4, scale * 1.15f, scale * 0.22f)));
        return jammer;
    }

    /** A barbed arrowhead, point forward. */
    static Shape barbShape(float scale) {
        GeneralPath p = new GeneralPath();
        p.moveTo(scale * 1.1f, 0);
        p.lineTo(scale * 0.1f, -scale * 0.5f);
        p.lineTo(-scale * 0.2f, -scale * 0.95f);
        p.lineTo(-scale * 0.5f, -scale * 0.3f);
        p.lineTo(-scale * 0.9f, 0);
        p.lineTo(-scale * 0.5f, scale * 0.3f);
        p.lineTo(-scale * 0.2f, scale * 0.95f);
        p.lineTo(scale * 0.1f, scale * 0.5f);
        p.closePath();
        return p;
    }

    /** A swept-back arrow, point forward: a runner. */
    static Shape sweptArrowShape(float scale) {
        GeneralPath p = new GeneralPath();
        p.moveTo(scale, 0);
        p.lineTo(-scale * 0.75f, -scale * 0.8f);
        p.lineTo(-scale * 0.3f, 0);
        p.lineTo(-scale * 0.75f, scale * 0.8f);
        p.closePath();
        return p;
    }

    /** A flame: round at the base, licking up to a point. */
    static Shape emberShape(float scale) {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, -scale * 1.1f);
        p.curveTo(scale * 0.75f, -scale * 0.2f, scale * 0.7f, scale * 0.85f, 0, scale * 0.85f);
        p.curveTo(-scale * 0.7f, scale * 0.85f, -scale * 0.75f, -scale * 0.2f, 0, -scale * 1.1f);
        p.closePath();
        return p;
    }

    /** A standing stone: a tall slab with a pointed top. */
    static Shape slabShape(float scale) {
        GeneralPath p = new GeneralPath();
        p.moveTo(-scale * 0.5f, scale);
        p.lineTo(scale * 0.5f, scale);
        p.lineTo(scale * 0.5f, -scale * 0.72f);
        p.lineTo(0, -scale * 1.08f);
        p.lineTo(-scale * 0.5f, -scale * 0.72f);
        p.closePath();
        return p;
    }

    /** A crescent: a disc with a bite taken out by its own shadow. */
    static Shape crescentShape(float scale) {
        Area crescent = new Area(Java2DFrameRenderer.circleShape(scale));
        crescent.subtract(new Area(new Ellipse2D.Float(-scale * 0.55f, -scale * 1.2f, scale * 2f, scale * 2f)));
        return crescent;
    }

    /** Two diamonds overlapping: one body seen twice. */
    static Shape twinDiamondShape(float scale) {
        Shape diamond = Java2DFrameRenderer.diamondShape(scale * 0.8f);
        Area twin = new Area(AffineTransform.getTranslateInstance(-scale * 0.3f, 0).createTransformedShape(diamond));
        twin.add(new Area(AffineTransform.getTranslateInstance(scale * 0.3f, 0).createTransformedShape(diamond)));
        return twin;
    }

    /** A bronze giant, upright: a broad torso and arms under a small head. */
    static Shape giantShape(float scale) {
        Area giant = new Area(new RoundRectangle2D.Float(-scale * 0.6f, -scale * 0.55f, scale * 1.2f, scale * 1.55f,
                scale * 0.4f, scale * 0.4f));
        giant.add(new Area(new Ellipse2D.Float(-scale * 0.3f, -scale * 1.1f, scale * 0.6f, scale * 0.6f)));
        giant.add(new Area(new RoundRectangle2D.Float(-scale * 0.95f, -scale * 0.5f, scale * 0.3f, scale * 1.1f,
                scale * 0.2f, scale * 0.2f)));
        giant.add(new Area(new RoundRectangle2D.Float(scale * 0.65f, -scale * 0.5f, scale * 0.3f, scale * 1.1f,
                scale * 0.2f, scale * 0.2f)));
        return giant;
    }

    /** A bull's head seen from above, muzzle forward. */
    static Shape bullHeadShape(float scale) {
        Area head = new Area(new Ellipse2D.Float(-scale * 0.8f, -scale * 0.62f, scale * 1.45f, scale * 1.24f));
        head.add(new Area(new Ellipse2D.Float(scale * 0.35f, -scale * 0.4f, scale * 0.7f, scale * 0.8f)));
        return head;
    }

    /** A pair of horns curving forward from the sides of a bull's head. */
    static Shape hornsShape(float scale) {
        GeneralPath horn = new GeneralPath();
        horn.moveTo(-scale * 0.25f, -scale * 0.45f);
        horn.quadTo(scale * 0.05f, -scale * 1.2f, scale * 0.85f, -scale * 1.0f);
        horn.quadTo(scale * 0.25f, -scale * 0.85f, scale * 0.2f, -scale * 0.42f);
        horn.closePath();
        Area horns = new Area(horn);
        horns.add(new Area(AffineTransform.getScaleInstance(1, -1).createTransformedShape(horn)));
        return horns;
    }

    /** A cross: two rectangles unioned. */
    static Shape crossShape(float scale) {
        float arm = scale * 0.55f;
        Area cross = new Area(new Rectangle2D.Float(-scale, -arm, scale * 2, arm * 2));
        cross.add(new Area(new Rectangle2D.Float(-arm, -scale, arm * 2, scale * 2)));
        return cross;
    }

    /**
     * A tall, spiked ovoid in the same star language as the boss body, so it reads as that boss's
     * egg.
     */
    static Shape wardenEggShape(float scale) {
        Shape star = Java2DFrameRenderer.starShape(6, scale, scale * 0.65f);
        AffineTransform elongate = AffineTransform.getScaleInstance(0.8, 1.3);
        return elongate.createTransformedShape(star);
    }

    /** A blunt many-pointed star, so it reads as a boulder rather than as spikes. */
    static Shape magmaShape(float scale) {
        return Java2DFrameRenderer.starShape(12, scale, scale * 0.78f);
    }

    /** A bee from above, head forward: a round body, a head and two wings. */
    static Shape beeShape(float scale) {
        Area bee = new Area(new Ellipse2D.Float(-scale * 0.8f, -scale * 0.5f, scale * 1.5f, scale * 1.0f));
        bee.add(new Area(Java2DFrameRenderer.circleShape(scale * 0.3f)).createTransformedArea(AffineTransform.getTranslateInstance(scale * 0.75f, 0)));
        bee.add(new Area(new Ellipse2D.Float(-scale * 0.45f, -scale * 1.05f, scale * 0.7f, scale * 0.6f)));
        bee.add(new Area(new Ellipse2D.Float(-scale * 0.45f, scale * 0.45f, scale * 0.7f, scale * 0.6f)));
        return bee;
    }

    /** Dark bands across a bee's body. */
    static Shape beeStripesShape(float scale) {
        Area stripes = new Area(new Rectangle2D.Float(-scale * 0.45f, -scale * 0.45f, scale * 0.2f, scale * 0.9f));
        stripes.add(new Area(new Rectangle2D.Float(-scale * 0.05f, -scale * 0.48f, scale * 0.2f, scale * 0.96f)));
        stripes.intersect(new Area(new Ellipse2D.Float(-scale * 0.8f, -scale * 0.5f, scale * 1.5f, scale * 1.0f)));
        return stripes;
    }

    /** A beetle from above, head forward: a domed shell, a head and two mandibles. */
    static Shape beetleShape(float scale) {
        Area beetle = new Area(new Ellipse2D.Float(-scale * 0.95f, -scale * 0.75f, scale * 1.65f, scale * 1.5f));
        beetle.add(new Area(new Ellipse2D.Float(scale * 0.5f, -scale * 0.42f, scale * 0.55f, scale * 0.84f)));
        GeneralPath mandible = new GeneralPath();
        mandible.moveTo(scale * 0.9f, -scale * 0.3f);
        mandible.quadTo(scale * 1.35f, -scale * 0.3f, scale * 1.3f, -scale * 0.02f);
        mandible.lineTo(scale * 0.95f, -scale * 0.1f);
        mandible.closePath();
        beetle.add(new Area(mandible));
        beetle.add(new Area(AffineTransform.getScaleInstance(1, -1).createTransformedShape(mandible)));
        return beetle;
    }

    /** A grub, head forward: three swollen segments in a row. */
    static Shape grubShape(float scale) {
        Area grub = new Area(new Ellipse2D.Float(-scale * 1.05f, -scale * 0.42f, scale * 0.84f, scale * 0.84f));
        grub.add(new Area(new Ellipse2D.Float(-scale * 0.52f, -scale * 0.55f, scale * 1.1f, scale * 1.1f)));
        grub.add(new Area(new Ellipse2D.Float(scale * 0.2f, -scale * 0.48f, scale * 0.95f, scale * 0.96f)));
        return grub;
    }

    /** Slanting bands of silk wound round a cocoon. */
    static Shape cocoonBandsShape(float scale) {
        Area bands = new Area();
        for (float y : new float[] {-scale * 0.6f, 0f, scale * 0.6f}) {
            Shape band = new Rectangle2D.Float(-scale, y - scale * 0.07f, scale * 2f, scale * 0.14f);
            bands.add(new Area(AffineTransform.getRotateInstance(-0.35, 0, y).createTransformedShape(band)));
        }
        bands.intersect(new Area(new Ellipse2D.Float(-scale * 0.7f, -scale * 1.15f, scale * 1.4f, scale * 2.3f)));
        return bands;
    }

    /** A hornet from above, head forward: a head, a thorax, a pointed abdomen and swept-back wings. */
    static Shape hornetShape(float scale) {
        Area hornet = new Area(hornetAbdomen(scale));
        hornet.add(new Area(new Ellipse2D.Float(scale * 0.05f, -scale * 0.32f, scale * 0.6f, scale * 0.64f)));
        hornet.add(new Area(new Ellipse2D.Float(scale * 0.6f, -scale * 0.22f, scale * 0.44f, scale * 0.44f)));
        GeneralPath wing = new GeneralPath();
        wing.moveTo(scale * 0.4f, -scale * 0.2f);
        wing.quadTo(scale * 0.1f, -scale * 1.1f, -scale * 0.7f, -scale * 0.75f);
        wing.quadTo(-scale * 0.2f, -scale * 0.45f, scale * 0.2f, -scale * 0.1f);
        wing.closePath();
        hornet.add(new Area(wing));
        hornet.add(new Area(AffineTransform.getScaleInstance(1, -1).createTransformedShape(wing)));
        return hornet;
    }

    private static Shape hornetAbdomen(float scale) {
        GeneralPath abdomen = new GeneralPath();
        abdomen.moveTo(scale * 0.15f, -scale * 0.3f);
        abdomen.curveTo(-scale * 0.5f, -scale * 0.42f, -scale * 0.9f, -scale * 0.15f, -scale * 1.15f, 0);
        abdomen.curveTo(-scale * 0.9f, scale * 0.15f, -scale * 0.5f, scale * 0.42f, scale * 0.15f, scale * 0.3f);
        abdomen.closePath();
        return abdomen;
    }

    /** Black bands across a hornet's abdomen. */
    static Shape hornetStripesShape(float scale) {
        Area stripes = new Area(new Rectangle2D.Float(-scale * 0.75f, -scale * 0.5f, scale * 0.14f, scale));
        stripes.add(new Area(new Rectangle2D.Float(-scale * 0.4f, -scale * 0.5f, scale * 0.14f, scale)));
        stripes.intersect(new Area(hornetAbdomen(scale)));
        return stripes;
    }

    /** A storm cloud: three puffs over a flat base. */
    static Shape cloudShape(float scale) {
        Area cloud = new Area(new Ellipse2D.Float(-scale * 0.95f, -scale * 0.45f, scale * 1.0f, scale * 1.0f));
        cloud.add(new Area(new Ellipse2D.Float(-scale * 0.5f, -scale * 0.85f, scale * 1.2f, scale * 1.2f)));
        cloud.add(new Area(new Ellipse2D.Float(scale * 0.1f, -scale * 0.4f, scale * 0.9f, scale * 0.9f)));
        cloud.add(new Area(new RoundRectangle2D.Float(-scale * 0.95f, -scale * 0.05f, scale * 1.95f, scale * 0.6f,
                scale * 0.5f, scale * 0.5f)));
        return cloud;
    }

    /** A thunderhead: a storm cloud, wider than a raincloud and heavier. */
    static Shape thunderheadShape(float scale) {
        return AffineTransform.getScaleInstance(1.2, 1.0).createTransformedShape(cloudShape(scale));
    }

    /** A lightning bolt, striking downward. */
    static Shape boltShape(float scale) {
        GeneralPath bolt = new GeneralPath();
        bolt.moveTo(scale * 0.25f, -scale);
        bolt.lineTo(-scale * 0.5f, scale * 0.15f);
        bolt.lineTo(-scale * 0.05f, scale * 0.15f);
        bolt.lineTo(-scale * 0.3f, scale);
        bolt.lineTo(scale * 0.5f, -scale * 0.2f);
        bolt.lineTo(scale * 0.05f, -scale * 0.2f);
        bolt.lineTo(scale * 0.45f, -scale);
        bolt.closePath();
        return bolt;
    }

    /** Three slanting streaks of rain under a cloud. */
    static Shape rainStreaksShape(float scale) {
        Area streaks = new Area();
        for (float x : new float[] {-scale * 0.5f, 0f, scale * 0.5f}) {
            Shape streak = new RoundRectangle2D.Float(x - scale * 0.07f, scale * 0.15f, scale * 0.14f, scale * 0.38f,
                    scale * 0.1f, scale * 0.1f);
            streaks.add(new Area(AffineTransform.getRotateInstance(0.3, x, scale * 0.3f).createTransformedShape(streak)));
        }
        return streaks;
    }

    /** A drop of rain, point forward. */
    static Shape raindropShape(float scale) {
        GeneralPath drop = new GeneralPath();
        drop.moveTo(scale * 1.1f, 0);
        drop.curveTo(scale * 0.2f, -scale * 0.75f, -scale * 0.8f, -scale * 0.6f, -scale * 0.8f, 0);
        drop.curveTo(-scale * 0.8f, scale * 0.6f, scale * 0.2f, scale * 0.75f, scale * 1.1f, 0);
        drop.closePath();
        return drop;
    }

    /** A whirlwind: three curved blades about a still eye. */
    static Shape galeShape(float scale) {
        GeneralPath blade = new GeneralPath();
        blade.moveTo(scale * 0.1f, 0);
        blade.curveTo(scale * 0.65f, -scale * 0.1f, scale * 1.0f, -scale * 0.45f, scale * 0.9f, -scale * 1.0f);
        blade.curveTo(scale * 0.65f, -scale * 0.5f, scale * 0.25f, -scale * 0.35f, 0, -scale * 0.12f);
        blade.closePath();
        Area gale = new Area(Java2DFrameRenderer.circleShape(scale * 0.32f));
        for (int i = 0; i < 3; i++) {
            gale.add(new Area(AffineTransform.getRotateInstance(i * 2 * Math.PI / 3).createTransformedShape(blade)));
        }
        return gale;
    }

    /** An hourglass: two glass bulbs meeting at a narrow waist, between a frame's top and bottom bars. */
    static Shape hourglassShape(float scale) {
        GeneralPath glass = new GeneralPath();
        glass.moveTo(-scale * 0.75f, -scale * 1.0f);
        glass.lineTo(scale * 0.75f, -scale * 1.0f);
        glass.lineTo(scale * 0.12f, 0);
        glass.lineTo(scale * 0.75f, scale * 1.0f);
        glass.lineTo(-scale * 0.75f, scale * 1.0f);
        glass.lineTo(-scale * 0.12f, 0);
        glass.closePath();
        Area hourglass = new Area(glass);
        hourglass.add(new Area(new Rectangle2D.Float(-scale * 0.95f, -scale * 1.2f, scale * 1.9f, scale * 0.22f)));
        hourglass.add(new Area(new Rectangle2D.Float(-scale * 0.95f, scale * 0.98f, scale * 1.9f, scale * 0.22f)));
        return hourglass;
    }

    /** The sand: a heap in the lower bulb and the trickle that feeds it. */
    static Shape hourglassSandShape(float scale) {
        GeneralPath heap = new GeneralPath();
        heap.moveTo(-scale * 0.6f, scale * 0.95f);
        heap.lineTo(scale * 0.6f, scale * 0.95f);
        heap.lineTo(0, scale * 0.4f);
        heap.closePath();
        Area sand = new Area(heap);
        sand.add(new Area(new Rectangle2D.Float(-scale * 0.04f, -scale * 0.1f, scale * 0.08f, scale * 0.55f)));
        GeneralPath top = new GeneralPath();
        top.moveTo(-scale * 0.42f, -scale * 0.55f);
        top.lineTo(scale * 0.42f, -scale * 0.55f);
        top.lineTo(0, -scale * 0.08f);
        top.closePath();
        sand.add(new Area(top));
        return sand;
    }

    /** A clock's two hands and the pin they turn on. */
    static Shape clockHandsShape(float scale) {
        Area hands = new Area(new Rectangle2D.Float(-scale * 0.07f, -scale * 0.75f, scale * 0.14f, scale * 0.8f));
        hands.add(new Area(new Rectangle2D.Float(-scale * 0.05f, -scale * 0.05f, scale * 0.55f, scale * 0.1f)));
        hands.add(new Area(Java2DFrameRenderer.circleShape(scale * 0.14f)));
        hands.add(new Area(Java2DFrameRenderer.ringShape(scale * 0.92f, 0.86f)));
        return hands;
    }

    /** A grain of sand: a small, uneven stone. */
    static Shape sandGrainShape(float scale) {
        GeneralPath grain = new GeneralPath();
        grain.moveTo(scale * 1.05f, 0);
        grain.lineTo(scale * 0.4f, -scale * 0.85f);
        grain.lineTo(-scale * 0.55f, -scale * 0.75f);
        grain.lineTo(-scale * 1.0f, scale * 0.1f);
        grain.lineTo(-scale * 0.45f, scale * 0.85f);
        grain.lineTo(scale * 0.5f, scale * 0.7f);
        grain.closePath();
        return grain;
    }
}
