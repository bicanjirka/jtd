package td.term;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TermNumbersTest {

    @Test
    void percentsRoundToWholeNumbers() {
        assertThat(TermNumbers.percent(0.15f)).isEqualTo("15%");
        assertThat(TermNumbers.signedPercent(0.15f)).isEqualTo("+15%");
        assertThat(TermNumbers.signedPercent(-0.4f)).isEqualTo("-40%");
    }

    @Test
    void numbersDropTrailingZeros() {
        assertThat(TermNumbers.number(1.5f)).isEqualTo("1.5");
        assertThat(TermNumbers.number(4f)).isEqualTo("4");
        assertThat(TermNumbers.seconds(2f)).isEqualTo("2 s");
        assertThat(TermNumbers.times(1.5f)).isEqualTo("x1.5");
    }

    @Test
    void ordinalsUseTheEnglishSuffixes() {
        assertThat(TermNumbers.ordinal(1)).isEqualTo("1st");
        assertThat(TermNumbers.ordinal(2)).isEqualTo("2nd");
        assertThat(TermNumbers.ordinal(3)).isEqualTo("3rd");
        assertThat(TermNumbers.ordinal(11)).isEqualTo("11th");
    }

    @Test
    void oneCellIsSingular() {
        assertThat(TermNumbers.cells(1f)).isEqualTo("1 cell");
        assertThat(TermNumbers.cells(1.5f)).isEqualTo("1.5 cells");
    }
}
