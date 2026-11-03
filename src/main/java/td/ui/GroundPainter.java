package td.ui;

import td.board.BoardGround;
import td.board.GroundMotif;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;

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
}
