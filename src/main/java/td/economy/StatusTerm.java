package td.economy;

import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;

/** What the status bar counts: the round, the lives left, the score and the cash. */
public enum StatusTerm implements Term {
    WAVE("Wave", "a round sends one wave down each path"),
    LIVES("Lives", "an enemy that reaches the end costs lives, and its bounty off your score"),
    SCORE("Score", "kills add their bounty, more for higher ranks; a leak takes its bounty back"),
    CASH("Cash", "kills pay their bounty; selling a tower refunds " + TermNumbers.percent(SellRefund.SHARE)
            + " of what you paid for it");

    private final String title;
    private final String meaning;

    StatusTerm(String title, String meaning) {
        this.title = title;
        this.meaning = meaning;
    }

    @Override
    public String key() {
        return "status." + this.name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public TermText describe() {
        return TermText.of(this.meaning);
    }
}
