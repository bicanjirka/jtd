package td.ui;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

class TextTintTest {

    @Test
    void aColourThatAlreadyReadsStaysItself() {
        Color bright = new Color(240, 200, 120);

        assertThat(TextTint.of(bright)).isEqualTo(bright);
    }

    @Test
    void aColourTooDarkToReadLightensUntilItDoes() {
        Color dark = new Color(60, 40, 160);

        Color tint = TextTint.of(dark);

        assertThat(TextTint.contrastOnBackground(tint)).isGreaterThanOrEqualTo(7.0);
    }

    @Test
    void aTintKeepsItsColourRatherThanTurningWhite() {
        Color dark = new Color(60, 40, 160);

        Color tint = TextTint.of(dark);

        assertThat(tint.getBlue()).isGreaterThan(tint.getRed()).isGreaterThan(tint.getGreen());
    }
}
