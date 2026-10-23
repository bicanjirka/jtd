package td.fixtures;

import td.term.Term;
import td.term.TermText;

public record FakeTerm(String key, String title, TermText sentence) implements Term {

    @Override
    public TermText describe() {
        return this.sentence;
    }
}
