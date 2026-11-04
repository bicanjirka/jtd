package td.ui;

import org.junit.jupiter.api.Test;
import td.board.BoardGround;
import td.ui.render.Palette;
import td.ui.render.RenderFrame;
import td.ui.render.ZoneDraw;
import td.ui.render.ZonePatchDraw;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ZonePatchPaintTest {

    private static final int SIZE = 200;
    private static final float CENTER = 100f;
    private static final float RADIUS = 40f;
    /** How near a stretch of rim a box looks for its brightest pixel. */
    private static final int BOX = 3;
    /** What antialiasing alone can move a pixel by. */
    private static final int ROUNDING = 8;

    private static BufferedImage painted(List<ZoneDraw> discs) {
        ZonePatchDraw patch = new ZonePatchDraw(Palette.ZONE_HEALING, discs, ZoneOutlineTracer.trace(discs));
        RenderFrame frame = new RenderFrame(32, SIZE, SIZE, List.of(), List.of(patch), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(),
                BoardGround.floor(0));
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        try {
            new Java2DFrameRenderer().paint(g2, frame);
        } finally {
            g2.dispose();
        }
        return image;
    }

    private static int brightestAround(BufferedImage image, double x, double y) {
        int brightest = 0;
        for (int px = (int) x - BOX; px <= (int) x + BOX; px++) {
            for (int py = (int) y - BOX; py <= (int) y + BOX; py++) {
                int rgb = image.getRGB(px, py);
                brightest = Math.max(brightest, (rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF));
            }
        }
        return brightest;
    }

    @Test
    void theOutlineIsNoBrighterWhereTwoRimsMeetThanAlongARim() {
        float right = CENTER + RADIUS;

        BufferedImage image = painted(List.of(new ZoneDraw(CENTER, CENTER, RADIUS, 1f, 0f),
                new ZoneDraw(right, CENTER, RADIUS, 1f, 0f)));

        double meetX = CENTER + RADIUS / 2;
        double meetY = CENTER - RADIUS * Math.sqrt(3) / 2;
        int alongARim = brightestAround(image, CENTER - RADIUS, CENTER);
        assertThat(brightestAround(image, meetX, meetY)).isLessThanOrEqualTo(alongARim + ROUNDING);
    }
}
