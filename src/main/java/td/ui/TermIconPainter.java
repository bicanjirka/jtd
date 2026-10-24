package td.ui;

import td.ui.render.IconFace;
import td.ui.render.Palette;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Optional;

/**
 * Paints a term's icon: its pictogram on a tinted tile inside its frame, with the live state an {@link IconFace}
 * carries. Alone paints a face; everything else describes one.
 */
public final class TermIconPainter {

    /** The size of a face on a sheet row. */
    public static final int ROW = 20;
    /** The size of a face on a button. */
    public static final int BUTTON = 16;
    /** The size of a face inside a line of text. */
    public static final int MENTION = 13;
    /** The height of the time label under a row face. */
    public static final int LABEL = 11;

    private static final float DESIGN = 20f;
    private static final Font COUNT_FONT = new Font(Font.DIALOG, Font.BOLD, 9);
    private static final Font NUMERAL_FONT = new Font(Font.DIALOG, Font.BOLD, 8);
    private static final Font LABEL_FONT = new Font(Font.DIALOG, Font.PLAIN, 9);
    private static final float FADED_ALPHA = 0.38f;

    private TermIconPainter() {
    }

    /** The colour a mention's title is written in: the tone lifted the way the symbol's ink is. */
    static Color ink(Palette tone) {
        return inkOf(Java2DFrameRenderer.colorFor(tone));
    }

    /**
     * Paints {@code face} with its top-left corner at the origin, {@code size} pixels square, and its time label
     * under it; works on a copy of {@code g}.
     */
    public static void paint(Graphics2D g, IconFace face, int size) {
        Graphics2D canvas = (Graphics2D) g.create();
        try {
            hints(canvas);
            float k = size / DESIGN;
            if (face.faded()) {
                canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, FADED_ALPHA));
            }
            Color tone = Java2DFrameRenderer.colorFor(face.tone());
            Color fill = fillOf(tone);
            Color ink = inkOf(tone);
            RoundRectangle2D.Float box = new RoundRectangle2D.Float(0.5f * k, 0.5f * k, size - k, size - k, 4f * k,
                    4f * k);
            canvas.setColor(fill);
            canvas.fill(box);
            paintSymbol(canvas, face, k, ink, fill);
            paintSweep(canvas, face, k, size, box);
            paintBar(canvas, face, k, size, ink);
            paintWard(canvas, face, k, size, fill);
            canvas.setColor(Java2DFrameRenderer.colorFor(face.frame()));
            canvas.setStroke(new BasicStroke(Math.max(1f, 1.25f * k)));
            canvas.draw(box);
            paintTexts(canvas, face, size);
        } finally {
            canvas.dispose();
        }
    }

    /**
     * Only {@code face}'s symbol, in {@code ink} with its knock-outs in {@code fill}, {@code size} pixels square
     * with its top-left corner at the origin: for a frame the caller draws itself.
     */
    static void paintBareSymbol(Graphics2D g, IconFace face, int size, Color ink, Color fill) {
        Graphics2D canvas = (Graphics2D) g.create();
        try {
            hints(canvas);
            paintSymbol(canvas, face, size / DESIGN, ink, fill);
        } finally {
            canvas.dispose();
        }
    }

    private static void paintSymbol(Graphics2D g, IconFace face, float k, Color ink, Color fill) {
        Optional<Pictogram> pictogram = PictogramCatalogue.of(face.pictogram());
        if (pictogram.isEmpty()) {
            paintMonogram(g, face.monogram(), k, ink);
            return;
        }
        Graphics2D s = (Graphics2D) g.create();
        try {
            s.translate(2.75 * k, 2.75 * k);
            float scale = 14.5f * k / Pictogram.UNIT;
            s.scale(scale, scale);
            paintLayers(s, pictogram.get(), ink, fill);
        } finally {
            s.dispose();
        }
    }

    private static void paintMonogram(Graphics2D g, String monogram, float k, Color ink) {
        g.setFont(new Font(Font.DIALOG, Font.BOLD, Math.round(DESIGN * k * 0.55f)));
        FontMetrics metrics = g.getFontMetrics();
        float size = DESIGN * k;
        g.setColor(ink);
        g.drawString(monogram, (size - metrics.stringWidth(monogram)) / 2f,
                (size + metrics.getAscent() - metrics.getDescent()) / 2f);
    }

    /** {@code pictogram} in {@code ink} on the unit grid, its knock-outs in {@code fill}. */
    static void paintLayers(Graphics2D s, Pictogram pictogram, Color ink, Color fill) {
        Color dim = new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 115);
        for (Pictogram.Layer layer : pictogram.layers()) {
            switch (layer.op()) {
                case FILL -> {
                    s.setColor(ink);
                    s.fill(windingOf(layer.shape(), Path2D.WIND_NON_ZERO));
                }
                case EVEN_ODD -> {
                    s.setColor(ink);
                    s.fill(windingOf(layer.shape(), Path2D.WIND_EVEN_ODD));
                }
                case DIM -> {
                    s.setColor(dim);
                    s.fill(windingOf(layer.shape(), Path2D.WIND_NON_ZERO));
                }
                case KNOCK_OUT -> {
                    s.setColor(fill);
                    s.fill(windingOf(layer.shape(), Path2D.WIND_NON_ZERO));
                }
                case STROKE -> stroke(s, ink, 2.25f, layer.shape());
                case THIN -> stroke(s, ink, 1.5f, layer.shape());
                case KNOCK_OUT_STROKE -> stroke(s, fill, 1.75f, layer.shape());
            }
        }
    }

    private static void stroke(Graphics2D s, Color color, float width, Shape shape) {
        s.setColor(color);
        s.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        s.draw(shape);
    }

    private static Shape windingOf(Shape shape, int rule) {
        Path2D.Float path = new Path2D.Float(rule);
        path.append(shape, false);
        return path;
    }

    private static void paintSweep(Graphics2D g, IconFace face, float k, int size, Shape box) {
        if (face.elapsed() <= 0f) {
            return;
        }
        Graphics2D sweep = (Graphics2D) g.create();
        try {
            sweep.clip(box);
            sweep.setColor(new Color(0, 0, 0, 150));
            sweep.fill(new Arc2D.Float(-size / 2f, -size / 2f, size * 2f, size * 2f, 90f, -360f * face.elapsed(),
                    Arc2D.PIE));
        } finally {
            sweep.dispose();
        }
    }

    private static void paintBar(Graphics2D g, IconFace face, float k, int size, Color ink) {
        if (face.bar().isEmpty()) {
            return;
        }
        float width = size - 5f * k;
        g.setColor(new Color(0, 0, 0, 170));
        g.fill(new Rectangle2D.Float(2.5f * k, size - 4.6f * k, width, 2.2f * k));
        g.setColor(ink);
        g.fill(new Rectangle2D.Float(2.5f * k, size - 4.6f * k, (float) (width * face.bar().getAsDouble()), 2.2f * k));
    }

    private static void paintWard(Graphics2D g, IconFace face, float k, int size, Color fill) {
        if (face.ward() == IconFace.Ward.NONE) {
            return;
        }
        Line2D.Float line = new Line2D.Float(3.6f * k, 3.6f * k, size - 3.6f * k, size - 3.6f * k);
        g.setColor(fill);
        g.setStroke(new BasicStroke(3.4f * k, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(line);
        g.setColor(Java2DFrameRenderer.colorFor(Palette.TERM_IMMUNE));
        g.setStroke(face.ward() == IconFace.Ward.IMMUNE
                ? new BasicStroke(1.7f * k, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                : new BasicStroke(1.7f * k, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f,
                        new float[]{2.2f * k, 1.6f * k}, 0f));
        g.draw(line);
    }

    private static void paintTexts(Graphics2D g, IconFace face, int size) {
        if (size >= BUTTON && !face.numeral().isEmpty()) {
            outlined(g, face.numeral(), NUMERAL_FONT, 2.5f * size / DESIGN, 8.5f * size / DESIGN, Color.WHITE);
        }
        if (size >= BUTTON && !face.corner().isEmpty()) {
            g.setFont(COUNT_FONT);
            float width = g.getFontMetrics().stringWidth(face.corner());
            outlined(g, face.corner(), COUNT_FONT, size - 1.5f * size / DESIGN - width, size - 2f * size / DESIGN,
                    Color.WHITE);
        }
        if (!face.below().isEmpty()) {
            g.setFont(LABEL_FONT);
            float width = g.getFontMetrics().stringWidth(face.below());
            g.setColor(new Color(220, 255, 220));
            g.drawString(face.below(), (size - width) / 2f, size + LABEL - 1f);
        }
    }

    private static void outlined(Graphics2D g, String text, Font font, float x, float y, Color color) {
        g.setFont(font);
        g.setColor(Color.BLACK);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    g.drawString(text, x + dx, y + dy);
                }
            }
        }
        g.setColor(color);
        g.drawString(text, x, y);
    }

    private static Color inkOf(Color tone) {
        float luminance = (0.2126f * tone.getRed() + 0.7152f * tone.getGreen() + 0.0722f * tone.getBlue()) / 255f;
        return mix(tone, Color.WHITE, luminance < 0.3f ? 0.5f : 0.3f);
    }

    private static Color fillOf(Color tone) {
        return mix(Color.BLACK, tone, 0.24f);
    }

    private static Color mix(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}
