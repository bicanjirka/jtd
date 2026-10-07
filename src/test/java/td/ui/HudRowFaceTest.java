package td.ui;

import org.junit.jupiter.api.Test;

import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class HudRowFaceTest {

    private static final FontMetrics METRICS = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
            .getFontMetrics(Hud.LABEL_FONT);

    @Test
    void aRowThatFitsIsDrawnAsItIs() {
        Hud.RowFace face = Hud.rowFace("1  Range", "$30", 400, METRICS);

        assertThat(face).isEqualTo(new Hud.RowFace("1  Range", "$30"));
    }

    @Test
    void aLongGateGivesWayBeforeTheNodeNameDoesAndKeepsItsCount() {
        int width = METRICS.stringWidth("2  Focused Optics III") + 70;

        Hud.RowFace face = Hud.rowFace("2  Focused Optics III", "Steady Aim shots 0/20", width, METRICS);

        assertThat(face.left()).isEqualTo("2  Focused Optics III");
        assertThat(face.right()).startsWith("…").endsWith("0/20").isNotEqualTo("Steady Aim shots 0/20");
    }

    @Test
    void aNameThatFitsOnlyAloneIsKeptWholeAndTheRightPartIsDropped() {
        int width = METRICS.stringWidth("2  Focused Optics III") + 14;

        Hud.RowFace face = Hud.rowFace("2  Focused Optics III", "$300", width, METRICS);

        assertThat(face).isEqualTo(new Hud.RowFace("2  Focused Optics III", ""));
    }

    @Test
    void aNameWiderThanTheButtonIsShortenedAndTheRightPartDropped() {
        Hud.RowFace face = Hud.rowFace("2  Focused Optics III", "$300", 80, METRICS);

        assertThat(face.left()).endsWith("…");
        assertThat(face.right()).isEmpty();
        assertThat(METRICS.stringWidth(face.left())).isLessThanOrEqualTo(80 - 12);
    }
}
