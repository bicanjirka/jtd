package td.ui;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.term.TermText;
import td.ui.render.InfoSheet;
import td.ui.render.SheetLine;

import static org.assertj.core.api.Assertions.assertThat;

class TermSheetsTest {

    private static TermText.Mention mention(TermText text) {
        return (TermText.Mention) text.spans().getFirst();
    }

    @Test
    void aMentionsTooltipIsItsTitleThenItsMeaningAsASentence() {
        InfoSheet sheet = TermSheets.of(mention(TermText.of(EffectKind.UNRAVELED)));

        assertThat(sheet.lines()).hasSize(2);
        assertThat(sheet.lines().getFirst()).isInstanceOfSatisfying(SheetLine.IconTitle.class,
                title -> assertThat(title.name()).isEqualTo("Unravel"));
        assertThat(sheet.lines().getLast()).isInstanceOfSatisfying(SheetLine.Sentence.class,
                sentence -> assertThat(sentence.text()).isEqualTo(EffectKind.UNRAVELED.describe().asSentence()));
    }

    @Test
    void aMentionWithDetailShowsTheDetailNotTheTermsOwnSentence() {
        TermText detail = TermText.of("a curse of this tower's own");

        InfoSheet sheet = TermSheets.of(mention(TermText.of(EffectKind.DOOM, detail)));

        assertThat(sheet.lines().getLast()).isInstanceOfSatisfying(SheetLine.Sentence.class,
                sentence -> assertThat(sentence.text().plain()).isEqualTo("A curse of this tower's own."));
    }
}
