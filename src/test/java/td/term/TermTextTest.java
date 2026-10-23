package td.term;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTerm;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TermTextTest {

    private static final Term CHILL = new FakeTerm("effect.chill", "Chill", TermText.of("slows"));
    private static final Term BURN = new FakeTerm("effect.burn", "Burn", TermText.of("damages"));

    @Test
    void formatPutsAMentionInEachSlotInOrder() {
        TermText text = TermText.format("applies {} and {} to it", CHILL, BURN);

        assertThat(text.mentions()).containsExactly(CHILL, BURN);
        assertThat(text.plain()).isEqualTo("applies Chill and Burn to it");
    }

    @Test
    void formatRejectsSlotsAndTermsThatDoNotPairUp() {
        assertThatThrownBy(() -> TermText.format("applies {}", CHILL, BURN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TermText.format("applies {} and {}", CHILL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void plusJoinsAdjacentPlainRunsIntoOne() {
        TermText text = TermText.of("a ").plus("b");

        assertThat(text.spans()).hasSize(1);
        assertThat(text.plain()).isEqualTo("a b");
    }

    @Test
    void emptyIsTheIdentityOfPlus() {
        TermText text = TermText.of(CHILL);

        assertThat(text.plus(TermText.empty())).isEqualTo(text);
        assertThat(TermText.empty().plus(text)).isEqualTo(text);
    }

    @Test
    void plainShowsAMentionAsItsTitle() {
        assertThat(TermText.of("a ").plus(CHILL).plain()).isEqualTo("a Chill");
    }

    @Test
    void aMentionWithoutDetailMeansItsTermsOwnSentence() {
        TermText.Mention mention = (TermText.Mention) TermText.of(CHILL).spans().getFirst();

        assertThat(mention.meaning()).isEqualTo(CHILL.describe());
    }

    @Test
    void aMentionWithDetailMeansTheDetail() {
        TermText detail = TermText.of("slows by half");
        TermText.Mention mention = (TermText.Mention) TermText.of(CHILL, detail).spans().getFirst();

        assertThat(mention.meaning()).isEqualTo(detail);
    }

    @Test
    void asSentenceCapitalizesTheFirstWordAndEndsWithAFullStop() {
        assertThat(TermText.of("slows it").asSentence().plain()).isEqualTo("Slows it.");
        assertThat(TermText.of("slows it.").asSentence().plain()).isEqualTo("Slows it.");
    }

    @Test
    void joinLeavesEmptyPartsOut() {
        TermText text = TermText.join(", ", List.of(TermText.of("a"), TermText.empty(), TermText.of("b")));

        assertThat(text.plain()).isEqualTo("a, b");
    }
}
