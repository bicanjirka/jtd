package td.enemy;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.fixtures.WorldFixtures;
import td.term.Term;
import td.term.TermText;
import td.tower.AbstractTower;
import td.tower.AppliedEffect;
import td.tower.TowerFactory;
import td.tower.TowerTerm;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every text the game builds names a term by mentioning it, and never as a plain word or an inflection of one.
 * Lives in {@code td.enemy} because it reads the package-private ability and trait wording.
 */
class TermWordingTest {

    private static final float CELL_SIZE = 40f;
    private static final List<String> FORBIDDEN_WORDS = List.of("chilled", "chills", "burning", "burns", "burned",
            "frozen", "freezes", "shielded", "healing", "revealed", "reveals", "poisoned", "scorched", "sickened",
            "sundered", "exposed", "exposes", "marked", "marks", "resonating", "fractured", "dazed", "dazes",
            "saturated", "saturates", "charged", "doomed", "blighted", "contagious", "rimed", "ashen", "inverted",
            "sympathetic", "reckoned", "silenced", "silences", "anchored", "unraveled", "unravels", "corroded",
            "dragged", "condemned", "condemns", "stilled", "unveiled", "cracked", "cracks", "tarred", "bleeding",
            "bleeds", "sprinting", "sprints");

    /** One text of the game, where it came from and whether it is a tower's. */
    private record Source(String label, TermText text, boolean tower) {
    }

    @Test
    void noPlainTextNamesATermWithoutMentioningIt() {
        List<String> titles = new ArrayList<>();
        Arrays.stream(EffectKind.values()).map(Term::title).forEach(titles::add);
        Arrays.stream(TraitTerm.values()).map(Term::title).forEach(titles::add);
        List<String> towerTitles = Arrays.stream(TowerTerm.values()).map(Term::title).toList();
        List<String> violations = new ArrayList<>();

        for (Source source : corpus()) {
            for (String span : plainSpans(source.text())) {
                Stream<String> checked = source.tower() ? Stream.concat(titles.stream(), towerTitles.stream())
                        : titles.stream();
                checked.filter(title -> Pattern.compile("\\b" + Pattern.quote(title) + "\\b").matcher(span).find())
                        .forEach(title -> violations.add(source.label() + ": \"" + span + "\" names " + title));
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void noPlainTextUsesAWordMadeFromATerm() {
        List<String> violations = new ArrayList<>();

        for (Source source : corpus()) {
            for (String span : plainSpans(source.text())) {
                String lower = span.toLowerCase(Locale.ROOT);
                FORBIDDEN_WORDS.stream()
                        .filter(word -> Pattern.compile("\\b" + word + "\\b").matcher(lower).find())
                        .forEach(word -> violations.add(source.label() + ": \"" + span + "\" uses " + word));
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void theCorpusCoversEveryKindOfText() {
        assertThat(corpus()).hasSizeGreaterThan(500);
    }

    private static List<String> plainSpans(TermText text) {
        return text.spans().stream()
                .filter(span -> span instanceof TermText.Plain)
                .map(span -> ((TermText.Plain) span).text())
                .toList();
    }

    private static List<Source> corpus() {
        List<Source> corpus = new ArrayList<>();
        for (EffectKind kind : EffectKind.values()) {
            corpus.add(new Source("effect " + kind, kind.describe(), false));
        }
        for (TraitTerm term : TraitTerm.values()) {
            corpus.add(new Source("trait " + term, term.describe(), false));
        }
        for (AbilityTerm term : AbilityTerm.values()) {
            corpus.add(new Source("ability " + term, term.describe(), false));
        }
        for (TowerTerm term : TowerTerm.values()) {
            corpus.add(new Source("tower term " + term, term.describe(), true));
        }
        GameWorld world = WorldFixtures.newWorld();
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            AbstractTower tower = (AbstractTower) TowerFactory.createTower(type, world, 3, 3);
            for (UpgradeNode node : tower.upgradeTree().nodes()) {
                String label = type + " " + node.displayName();
                tower.wordsOf(node).sentences()
                        .forEach(text -> corpus.add(new Source(label, text, true)));
                corpus.add(new Source(label + " gate", node.gate().describe(), true));
            }
            for (AppliedEffect applied : tower.applies()) {
                for (AppliedEffect.Source how : applied.sources()) {
                    corpus.add(new Source(type + " applies " + applied.term().title(), how.how(), true));
                }
            }
            corpus.add(new Source(type + " shop", TermText.of(tower.inspect().description()), true));
        }
        EnemyCatalog catalog = EnemyCatalog.builtIn();
        AbilityText.Words words = new AbilityText.Words(CELL_SIZE, id -> catalog.get(id).displayName());
        for (String id : catalog.ids()) {
            for (Rank rank : Rank.values()) {
                EnemyDefinition definition = catalog.get(id, rank);
                String label = id + " " + rank;
                corpus.add(new Source(label + " description", TermText.of(definition.description()), false));
                definition.traitSlots().forEach(slot -> corpus.add(
                        new Source(label + " trait", slot.template().entry().text(), false)));
                definition.abilities().forEach(ability -> corpus.add(
                        new Source(label + " ability", AbilityText.of(ability, words), false)));
            }
        }
        return corpus;
    }
}
