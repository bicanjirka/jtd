package td.ui;

import org.junit.jupiter.api.Test;
import td.board.BoardGround;
import td.board.GroundMotif;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class GroundPainterTest {

    private static final int FLOOR = 0x102030;
    private static final BoardGround COMB = BoardGround.of(FLOOR, GroundMotif.COMB, 0xA0B0C0);

    private final GroundPainter painter = new GroundPainter();

    private static BufferedImage canvas(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    private static GraphicsConfiguration configurationOf(BufferedImage canvas) {
        Graphics2D g2 = canvas.createGraphics();
        try {
            return g2.getDeviceConfiguration();
        } finally {
            g2.dispose();
        }
    }

    private static int rgb(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) & 0xFFFFFF;
    }

    /** {@code canvas} with the ground painted on it under {@code zoom} and {@code shift}, as a board's graphics is. */
    private void paintOn(BufferedImage canvas, BoardGround ground, double zoom, int shift) {
        Graphics2D g2 = canvas.createGraphics();
        try {
            g2.translate(shift, shift);
            g2.scale(zoom, zoom);
            this.painter.paint(g2, ground, 32, 64, 64);
        } finally {
            g2.dispose();
        }
    }

    @Test
    void theSameLayoutKeepsItsImage() {
        GraphicsConfiguration configuration = configurationOf(canvas(1, 1));
        BufferedImage first = this.painter.imageFor(GroundPainter.Layout.of(COMB, 32, 64, 64), configuration);

        assertThat(this.painter.imageFor(GroundPainter.Layout.of(COMB, 32, 64, 64), configuration)).isSameAs(first);
    }

    @Test
    void anotherZoomOrGroundBuildsANewImage() {
        GraphicsConfiguration configuration = configurationOf(canvas(1, 1));
        GroundPainter.Layout unzoomed = GroundPainter.Layout.of(COMB, 32, 64, 64);
        BufferedImage first = this.painter.imageFor(unzoomed, configuration);

        BufferedImage zoomed = this.painter.imageFor(unzoomed.withZoom(1.5, 1.5), configuration);
        BufferedImage plain = this.painter.imageFor(GroundPainter.Layout.of(BoardGround.floor(FLOOR), 32, 64, 64).withZoom(1.5, 1.5),
                configuration);

        assertThat(zoomed).isNotSameAs(first);
        assertThat(zoomed.getWidth()).isEqualTo(96);
        assertThat(plain).isNotSameAs(zoomed);
    }

    @Test
    void itFillsTheBoardAtItsZoomAndLeavesTheRestAlone() {
        BufferedImage canvas = canvas(200, 200);

        this.paintOn(canvas, BoardGround.floor(FLOOR), 2, 10);

        assertThat(rgb(canvas, 10, 10)).isEqualTo(FLOOR);
        assertThat(rgb(canvas, 137, 137)).isEqualTo(FLOOR);
        assertThat(rgb(canvas, 9, 9)).isZero();
        assertThat(rgb(canvas, 138, 138)).isZero();
    }

    @Test
    void itLeavesTheGraphicsAsItFoundIt() {
        BufferedImage canvas = canvas(100, 100);
        Graphics2D g2 = canvas.createGraphics();
        g2.scale(1.5, 1.5);
        var before = g2.getTransform();

        this.painter.paint(g2, COMB, 32, 64, 64);

        assertThat(g2.getTransform()).isEqualTo(before);
        g2.dispose();
    }

    @Test
    void aMotifDrawsMarksOverTheFloor() {
        BufferedImage canvas = canvas(64, 64);

        this.paintOn(canvas, COMB, 1, 0);

        boolean anyMark = false;
        for (int x = 0; x < 64 && !anyMark; x++) {
            for (int y = 0; y < 64 && !anyMark; y++) {
                anyMark = rgb(canvas, x, y) != FLOOR;
            }
        }
        assertThat(anyMark).isTrue();
    }
}
