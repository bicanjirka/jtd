package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTerm;
import td.term.TermText;
import td.ui.render.IconFace;
import td.ui.render.Palette;
import td.wave.WaveScriptSuggestions.Candidate;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ChoicesTest {

    private final Choices<FakeTerm> choices = Choices.ofTerms(
            List.of(term("b", "Ash"), term("a", "delta"), term("a", "Bravo"), term("b", "Cinder")),
            Comparator.comparing(ChoicesTest::type), ChoicesTest::type,
            term -> type(term).equals("a") ? Palette.TERM_FRAME_HEX : Palette.TERM_FRAME_MARK,
            term -> IconFace.of(term.key(), "", type(term).equals("a") ? Palette.ENEMY_CIRCLE : Palette.ENEMY_GHOST,
                    term.title().equals("Ash") ? Palette.TERM_FRAME_BOON : Palette.TERM_FRAME_HEX));

    @Test
    void rowsGroupByTypeThenByTitleIgnoringCaseHintedWithTheirType() {
        List<Candidate> rows = this.choices.rows();

        assertThat(rows).extracting(Candidate::text).containsExactly("Bravo", "delta", "Ash", "Cinder");
        assertThat(rows).extracting(Candidate::hint).containsExactly("a", "a", "b", "b");
    }

    @Test
    void aTermRowIsWrittenInItsIconsTone() {
        int cinder = 3;

        assertThat(this.choices.lookAt(cinder).text()).isEqualTo(TermIconPainter.ink(Palette.ENEMY_GHOST));
    }

    @Test
    void everyHintOfATypeIsWrittenInTheTypesFrameWhateverItsIconsFrame() {
        List<RowLook> typeB = List.of(this.choices.lookAt(2), this.choices.lookAt(3));

        assertThat(typeB).map(RowLook::hint).containsOnly(TermIconPainter.ink(Palette.TERM_FRAME_MARK));
    }

    @Test
    void twoItemsWithTheSameTitleAndHintEachKeepTheirOwnLook() {
        Choices<String> twins = new Choices<>(List.of("x", "y"), Comparator.naturalOrder(), item -> "Same",
                item -> "", item -> new RowLook(new PaintedIcon(8, 8, g2 -> {
                }), item.equals("x") ? Color.RED : Color.BLUE, Color.GRAY));

        RowLook second = twins.lookAt(1);

        assertThat(second.text()).isEqualTo(Color.BLUE);
    }

    @Test
    void aTermThatCannotBeTakenComesLastInItsTypeGreyedOutAndHintedWithWhy() {
        FakeTerm ash = term("b", "Ash");
        Choices<FakeTerm> some = Choices.ofTerms(List.of(ash, term("b", "Cinder"), term("a", "Bravo")),
                Comparator.comparing(ChoicesTest::type), ChoicesTest::type, term -> Palette.TERM_FRAME_HEX,
                term -> IconFace.of(term.key(), "", Palette.ENEMY_CIRCLE, Palette.TERM_FRAME_HEX),
                term -> term == ash ? Optional.of("not here") : Optional.empty());

        List<Candidate> rows = some.rows();

        assertThat(rows).extracting(Candidate::text).containsExactly("Bravo", "Cinder", "Ash");
        assertThat(rows.get(2).hint()).isEqualTo("not here");
        assertThat(some.usableAt(2)).isFalse();
        assertThat(some.lookAt(2).text()).isNotEqualTo(some.lookAt(1).text());
    }

    private static FakeTerm term(String type, String title) {
        return new FakeTerm(type + "." + title, title, TermText.empty());
    }

    private static String type(FakeTerm term) {
        return term.key().substring(0, 1);
    }
}
