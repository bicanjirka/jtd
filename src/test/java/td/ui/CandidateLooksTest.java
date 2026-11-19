package td.ui;

import org.junit.jupiter.api.Test;
import td.enemy.Rank;
import td.fixtures.EnemyFixtures;
import td.ui.render.SheetTooltip;
import td.wave.WaveScriptSuggestions.Candidate;
import td.wave.WaveScriptSuggestions.Subject;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateLooksTest {

    private final CandidateLooks looks = new CandidateLooks(() -> 40);

    private static Candidate enemy(String id) {
        return Candidate.token(id, "enemy", new Subject.EnemySubject(EnemyFixtures.simpleDefinition(id), Rank.GRUNT));
    }

    @Test
    void everyEnemyWearsTheSameGlyphButOpensACardOfItsOwn() {
        Candidate alpha = enemy("alpha");
        Candidate brute = enemy("brute");

        SheetTooltip alphaCard = this.looks.tooltip(alpha).orElseThrow();
        SheetTooltip bruteCard = this.looks.tooltip(brute).orElseThrow();

        assertThat(this.looks.look(alpha)).isEqualTo(this.looks.look(brute)).isPresent();
        assertThat(alphaCard.sheet()).isNotEqualTo(bruteCard.sheet());
    }

    @Test
    void aRankShowsItsBadgeButOpensNoCard() {
        Candidate elite = Candidate.token("elite", "rank", new Subject.RankSubject(Rank.ELITE));

        assertThat(this.looks.look(elite)).isPresent();
        assertThat(this.looks.tooltip(elite)).isEmpty();
    }

    @Test
    void aWordOfTheGrammarShowsNoGlyphAndOpensNoCard() {
        Candidate spacer = Candidate.token("e", "spacer");

        assertThat(this.looks.look(spacer)).isEmpty();
        assertThat(this.looks.tooltip(spacer)).isEmpty();
    }
}
