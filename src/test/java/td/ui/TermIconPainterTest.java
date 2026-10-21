package td.ui;

import org.junit.jupiter.api.Test;
import td.ui.render.IconFace;
import td.ui.render.Palette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class TermIconPainterTest {

    private static BufferedImage paint(IconFace face) {
        BufferedImage image = new BufferedImage(TermIconPainter.ROW, TermIconPainter.ROW + TermIconPainter.LABEL,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        TermIconPainter.paint(g, face, TermIconPainter.ROW);
        g.dispose();
        return image;
    }

    private static int distance(int rgb, Color color) {
        Color pixel = new Color(rgb);
        return Math.abs(pixel.getRed() - color.getRed()) + Math.abs(pixel.getGreen() - color.getGreen())
                + Math.abs(pixel.getBlue() - color.getBlue());
    }

    @Test
    void anImmuneFaceDrawsItsSlashAcrossTheDiagonal() {
        IconFace face = IconFace.of("effect.burn", "Bu", Palette.STATUS_MARKER_BURN, Palette.TERM_FRAME_AFFLICTION)
                .withWard(IconFace.Ward.IMMUNE);

        BufferedImage image = paint(face);

        assertThat(distance(image.getRGB(10, 10), Java2DFrameRenderer.colorFor(Palette.TERM_IMMUNE)))
                .isLessThan(60);
    }

    @Test
    void aFaceWithoutArtPaintsItsMonogram() {
        IconFace blank = IconFace.of("none", "", Palette.STATUS_MARKER_BURN, Palette.TERM_FRAME_AFFLICTION);
        IconFace named = IconFace.of("none", "Ab", Palette.STATUS_MARKER_BURN, Palette.TERM_FRAME_AFFLICTION);

        BufferedImage without = paint(blank);
        BufferedImage with = paint(named);

        boolean differs = false;
        for (int x = 0; x < TermIconPainter.ROW; x++) {
            for (int y = 0; y < TermIconPainter.ROW; y++) {
                differs |= without.getRGB(x, y) != with.getRGB(x, y);
            }
        }
        assertThat(differs).isTrue();
    }
}
