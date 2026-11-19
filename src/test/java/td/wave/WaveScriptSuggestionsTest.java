package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.Affix;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.fixtures.EnemyFixtures;
import td.wave.WaveScriptSuggestions.Candidate;
import td.wave.WaveScriptSuggestions.Subject;

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
        catalog.register(RankedEnemy.unique(lord));
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
        assertThat(this.at("alpha w")).doesNotContain("w");
    }

    @Test
    void theHeadEndIsOfferedAfterTheHeadsWords() {
        assertThat(this.at("w30 soldier ")).contains(":");
        assertThat(this.at("w30 ")).contains(":");
    }

    @Test
    void theHeadEndIsNotOfferedOnceASlotHasBegun() {
        assertThat(this.at("2 ")).doesNotContain(":");
        assertThat(this.at("alpha ")).doesNotContain(":");
    }

    @Test
    void theSpacingTokenIsNotOfferedAfterTheHead() {
        assertThat(this.at("soldier: ")).doesNotContain("w").contains("alpha");
    }

    @Test
    void anAffixIsOfferedBeforeTheEnemyAndOnlyTheEnemyOrAnotherAffixAfterIt() {
        assertThat(this.at("elite ")).contains("armored");
        assertThat(this.at("elite armored ")).contains("alpha").doesNotContain("swarm", "e");
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
    void afterASpacingOnlyTheHeadsWordsFollow() {
        assertThat(this.at("w30 b")).contains("boss").doesNotContain("brute");
    }

    @Test
    void aSlotThatCarriesTheMostAffixesIsOfferedNoMore() {
        assertThat(this.at("armored hulking ")).doesNotContain("elusive").contains("alpha");
        assertThat(this.at("armored: hulking ")).doesNotContain("elusive").contains("alpha");
    }

    @Test
    void anAffixThatExcludesOneTheSlotCarriesIsNotOffered() {
        assertThat(this.at("regenerating ")).doesNotContain("withered", "regenerating").contains("armored");
    }

    @Test
    void anEnemyThatTakesNoAffixIsLeftOutAfterOne() {
        assertThat(this.at("armored lo")).isEmpty();
    }

    @Test
    void nothingIsOfferedPastAFault() {
        assertThat(this.at("elite elite ")).isEmpty();
    }

    @Test
    void aShapeThatTakesACountOffersNothingUntilItHasOne() {
        assertThat(this.at("swarm ")).isEmpty();
        assertThat(this.at("swarm 3 ")).contains("alpha");
    }

    @Test
    void aShapeWithAFixedSizeGoesStraightToTheEnemy() {
        assertThat(this.at("flank ")).contains("alpha", "alder", "brute", "lord").doesNotContain("swarm", "elite");
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

    private Subject subjectOf(String text, String token) {
        return WaveScriptSuggestions.suggest(text, text.length(), this.catalog).candidates().stream()
                .filter(candidate -> candidate.text().equals(token)).findFirst().orElseThrow().subject();
    }

    @Test
    void eachWordNamesWhatItIs() {
        assertThat(this.subjectOf("", "soldier")).isEqualTo(new Subject.RankSubject(Rank.SOLDIER));
        assertThat(this.subjectOf("", "flank")).isEqualTo(new Subject.TermSubject(ShapeKeyword.FLANK));
        assertThat(this.subjectOf("", "armored")).isEqualTo(new Subject.TermSubject(Affix.ARMORED));
        assertThat(this.subjectOf("", "e")).isEqualTo(Subject.syntax());
    }

    @Test
    void anEnemyIsNamedAtTheRankItSpawnsAtWhenNothingRanksIt() {
        assertThat(this.subjectOf("", "alpha")).isEqualTo(
                new Subject.EnemySubject(this.catalog.ranked("alpha").definitionFor(Rank.GRUNT), Rank.GRUNT));
        assertThat(this.subjectOf("", "lord")).isEqualTo(
                new Subject.EnemySubject(this.catalog.ranked("lord").definitionFor(Rank.BOSS), Rank.BOSS));
    }
}
