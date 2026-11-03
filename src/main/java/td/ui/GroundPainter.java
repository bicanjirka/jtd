package td.ui;

import td.board.BoardGround;
import td.board.GroundMotif;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

/** Paints a board's floor and its motif, under everything else on the board. */
final class GroundPainter {

    private static final Stroke MOTIF_STROKE = new BasicStroke(1f);

    private GroundPainter() {
    }

    /** Fills the whole board, so it is also what clears the previous frame. */
    static void paint(Graphics2D g2, BoardGround ground, int scale, int width, int height) {
        g2.setColor(new Color(ground.floorRgb()));
        g2.fillRect(0, 0, width, height);
        if (ground.motif() == GroundMotif.NONE || scale <= 0) {
            return;
        }
        GeneralPath motif = switch (ground.motif()) {
            case NONE -> new GeneralPath();
            case COMB -> comb(scale, width, height);
            case RAIN -> rain(scale, width, height);
            case DUNES -> dunes(scale, width, height);
            case PLATES -> plates(scale, width, height);
            case FISSURES -> fissures(scale, width, height);
            case FLAGSTONES -> flagstones(scale, width, height);
        };
        Object antialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Stroke stroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(MOTIF_STROKE);
        g2.setColor(new Color(ground.motifRgb()));
        g2.draw(motif);
        g2.setStroke(stroke);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
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

    /** Rippled lines across the board, each row shifted from the last. */
    private static GeneralPath dunes(int scale, int width, int height) {
        GeneralPath p = new GeneralPath();
        float rowStep = scale * 0.7f;
        float amplitude = scale * 0.12f;
        double wavelength = scale * 3.0;
        float segment = scale / 4f;
        int row = 0;
        for (float y = rowStep / 2f; y < height; y += rowStep, row++) {
            double phase = row * 1.3;
            p.moveTo(0, y + (float) (Math.sin(phase) * amplitude));
            for (float x = segment; x <= width + segment; x += segment) {
                p.lineTo(x, y + (float) (Math.sin(phase + x / wavelength * 2 * Math.PI) * amplitude));
            }
        }
        return p;
    }

    /** Seams two cells apart, with a rivet at every corner and halfway along each seam. */
    private static GeneralPath plates(int scale, int width, int height) {
        GeneralPath p = new GeneralPath();
        float plate = scale * 2f;
        float rivet = scale * 0.07f;
        for (float x = plate; x < width; x += plate) {
            p.moveTo(x, 0);
            p.lineTo(x, height);
        }
        for (float y = plate; y < height; y += plate) {
            p.moveTo(0, y);
            p.lineTo(width, y);
        }
        for (float x = plate / 2f; x < width; x += plate / 2f) {
            for (float y = plate / 2f; y < height; y += plate / 2f) {
                p.append(new Ellipse2D.Float(x - rivet - scale * 0.15f, y - rivet - scale * 0.15f, rivet * 2, rivet * 2),
                        false);
            }
        }
        return p;
    }

    /** Jagged cracks from the middle out to past the edges, some forking once, the same every frame. */
    private static GeneralPath fissures(int scale, int width, int height) {
        GeneralPath p = new GeneralPath();
        float cx = width / 2f;
        float cy = height / 2f;
        double reach = Math.hypot(width, height) / 2;
        int cracks = 11;
        for (int i = 0; i < cracks; i++) {
            double angle = i * 2 * Math.PI / cracks + jitter(i, 1) * 0.35;
            crack(p, cx, cy, angle, scale * 0.6, reach, scale, i);
            if (i % 3 == 0) {
                double fork = reach * (0.35 + 0.2 * jitter(i, 2));
                float fx = cx + (float) (Math.cos(angle) * fork);
                float fy = cy + (float) (Math.sin(angle) * fork);
                crack(p, fx, fy, angle + 0.6 * Math.signum(jitter(i, 3) + 0.01), 0, reach * 0.4, scale, i + 50);
            }
        }
        return p;
    }

    private static void crack(GeneralPath p, float x, float y, double angle, double from, double length, int scale,
                              int seed) {
        float step = scale * 0.6f;
        float px = x + (float) (Math.cos(angle) * from);
        float py = y + (float) (Math.sin(angle) * from);
        p.moveTo(px, py);
        int n = 0;
        for (double d = from + step; d < from + length; d += step, n++) {
            double wobble = angle + jitter(seed, n + 10) * 0.5;
            px += (float) (Math.cos(wobble) * step);
            py += (float) (Math.sin(wobble) * step);
            p.lineTo(px, py);
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
