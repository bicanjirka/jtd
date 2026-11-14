package td.ui;

import org.junit.jupiter.api.Test;
import td.ui.render.TowerLook;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class TowerSpritesTest {

    private static final int SIDE = 64;

    private static BufferedImage still(boolean drained) {
        BufferedImage picture = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = picture.createGraphics();
        g.translate(SIDE / 2.0, SIDE / 2.0);
        TowerSprites.paintStill(g, TowerLook.SNIPER, 1, SIDE / 2.0, drained);
        g.dispose();
        return picture;
    }

    private static int paintedPixels(BufferedImage picture, boolean coloured) {
        int count = 0;
        for (int y = 0; y < SIDE; y++) {
            for (int x = 0; x < SIDE; x++) {
                int argb = picture.getRGB(x, y);
                boolean grey = ((argb >> 16) & 0xff) == ((argb >> 8) & 0xff) && ((argb >> 8) & 0xff) == (argb & 0xff);
                if (argb >>> 24 != 0 && grey != coloured) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void aStillTowerIsPaintedInItsColours() {
        BufferedImage picture = still(false);

        assertThat(paintedPixels(picture, true)).isPositive();
    }

    @Test
    void aDrainedStillTowerKeepsItsShapeAndLosesItsColour() {
        BufferedImage picture = still(true);

        assertThat(paintedPixels(picture, false)).isPositive();
        assertThat(paintedPixels(picture, true)).isZero();
    }
}
