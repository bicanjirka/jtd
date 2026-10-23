package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTerm;
import td.term.TermText;
import td.ui.render.IconFace;
import td.ui.render.Palette;
import td.wave.WaveScriptSuggestions.Candidate;

import java.util.Comparator;
import java.util.List;

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
        Candidate cinder = this.choices.rows().get(3);

        assertThat(this.choices.lookOfRow(cinder)).map(RowLook::text)
                .contains(TermIconPainter.ink(Palette.ENEMY_GHOST));
    }

    @Test
    void everyHintOfATypeIsWrittenInTheTypesFrameWhateverItsIconsFrame() {
        List<Candidate> rows = this.choices.rows();

        assertThat(rows.subList(2, 4)).map(row -> this.choices.lookOfRow(row).orElseThrow().hint())
                .containsOnly(TermIconPainter.ink(Palette.TERM_FRAME_MARK));
    }

    private static FakeTerm term(String type, String title) {
        return new FakeTerm(type + "." + title, title, TermText.empty());
    }

    private static String type(FakeTerm term) {
        return term.key().substring(0, 1);
    }
}
