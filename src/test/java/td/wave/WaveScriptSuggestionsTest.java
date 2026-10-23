package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.fixtures.EnemyFixtures;
import td.wave.WaveScriptSuggestions.Candidate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WaveScriptSuggestionsTest {

    private final EnemyCatalog catalog = catalog();

    private static EnemyCatalog catalog() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(EnemyFixtures.simpleDefinition("alpha"));
        catalog.register(EnemyFixtures.simpleDefinition("alder"));
        catalog.register(EnemyFixtures.simpleDefinition("brute"));
        EnemyDefinition lord = EnemyFixtures.simpleDefinition("lord");
        catalog.registerUnique(lord);
        return catalog;
    }

    private List<String> at(String text) {
        return this.suggestAt(text, text.length());
    }

    private List<String> suggestAt(String text, int caret) {
        return WaveScriptSuggestions.suggest(text, caret, this.catalog).candidates().stream()
                .map(Candidate::text).toList();
    }

    @Test
    void aPrefixKeepsOnlyTheTokensThatStartWithIt() {
        assertThat(this.at("al")).containsExactlyInAnyOrder("alpha", "alder");
    }

    @Test
    void theTokenUnderTheCaretIsReplacedWhole() {
        WaveScriptSuggestions.Result result = WaveScriptSuggestions.suggest("2 al brute", 4, this.catalog);

        assertThat(result.start()).isEqualTo(2);
        assertThat(result.end()).isEqualTo(4);
        assertThat(result.candidates()).extracting(Candidate::text).containsExactlyInAnyOrder("alpha", "alder");
    }

    @Test
    void theSpacingTokenIsOnlyOfferedFirst() {
        assertThat(this.at("w")).contains("w");
        assertThat(this.at("alpha w")).isEmpty();
    }

    @Test
    void theSpacingTokenIsAStemTheTicksFinish() {
        assertThat(WaveScriptSuggestions.suggest("w", 1, this.catalog).best()).get()
                .extracting(Candidate::complete).isEqualTo(false);
    }

    @Test
    void ranksAreOfferedWeakestFirst() {
        assertThat(this.at("elite swarm 3 alpha ")).containsSubsequence("grunt", "soldier", "veteran", "elite", "boss");
    }

    @Test
    void aSpacingTokenIsNotTheSlotThatFollowsIt() {
        assertThat(this.at("w30 b")).contains("boss", "brute");
    }

    @Test
    void aShapeThatTakesACountOffersNothingUntilItHasOne() {
        assertThat(this.at("swarm ")).isEmpty();
        assertThat(this.at("swarm 3 ")).contains("alpha");
    }

    @Test
    void aShapeWithAFixedSizeGoesStraightToTheEnemy() {
        assertThat(this.at("plated ")).containsExactlyInAnyOrder("alpha", "alder", "brute", "lord");
    }

    @Test
    void afterARankComesAShapeOrAnEnemyButNotAnotherRankOrASpacer() {
        List<String> next = this.at("elite ");

        assertThat(next).contains("swarm", "alpha").doesNotContain("boss", "grunt", "e");
    }

    @Test
    void anEnemyThatComesOnlyAsBossIsLeftOutAfterAnyOtherRank() {
        assertThat(this.at("elite lo")).isEmpty();
        assertThat(this.at("boss lo")).containsExactly("lord");
    }

    @Test
    void anEnemyEndsTheSlotSoTheNextTokenStartsFresh() {
        assertThat(this.at("elite swarm 3 alpha ")).contains("grunt", "e", "swarm");
    }

    @Test
    void anEnemyAddedToTheCatalogIsSuggestedWithoutAnyOtherChange() {
        this.catalog.register(EnemyFixtures.simpleDefinition("zeta"));

        assertThat(this.at("z")).containsExactly("zeta");
    }

    @Test
    void aCaretPastTheTextClampsToTheEnd() {
        assertThat(this.suggestAt("al", 99)).containsExactlyInAnyOrder("alpha", "alder");
    }

    @Test
    void aTokenTypedInFullComesBeforeLongerOnesItStarts() {
        assertThat(WaveScriptSuggestions.suggest("e", 1, this.catalog).best()).get()
                .extracting(Candidate::text).isEqualTo("e");
    }
}
