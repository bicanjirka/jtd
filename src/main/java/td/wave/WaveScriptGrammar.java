package td.wave;

import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.enemy.SpawnParameters;
import td.util.GameStartupException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * The wave script as a grammar of rules over an immutable {@link Cursor}; each rule returns what it
 * read and the cursor after it, so no rule remembers anything between calls.
 * <pre>
 * script := head? item*
 * head   := spacing? rank? ':'
 * item   := count* rank? count* (shape count*)? (enemy | 'e')
 * </pre>
 * The head holds the wave's defaults, and is the only place a spacing may stand; a script without one takes the
 * fallback rank. A token ending in {@code :} reads as the word and then the {@code :}, and the first {@code :} ends
 * the head. The last count before the shape (or enemy) repeats the slot; the last one after a shape is its member
 * count. A count left over at the end of the script repeats nothing.
 */
final class WaveScriptGrammar {

    private final Rank fallbackRank;
    private final EnemyCatalog catalog;

    WaveScriptGrammar(Rank fallbackRank, EnemyCatalog catalog) {
        this.fallbackRank = fallbackRank;
        this.catalog = catalog;
    }

    WaveContent script(String[] tokens) {
        Parsed<Head> head = head(Cursor.over(tokens));
        List<WaveSlot> slots = new ArrayList<>();
        Cursor at = head.rest();
        while (!at.atEnd()) {
            Parsed<List<WaveSlot>> item = item(at, head.value());
            slots.addAll(item.value());
            at = item.rest();
        }
        return new WaveContent(slots, head.value().ticksPerSlot());
    }

    private Parsed<Head> head(Cursor at) {
        if (!at.holds(WaveScript.HEAD_END)) {
            return new Parsed<>(new Head(SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT, this.fallbackRank), at);
        }
        Parsed<Float> spacing = spacing(at);
        Parsed<Optional<Rank>> rank = rank(spacing.rest());
        Cursor end = rank.rest();
        if (!end.peek().equals(WaveScript.HEAD_END)) {
            throw end.fail("A head holds only w<n> and a rank, in that order, and ends with '" + WaveScript.HEAD_END + "'");
        }
        return new Parsed<>(new Head(spacing.value(), rank.value().orElse(this.fallbackRank)), end.next());
    }

    private Parsed<Float> spacing(Cursor at) {
        if (!WaveScript.isSpacingToken(at.peek())) {
            return new Parsed<>(SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT, at);
        }
        float ticks = WaveScript.spacingTicks(at.peek());
        if (ticks <= 0f) {
            throw at.fail("Spacing token '" + at.peek() + "' must be greater than zero");
        }
        return new Parsed<>(ticks, at.next());
    }

    private Parsed<List<WaveSlot>> item(Cursor at, Head head) {
        Parsed<OptionalInt> leadingCount = count(at);
        Parsed<Optional<Rank>> rank = rank(leadingCount.rest());
        Parsed<OptionalInt> countAfterRank = count(rank.rest());
        if (rank.value().isPresent() && isRank(countAfterRank.rest())) {
            throw countAfterRank.rest().fail("Rank token '" + countAfterRank.rest().peek()
                    + "' follows another rank token with no id between them");
        }
        Parsed<Optional<ShapeKeyword>> shape = shape(countAfterRank.rest());
        Parsed<OptionalInt> memberCount = shape.value().isPresent()
                ? count(shape.rest())
                : new Parsed<>(OptionalInt.empty(), shape.rest());
        Cursor target = memberCount.rest();
        if (shape.value().isPresent() && isRank(target)) {
            throw target.fail("Rank token '" + target.peek() + "' follows spawn-type token '"
                    + shape.value().get().token() + "' - a rank token must come before the spawn-type token it shapes, not after");
        }
        if (shape.value().isPresent() && isShape(target)) {
            throw target.fail("Spawn-type token '" + target.peek() + "' follows '" + shape.value().get().token()
                    + "' with no enemy id between them");
        }
        int slotRepeat = countAfterRank.value().orElse(leadingCount.value().orElse(1));
        if (slotRepeat > WaveScript.MAX_REPEAT) {
            throw target.fail("Repeat count " + slotRepeat + " is more than the most a slot may repeat, "
                    + WaveScript.MAX_REPEAT);
        }
        return slot(target, head, slotRepeat, rank.value(), shape.value(), memberCount.value());
    }

    private Parsed<List<WaveSlot>> slot(Cursor at, Head head, int slotRepeat, Optional<Rank> rank,
                                        Optional<ShapeKeyword> shape, OptionalInt memberCount) {
        if (at.atEnd()) {
            if (shape.isPresent()) {
                throw at.fail("Wave ends with spawn-type token '" + shape.get().token() + "' and no enemy id");
            }
            if (rank.isPresent()) {
                throw at.fail("Wave ends with rank token '" + rank.get() + "' and no enemy id");
            }
            return new Parsed<>(List.of(), at);
        }
        String token = at.peek();
        if (token.equals(WaveScript.HEAD_END)) {
            throw at.fail("A script has one head, so one '" + WaveScript.HEAD_END + "'");
        }
        if (WaveScript.isSpacingToken(token)) {
            throw at.fail("Spacing token '" + token + "' belongs in the head: end the head with '" + WaveScript.HEAD_END
                    + "', e.g. '" + token + WaveScript.HEAD_END + " 3 walker'");
        }
        if (token.equals(WaveScript.EMPTY_TOKEN)) {
            if (shape.isPresent()) {
                throw at.fail("The spacer 'e' cannot follow spawn-type token '" + shape.get().token() + "'");
            }
            if (rank.isPresent()) {
                throw at.fail("The spacer 'e' cannot follow rank token '" + rank.get() + "'");
            }
            return new Parsed<>(repeated(new EmptySlot(), slotRepeat), at.next());
        }
        if (this.catalog.contains(token)) {
            return new Parsed<>(repeated(enemySlot(at, head, rank, shape, memberCount), slotRepeat), at.next());
        }
        throw at.fail("Unrecognized wave token '" + token
                + "': expected a spawn-type or rank keyword (" + WaveScript.RESERVED_TOKENS
                + "), a repeat count, or an id registered in the enemy catalog (" + this.catalog.ids() + ")");
    }

    private WaveSlot enemySlot(Cursor at, Head head, Optional<Rank> rank, Optional<ShapeKeyword> shape,
                               OptionalInt memberCount) {
        RankedEnemy enemy = this.catalog.ranked(at.peek());
        if (enemy.isUnique() && rank.isPresent() && rank.get() != Rank.BOSS) {
            throw at.fail("'" + at.peek() + "' is unique and comes only at Boss rank, not " + rank.get());
        }
        Rank requested = rank.orElse(head.rank());
        EnemyDefinition definition = enemy.definitionFor(requested);
        SpawnShape spawnShape = shape.map(keyword -> shapeFor(at, keyword, memberCount)).orElse(SpawnShape.normal());
        return new EnemySlot(definition, enemy.effectiveRank(requested), spawnShape, enemy.isUnique());
    }

    private static List<WaveSlot> repeated(WaveSlot slot, int times) {
        return Stream.generate(() -> slot).limit(Math.max(times, 0)).toList();
    }

    private static Parsed<Optional<Rank>> rank(Cursor at) {
        return isRank(at)
                ? new Parsed<>(Optional.of(WaveScript.rankFor(at.peek())), at.next())
                : new Parsed<>(Optional.empty(), at);
    }

    private static Parsed<Optional<ShapeKeyword>> shape(Cursor at) {
        Optional<ShapeKeyword> keyword = at.atEnd() ? Optional.empty() : ShapeKeyword.of(at.peek());
        return new Parsed<>(keyword, keyword.isPresent() ? at.next() : at);
    }

    /** Consecutive counts collapse to the last one. */
    private Parsed<OptionalInt> count(Cursor at) {
        OptionalInt here = at.atEnd() || this.catalog.contains(at.peek()) ? OptionalInt.empty() : asCount(at.peek());
        if (here.isEmpty()) {
            return new Parsed<>(OptionalInt.empty(), at);
        }
        Parsed<OptionalInt> later = count(at.next());
        return later.value().isPresent() ? later : new Parsed<>(here, later.rest());
    }

    private static OptionalInt asCount(String token) {
        try {
            return OptionalInt.of(Integer.parseInt(token));
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    private static boolean isRank(Cursor at) {
        return !at.atEnd() && WaveScript.isRankToken(at.peek());
    }

    private static boolean isShape(Cursor at) {
        return !at.atEnd() && ShapeKeyword.of(at.peek()).isPresent();
    }

    private static SpawnShape shapeFor(Cursor at, ShapeKeyword keyword, OptionalInt memberCount) {
        if (!keyword.takesMemberCount()) {
            if (memberCount.isPresent()) {
                throw at.fail("Spawn type '" + keyword.token() + "' has a fixed member count and cannot take one;"
                        + " to repeat it, put the count before the keyword, e.g. '3 " + keyword.token() + " walker'");
            }
            return keyword.shape(1);
        }
        return keyword.shape(memberCount.orElseThrow(() -> at.fail("Spawn type '" + keyword.token()
                + "' needs a member count, e.g. '" + keyword.token() + " 3 walker'")));
    }

    private record Parsed<T>(T value, Cursor rest) {
    }

    /** What a script's head sets for every slot. */
    private record Head(float ticksPerSlot, Rank rank) {
    }

    /** A position in the non-blank tokens, with a trailing ':' split off as a token of its own; moving it makes a new one. */
    private record Cursor(List<String> tokens, int index) {

        static Cursor over(String[] tokens) {
            return new Cursor(Stream.of(tokens).filter(token -> !token.isBlank()).flatMap(Cursor::splitHeadEnd)
                    .toList(), 0);
        }

        private static Stream<String> splitHeadEnd(String token) {
            return token.length() > WaveScript.HEAD_END.length() && token.endsWith(WaveScript.HEAD_END)
                    ? Stream.of(token.substring(0, token.length() - WaveScript.HEAD_END.length()), WaveScript.HEAD_END)
                    : Stream.of(token);
        }

        /** Whether {@code token} stands here or anywhere after. */
        boolean holds(String token) {
            return this.tokens.subList(this.index, this.tokens.size()).contains(token);
        }

        boolean atEnd() {
            return this.index >= this.tokens.size();
        }

        String peek() {
            return this.tokens.get(this.index);
        }

        Cursor next() {
            return new Cursor(this.tokens, this.index + 1);
        }

        /** Names this position and shows the script with it marked, so the fix is a look away. */
        GameStartupException fail(String message) {
            String before = String.join(" ", this.tokens.subList(0, Math.min(this.index, this.tokens.size())));
            if (atEnd()) {
                return new GameStartupException("Wave script ends early, in \"" + before + " »«\": " + message);
            }
            String after = String.join(" ", this.tokens.subList(this.index + 1, this.tokens.size()));
            return new GameStartupException("Wave script token " + (this.index + 1) + " '" + peek() + "', in \""
                    + before + " »" + peek() + "« " + after + "\": " + message);
        }
    }
}
