package td.enemy;

import td.term.Term;
import td.term.TermText;

import java.util.Locale;

/** A trait an enemy carries, named for the player. */
public enum TraitTerm implements Term {
    REGENERATION("Regeneration", "heals itself over time"),
    ADRENALINE("Adrenaline", "moves faster the more it is hurt"),
    ELUSIVE("Elusive", "dodges more the more it is hurt"),
    ADAPTATION("Adaptation", "resists whichever damage type has hit its kind most this level"),
    JAM("Jam", "weakens the towers near it");

    private final String title;
    private final String sentence;

    TraitTerm(String title, String sentence) {
        this.title = title;
        this.sentence = sentence;
    }

    @Override
    public String key() {
        return "trait." + this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public TermText describe() {
        return TermText.of(this.sentence);
    }
}
