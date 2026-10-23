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

class TermPickerTest {

    private final TermPicker<FakeTerm> picker = new TermPicker<>(
            List.of(term("b", "Ash"), term("a", "delta"), term("a", "Bravo"), term("b", "Cinder")),
            Comparator.comparing(TermPickerTest::type), TermPickerTest::type,
            term -> IconFace.of(term.key(), "", Palette.ENEMY_CIRCLE, Palette.ENEMY_CIRCLE));

    @Test
    void rowsGroupByTypeThenByTitleIgnoringCaseHintedWithTheirType() {
        List<Candidate> rows = this.picker.rows();

        assertThat(rows).extracting(Candidate::text).containsExactly("Bravo", "delta", "Ash", "Cinder");
        assertThat(rows).extracting(Candidate::hint).containsExactly("a", "a", "b", "b");
    }

    @Test
    void aRowShowsTheFaceOfTheTermItNames() {
        Candidate cinder = this.picker.rows().get(3);

        assertThat(this.picker.faceOfRow(cinder)).map(IconFace::pictogram).contains("b.Cinder");
    }

    private static FakeTerm term(String type, String title) {
        return new FakeTerm(type + "." + title, title, TermText.empty());
    }

    private static String type(FakeTerm term) {
        return term.key().substring(0, 1);
    }
}
