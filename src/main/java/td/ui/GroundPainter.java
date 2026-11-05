package td.ui;

import td.board.BoardGround;
import td.board.GroundMotif;
import td.util.ThreadConfined;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Paints a board's floor and its motif, under everything else on the board. A motif stays background by being made
 * of short or broken marks: paths are dotted lines, so a long unbroken line would read louder than a path.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class GroundPainter {

    private static final Stroke MOTIF_STROKE = new BasicStroke(1f);

    private Optional<Painted> painted = Optional.empty();

    /** Everything the image of a ground depends on. */
    record Layout(BoardGround ground, int scale, int width, int height, double zoomX, double zoomY) {

        /** A board of {@code width} by {@code height} pixels, unzoomed. */
        static Layout of(BoardGround ground, int scale, int width, int height) {
            return new Layout(ground, scale, width, height, 1, 1);
        }

        Layout withZoom(double x, double y) {
            return new Layout(this.ground, this.scale, this.width, this.height, x, y);
        }

        int imageWidth() {
            return Math.max(1, (int) Math.ceil(this.width * this.zoomX));
        }

        int imageHeight() {
            return Math.max(1, (int) Math.ceil(this.height * this.zoomY));
        }
    }

    private record Painted(Layout layout, BufferedImage image) {
    }

    /**
     * Fills the whole board, so it is also what clears the previous frame. A motif is too many marks to draw again for
     * every frame, so they are drawn once into an image the size of the board on screen, which lands on whole pixels.
     */
    void paint(Graphics2D g2, BoardGround ground, int scale, int width, int height) {
        AffineTransform view = g2.getTransform();
        Layout layout = Layout.of(ground, scale, width, height).withZoom(view.getScaleX(), view.getScaleY());
        BufferedImage image = this.imageFor(layout, g2.getDeviceConfiguration());
        g2.setTransform(AffineTransform.getTranslateInstance(Math.round(view.getTranslateX()),
                Math.round(view.getTranslateY())));
        g2.drawImage(image, 0, 0, null);
        g2.setTransform(view);
    }

    /** The image of {@code layout}: the one it already has, unless the layout changed. */
    BufferedImage imageFor(Layout layout, GraphicsConfiguration configuration) {
        if (this.painted.isEmpty() || !this.painted.get().layout().equals(layout)) {
            this.painted = Optional.of(new Painted(layout, render(layout, configuration)));
        }
        return this.painted.get().image();
    }

    private static BufferedImage render(Layout layout, GraphicsConfiguration configuration) {
        BufferedImage image = configuration.createCompatibleImage(layout.imageWidth(), layout.imageHeight());
        Graphics2D g2 = image.createGraphics();
        try {
            g2.scale(layout.zoomX(), layout.zoomY());
            paintGround(g2, layout.ground(), layout.scale(), layout.width(), layout.height());
        } finally {
            g2.dispose();
        }
        return image;
    }

    static void paintGround(Graphics2D g2, BoardGround ground, int scale, int width, int height) {
        g2.setColor(new Color(ground.floorRgb()));
        g2.fillRect(0, 0, width, height);
        if (ground.motif() == GroundMotif.NONE || scale <= 0) {
            return;
        }
        Marks marks = switch (ground.motif()) {
            case NONE -> Marks.lines(new GeneralPath());
            case COMB -> Marks.lines(comb(scale, width, height));
            case RAIN -> Marks.lines(rain(scale, width, height));
            case DUNES -> Marks.lines(dunes(scale, width, height));
            case PLATES -> plates(scale, width, height);
            case FISSURES -> Marks.lines(fissures(scale, width, height));
            case FLAGSTONES -> Marks.lines(flagstones(scale, width, height));
        };
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(MOTIF_STROKE);
        g2.setColor(halfway(ground.floorRgb(), ground.motifRgb()));
        g2.draw(marks.faintLines());
        g2.setColor(new Color(ground.motifRgb()));
        g2.draw(marks.lines());
        g2.fill(marks.dots());
    }

    private static Color halfway(int fromRgb, int toRgb) {
        int r = (((fromRgb >> 16) & 0xFF) + ((toRgb >> 16) & 0xFF)) / 2;
        int g = (((fromRgb >> 8) & 0xFF) + ((toRgb >> 8) & 0xFF)) / 2;
        int b = ((fromRgb & 0xFF) + (toRgb & 0xFF)) / 2;
        return new Color(r, g, b);
    }

    /** A motif's marks: lines in the motif colour, lines halfway to the floor, and dots filled solid. */
    private record Marks(Shape lines, Shape faintLines, Shape dots) {

        static Marks lines(Shape lines) {
            return new Marks(lines, new GeneralPath(), new GeneralPath());
        }
    }

    /** Flat-topped hexagons, each half a cell across. */
    private static GeneralPath comb(int scale, int width, int height) {
        GeneralPath p = new GeneralPath();
        float r = scale * 0.5f;
        float stepX = r * 1.5f;
        float stepY = (float) (Math.sqrt(3) * r);
        int column = 0;
        for (float cx = 0; cx < width + r; cx += stepX, column++) {
            float offset = (column % 2 == 0) ? 0f : stepY / 2f;
            for (float cy = -stepY; cy < height + stepY; cy += stepY) {
                hexagon(p, cx, cy + offset, r);
            }
        }
        return p;
    }

    private static void hexagon(GeneralPath p, float cx, float cy, float r) {
        for (int i = 0; i <= 6; i++) {
            double angle = Math.PI / 3 * i;
            float x = cx + (float) (Math.cos(angle) * r);
            float y = cy + (float) (Math.sin(angle) * r);
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
    }

    /** Short slanting streaks, scattered the same way every frame so the rain holds still. */
    private static GeneralPath rain(int scale, int width, int height) {
        GeneralPath p = new GeneralPath();
        float step = scale * 0.5f;
        float length = scale * 0.35f;
        int row = 0;
        for (float y = 0; y < height; y += step, row++) {
            int column = 0;
            for (float x = 0; x < width; x += step, column++) {
                int hash = (row * 73_856_093) ^ (column * 19_349_663);
                if ((hash & 3) != 0) {
                    continue;
                }
                float jx = ((hash >> 4) & 15) / 15f * step;
                float jy = ((hash >> 8) & 15) / 15f * step;
                p.moveTo(x + jx, y + jy);
                p.lineTo(x + jx - length * 0.35f, y + jy + length);
            }
        }
        return p;
    }

    /** Short arched ripples in loose rows, like sand ripple marks, each row's gaps off the last's. */
    private static GeneralPath dunes(int scale, int width, int height) {
        GeneralPath p = new GeneralPath();
        float rowStep = scale * 0.6f;
        float rise = scale * 0.1f;
        int pieces = 6;
        int row = 0;
        for (float y = rowStep / 2f; y < height + rowStep; y += rowStep, row++) {
            float x = -scale * (1f + (float) jitter(row, 0));
            int ripple = 0;
            while (x < width) {
                float length = scale * (0.9f + 0.35f * (float) jitter(row, ripple + 1));
                float dy = scale * 0.08f * (float) jitter(row, ripple + 100);
                p.moveTo(x, y + dy);
                for (int i = 1; i <= pieces; i++) {
                    float t = i / (float) pieces;
                    p.lineTo(x + length * t, y + dy - rise * (float) Math.sin(Math.PI * t));
                }
                x += length + scale * (0.45f + 0.3f * (float) jitter(row, ripple + 200));
                ripple++;
            }
        }
        return p;
    }

    /**
     * Staggered plates, each row shifted half a plate so no seam lines up with the cell grid for long, the seams
     * faint and a solid rivet in from each plate corner.
     */
    private static Marks plates(int scale, int width, int height) {
        GeneralPath seams = new GeneralPath();
        GeneralPath rivets = new GeneralPath();
        float plateWidth = scale * 3f;
        float plateHeight = scale * 2f;
        float inset = scale * 0.25f;
        float rivet = scale * 0.06f;
        int row = 0;
        for (float y = 0; y < height; y += plateHeight, row++) {
            if (y > 0) {
                seams.moveTo(0, y);
                seams.lineTo(width, y);
            }
            for (float x = -plateWidth / 2f * (row % 2); x < width; x += plateWidth) {
                if (x > 0) {
                    seams.moveTo(x, y);
                    seams.lineTo(x, y + plateHeight);
                }
                for (float rx : new float[] {x + inset, x + plateWidth - inset}) {
                    for (float ry : new float[] {y + inset, y + plateHeight - inset}) {
                        rivets.append(new Ellipse2D.Float(rx - rivet, ry - rivet, rivet * 2, rivet * 2), false);
                    }
                }
            }
        }
        return new Marks(new GeneralPath(), seams, rivets);
    }

    /**
     * Jagged cracks from the middle out, some forking once, the same every frame. Each breaks up more the further it
     * runs and dies out before the edge, so no crack reads as a route.
     */
    private static GeneralPath fissures(int scale, int width, int height) {
        GeneralPath p = new GeneralPath();
        float cx = width / 2f;
        float cy = height / 2f;
        double reach = Math.hypot(width, height) / 2 * 0.7;
        int cracks = 11;
        for (int i = 0; i < cracks; i++) {
            double angle = i * 2 * Math.PI / cracks + jitter(i, 1) * 0.35;
            double length = reach * (0.6 + 0.4 * Math.abs(jitter(i, 4)));
            crack(p, cx, cy, angle, scale * 0.6, length, scale, i);
            if (i % 3 == 0) {
                double fork = length * (0.35 + 0.2 * jitter(i, 2));
                float fx = cx + (float) (Math.cos(angle) * fork);
                float fy = cy + (float) (Math.sin(angle) * fork);
                crack(p, fx, fy, angle + 0.6 * Math.signum(jitter(i, 3) + 0.01), 0, length * 0.4, scale, i + 50);
            }
        }
        return p;
    }

    private static void crack(GeneralPath p, float x, float y, double angle, double from, double length, int scale,
                              int seed) {
        float step = scale * 0.4f;
        float px = x + (float) (Math.cos(angle) * from);
        float py = y + (float) (Math.sin(angle) * from);
        p.moveTo(px, py);
        int n = 0;
        for (double d = from + step; d < from + length; d += step, n++) {
            double wobble = angle + jitter(seed, n + 10) * 0.5;
            px += (float) (Math.cos(wobble) * step);
            py += (float) (Math.sin(wobble) * step);
            double travelled = (d - from) / length;
            if (jitter(seed, n + 30) > 0.8 - 1.6 * travelled) {
                p.moveTo(px, py);
            } else {
                p.lineTo(px, py);
            }
        }
    }

    /** A repeatable number in {@code [-1, 1]} for {@code a} and {@code b}. */
    private static double jitter(int a, int b) {
        int hash = (a * 73_856_093) ^ (b * 19_349_663);
        hash ^= hash >>> 13;
        hash *= 0x5bd1e995;
        hash ^= hash >>> 15;
        return (hash & 0xFFFF) / 32_767.5 - 1.0;
    }

    /** Rows a cell high of stones one to two cells long, each row's joints off the last's. */
    private static GeneralPath flagstones(int scale, int width, int height) {
        GeneralPath p = new GeneralPath();
        float inset = scale * 0.08f;
        int row = 0;
        for (float y = 0; y < height; y += scale, row++) {
            float x = -scale * 0.5f * (row % 2);
            int stone = 0;
            while (x < width) {
                float length = scale * (jitter(row, stone) > 0 ? 2f : 1.5f);
                p.append(new RoundRectangle2D.Float(x + inset, y + inset, length - 2 * inset,
                        scale - 2 * inset, scale * 0.25f, scale * 0.25f), false);
                x += length;
                stone++;
            }
        }
        return p;
    }
}
