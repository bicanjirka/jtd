package td.wave;

import td.enemy.Affix;
import td.enemy.EnemyCatalog;
import td.enemy.Rank;
import td.wave.WaveScriptGrammar.NextWords;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * What could be typed at the caret of a wave script: what {@link WaveScriptGrammar} accepts after the tokens before
 * the caret. It never reports a problem: the parser does, and past one it offers nothing.
 */
public final class WaveScriptSuggestions {

    private static final String SPACING_STEM = "w";
    private static final List<ShapeKeyword> SHAPES = Stream.of(ShapeKeyword.values())
            .sorted(Comparator.comparing(ShapeKeyword::token)).toList();

    private WaveScriptSuggestions() {
    }

    /**
     * One thing that can be typed, and a few words on what it is. A stem is only the start of a
     * token, so taking it leaves the caret on the token rather than past it.
     */
    public record Candidate(String text, String hint, boolean complete) {

        public static Candidate token(String text, String hint) {
            return new Candidate(text, hint, true);
        }

        public static Candidate stem(String text, String hint) {
            return new Candidate(text, hint, false);
        }
    }

    /**
     * The candidates, best first, for the token spanning {@code start} to {@code end}. Taking one
     * replaces that span.
     */
    public record Result(int start, int end, List<Candidate> candidates) {

        public Result {
            candidates = List.copyOf(candidates);
        }

        public static Result none() {
            return new Result(0, 0, List.of());
        }

        public Optional<Candidate> best() {
            return this.candidates.stream().findFirst();
        }
    }

    public static Result suggest(String text, int caret, EnemyCatalog catalog) {
        int at = Math.clamp(caret, 0, text.length());
        int start = text.lastIndexOf(' ', at - 1) + 1;
        int end = text.indexOf(' ', at) < 0 ? text.length() : text.indexOf(' ', at);
        String prefix = text.substring(start, at).toLowerCase(Locale.ROOT);

        List<Candidate> candidates = new ArrayList<>();
        for (Candidate candidate : candidatesAt(text.substring(0, start), catalog)) {
            if (candidate.text().equals(prefix)) {
                candidates.addFirst(candidate);
            } else if (candidate.text().startsWith(prefix)) {
                candidates.add(candidate);
            }
        }
        return new Result(start, end, candidates);
    }

    private static List<Candidate> candidatesAt(String typed, EnemyCatalog catalog) {
        NextWords next;
        try {
            next = new WaveScriptGrammar(Rank.GRUNT, catalog).next(typed.split(" "));
        } catch (WaveScriptGrammar.ScriptFault fault) {
            // The script box marks the fault; nothing typed after it can mend it.
            return List.of();
        }
        List<Candidate> all = new ArrayList<>();
        if (next.accepts(NextWords.Kind.SPACING)) {
            all.add(Candidate.stem(SPACING_STEM, "ticks per slot, e.g. w30:"));
        }
        if (next.accepts(NextWords.Kind.HEAD_END)) {
            all.add(Candidate.token(WaveScript.HEAD_END, "ends the head"));
        }
        if (next.accepts(NextWords.Kind.RANK)) {
            for (RankKeyword rank : RankKeyword.values()) {
                all.add(Candidate.token(rank.token(), "rank"));
            }
        }
        if (next.accepts(NextWords.Kind.SPACER)) {
            all.add(Candidate.token(WaveScript.EMPTY_TOKEN, "spacer"));
        }
        if (next.accepts(NextWords.Kind.SHAPE)) {
            for (ShapeKeyword shape : SHAPES) {
                all.add(Candidate.token(shape.token(), shape.takesMemberCount() ? "shape, needs a count" : "shape"));
            }
        }
        for (Affix affix : next.affixes()) {
            all.add(Candidate.token(affix.word(), "affix"));
        }
        for (String id : catalog.ids()) {
            boolean unique = catalog.ranked(id).isUnique();
            if (next.accepts(unique ? NextWords.Kind.UNIQUE_ENEMY : NextWords.Kind.ENEMY)) {
                all.add(Candidate.token(id, unique ? "enemy, boss only" : "enemy"));
            }
        }
        return all;
    }
}
