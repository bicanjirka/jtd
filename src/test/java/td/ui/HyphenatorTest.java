package td.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HyphenatorTest {

    @Test
    void anOddDigitAllowsABreakAndTheHighestDigitBetweenTwoLettersWins() {
        Hyphenator hyphenator = new Hyphenator(List.of("n1d", "e1n", "e2nd"), List.of());

        List<Integer> breaks = hyphenator.breaksOf("tendering");

        assertThat(breaks).containsExactly(3);
    }

    @Test
    void anEdgePatternMatchesOnlyAtTheStartOfTheWord() {
        Hyphenator hyphenator = new Hyphenator(List.of(".ab1c"), List.of());

        List<Integer> atStart = hyphenator.breaksOf("abcdef");
        List<Integer> inside = hyphenator.breaksOf("xxabcdef");

        assertThat(atStart).containsExactly(2);
        assertThat(inside).isEmpty();
    }

    @Test
    void noBreakLeavesTooFewLettersOnEitherSide() {
        Hyphenator hyphenator = new Hyphenator(List.of("1a", "1b", "1c", "1d", "1e", "1f"), List.of());

        List<Integer> breaks = hyphenator.breaksOf("abcdef");

        assertThat(breaks).containsExactly(2, 3);
    }

    @Test
    void anExceptionReplacesThePatternsAndIgnoresCase() {
        Hyphenator hyphenator = new Hyphenator(List.of("1b"), List.of("pro-ject"));

        List<Integer> breaks = hyphenator.breaksOf("Project");

        assertThat(breaks).containsExactly(3);
    }

    @Test
    void aWordWithAnythingButLettersNeverBreaks() {
        Hyphenator hyphenator = new Hyphenator(List.of("1b"), List.of());

        List<Integer> breaks = hyphenator.breaksOf("aabbb-bbb");

        assertThat(breaks).isEmpty();
    }

    @Test
    void aWordWhoseLowercaseChangesLengthNeverBreaks() {
        Hyphenator hyphenator = new Hyphenator(List.of("1b"), List.of());

        List<Integer> breaks = hyphenator.breaksOf("aaİbbb");

        assertThat(breaks).isEmpty();
    }
}
