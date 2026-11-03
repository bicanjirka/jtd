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
    void numbersKeepOneDecimalAndTwoBelowOne() {
        assertThat(TermNumbers.number(1.25f)).isEqualTo("1.3");
        assertThat(TermNumbers.number(0.25f)).isEqualTo("0.25");
        assertThat(TermNumbers.number(-0.04f)).isEqualTo("-0.04");
        assertThat(TermNumbers.number(0.001f)).isEqualTo("0");
    }

    @Test
    void numbersFromTenThousandAreAbbreviated() {
        assertThat(TermNumbers.number(9999f)).isEqualTo("9999");
        assertThat(TermNumbers.number(12345f)).isEqualTo("12.3k");
        assertThat(TermNumbers.number(1_234_567f)).isEqualTo("1.2M");
        assertThat(TermNumbers.whole(12345f)).isEqualTo("12345");
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
