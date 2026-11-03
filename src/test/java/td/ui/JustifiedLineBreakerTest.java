package td.ui;

import org.junit.jupiter.api.Test;
import td.ui.JustifiedLineBreaker.TextLine;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JustifiedLineBreakerTest {

    /** Every character one unit wide, and a break allowed after every letter the minimums permit. */
    private final JustifiedLineBreaker breaker = new JustifiedLineBreaker(
            new Hyphenator(List.of("1a", "1b", "1c", "1d", "1e", "1f", "1g", "1h"), List.of()), String::length);

    @Test
    void wordsFillALineAndOnlyTheParagraphsLastLineIsNotStretched() {
        List<TextLine> lines = this.breaker.lines("ab cd ef gh", 5);

        assertThat(lines).containsExactly(TextLine.stretched(List.of("ab", "cd")), TextLine.last(List.of("ef", "gh")));
    }

    @Test
    void aWordThatDoesNotFitSplitsAtItsLastBreakWhoseFirstPartAndHyphenStillFit() {
        List<TextLine> lines = this.breaker.lines("ab abcdefgh", 8);

        assertThat(lines).containsExactly(TextLine.stretched(List.of("ab", "abcd-")), TextLine.last(List.of("efgh")));
    }

    @Test
    void punctuationAroundAWordStaysWithItsPiece() {
        List<TextLine> lines = this.breaker.lines("ab (abcdefgh),", 9);

        assertThat(lines).containsExactly(TextLine.stretched(List.of("ab", "(abcd-")), TextLine.last(List.of("efgh),")));
    }

    @Test
    void aWordWithNoBreakThatFitsMovesWholeToTheNextLine() {
        List<TextLine> lines = this.breaker.lines("abcd abcd", 7);

        assertThat(lines).containsExactly(TextLine.stretched(List.of("abcd")), TextLine.last(List.of("abcd")));
    }

    @Test
    void aWordsOwnHyphenIsASplitThatAddsNoSecondHyphen() {
        List<TextLine> lines = this.breaker.lines("ab well-known", 9);

        assertThat(lines).containsExactly(TextLine.stretched(List.of("ab", "well-")), TextLine.last(List.of("known")));
    }

    @Test
    void whatIsLeftOfASplitWordBreaksOnlyWhereTheWholeWordMay() {
        JustifiedLineBreaker edgeAware = new JustifiedLineBreaker(new Hyphenator(List.of("1e", ".ef1g"), List.of()), String::length);

        List<TextLine> lines = edgeAware.lines("abcdefghij", 5);

        assertThat(lines).containsExactly(TextLine.stretched(List.of("abcd-")), TextLine.last(List.of("efghij")));
    }

    @Test
    void aParagraphEndingOnAnOverflowingWordGetsNoEmptyLastLine() {
        List<TextLine> lines = this.breaker.lines("ab 1234567", 5);

        assertThat(lines).containsExactly(TextLine.stretched(List.of("ab")), TextLine.last(List.of("1234567")));
    }

    @Test
    void aNewlineEndsAParagraph() {
        List<TextLine> lines = this.breaker.lines("ab\ncd", 20);

        assertThat(lines).containsExactly(TextLine.last(List.of("ab")), TextLine.last(List.of("cd")));
    }
}
