package td.wave;

import td.enemy.EnemyCatalog;
import td.enemy.Rank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * What could be typed at the caret of a wave script, following the order {@link WaveScript} accepts
 * its tokens in. It reads the tokens before the caret, never reports a problem: the parser does.
 */
public final class WaveScriptSuggestions {

    private static final Pattern COUNT = Pattern.compile("\\d+");
    private static final String SPACING_STEM = "w";
    private static final List<String> RANKS = WaveScript.RESERVED_TOKENS.stream()
            .filter(WaveScript::isRankToken).sorted(Comparator.comparing(WaveScript::rankFor)).toList();
    private static final List<String> SHAPES = WaveScript.RESERVED_TOKENS.stream()
            .filter(WaveScript::isShapeToken).sorted().toList();

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

        Position position = positionAfter(text.substring(0, start), catalog);
        List<Candidate> candidates = new ArrayList<>();
        for (Candidate candidate : candidatesAt(position, catalog)) {
            if (candidate.text().equals(prefix)) {
                candidates.addFirst(candidate);
            } else if (candidate.text().startsWith(prefix)) {
                candidates.add(candidate);
            }
        }
        return new Result(start, end, candidates);
    }

    private static List<Candidate> candidatesAt(Position position, EnemyCatalog catalog) {
        List<Candidate> all = new ArrayList<>();
        if (position.shape() == ShapeState.AWAITING_COUNT) {
            return all;
        }
        if (position.first()) {
            all.add(Candidate.stem(SPACING_STEM, "ticks per slot, e.g. w30"));
        }
        if (position.shape() == ShapeState.NONE) {
            if (position.rank().isEmpty()) {
                for (String rank : RANKS) {
                    all.add(Candidate.token(rank, "rank"));
                }
                all.add(Candidate.token(WaveScript.EMPTY_TOKEN, "spacer"));
            }
            for (String shape : SHAPES) {
                all.add(Candidate.token(shape, WaveScript.takesMemberCount(shape) ? "shape, needs a count" : "shape"));
            }
        }
        boolean belowBoss = position.rank().filter(rank -> rank != Rank.BOSS).isPresent();
        for (String id : catalog.ids()) {
            boolean unique = catalog.ranked(id).isUnique();
            if (!(unique && belowBoss)) {
                all.add(Candidate.token(id, unique ? "enemy, boss only" : "enemy"));
            }
        }
        return all;
    }

    /** What the tokens already typed leave pending. */
    private static Position positionAfter(String typed, EnemyCatalog catalog) {
        Position position = new Position(true, Optional.empty(), ShapeState.NONE);
        for (String token : typed.split(" ")) {
            if (token.isBlank()) {
                continue;
            }
            boolean leading = position.first();
            position = position.withFirst(false);
            if (leading && WaveScript.isSpacingToken(token)) {
                continue;
            }
            if (WaveScript.isRankToken(token)) {
                position = position.withRank(Optional.of(WaveScript.rankFor(token)));
            } else if (WaveScript.isShapeToken(token)) {
                position = position.withShape(
                        WaveScript.takesMemberCount(token) ? ShapeState.AWAITING_COUNT : ShapeState.READY);
            } else if (token.equals(WaveScript.EMPTY_TOKEN) || catalog.contains(token)) {
                position = position.withRank(Optional.empty()).withShape(ShapeState.NONE);
            } else if (position.shape() == ShapeState.AWAITING_COUNT && COUNT.matcher(token).matches()) {
                position = position.withShape(ShapeState.READY);
            }
        }
        return position;
    }

    /** Where the slot's shape keyword stands: none yet, waiting for its member count, or done. */
    private enum ShapeState {
        NONE, AWAITING_COUNT, READY
    }

    private record Position(boolean first, Optional<Rank> rank, ShapeState shape) {

        Position withFirst(boolean first) {
            return new Position(first, this.rank, this.shape);
        }

        Position withRank(Optional<Rank> rank) {
            return new Position(this.first, rank, this.shape);
        }

        Position withShape(ShapeState shape) {
            return new Position(this.first, this.rank, shape);
        }
    }
}
