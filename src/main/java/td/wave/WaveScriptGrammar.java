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
 * The wave script as a recursive-descent grammar over an immutable {@link Cursor}; each rule returns what it
 * read and the cursor after it, so no rule remembers anything between calls.
 * <pre>
 * script := spacing? item*
 * item   := count* rank? count* (shape count*)? (enemy | 'e')
 * </pre>
 * The last count before the shape (or enemy) repeats the slot; the last one after a shape is its member count. A
 * count left over at the end of the script repeats nothing.
 */
final class WaveScriptGrammar {

    private final Rank defaultRank;
    private final EnemyCatalog catalog;

    WaveScriptGrammar(Rank defaultRank, EnemyCatalog catalog) {
        this.defaultRank = defaultRank;
        this.catalog = catalog;
    }

    WaveContent script(String[] tokens) {
        Parsed<Float> spacing = spacing(Cursor.over(tokens));
        List<WaveSlot> slots = new ArrayList<>();
        Cursor at = spacing.rest();
        while (!at.atEnd()) {
            Parsed<List<WaveSlot>> item = item(at);
            slots.addAll(item.value());
            at = item.rest();
        }
        return new WaveContent(slots, spacing.value());
    }

    private Parsed<Float> spacing(Cursor at) {
        if (at.atEnd() || !WaveScript.isSpacingToken(at.peek())) {
            return new Parsed<>(SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT, at);
        }
        float ticks = WaveScript.spacingTicks(at.peek());
        if (ticks <= 0f) {
            throw at.fail("Spacing token '" + at.peek() + "' must be greater than zero");
        }
        return new Parsed<>(ticks, at.next());
    }

    private Parsed<List<WaveSlot>> item(Cursor at) {
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
        return slot(target, slotRepeat, rank.value(), shape.value(), memberCount.value());
    }

    private Parsed<List<WaveSlot>> slot(Cursor at, int slotRepeat, Optional<Rank> rank, Optional<ShapeKeyword> shape,
                                        OptionalInt memberCount) {
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
        if (WaveScript.isSpacingToken(token)) {
            throw at.fail("Spacing token '" + token + "' must be the first token of the wave");
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
            return new Parsed<>(repeated(enemySlot(at, rank, shape, memberCount), slotRepeat), at.next());
        }
        throw at.fail("Unrecognized wave token '" + token
                + "': expected a spawn-type or rank keyword (" + WaveScript.RESERVED_TOKENS
                + "), a repeat count, or an id registered in the enemy catalog (" + this.catalog.ids() + ")");
    }

    private WaveSlot enemySlot(Cursor at, Optional<Rank> rank, Optional<ShapeKeyword> shape, OptionalInt memberCount) {
        RankedEnemy enemy = this.catalog.ranked(at.peek());
        if (enemy.isUnique() && rank.isPresent() && rank.get() != Rank.BOSS) {
            throw at.fail("'" + at.peek() + "' is unique and comes only at Boss rank, not " + rank.get());
        }
        Rank requested = rank.orElse(this.defaultRank);
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
                        + " to repeat it, put the count before the keyword, e.g. '3 " + keyword.token() + " c'");
            }
            return keyword.shape(1);
        }
        return keyword.shape(memberCount.orElseThrow(() -> at.fail("Spawn type '" + keyword.token()
                + "' needs a member count, e.g. '" + keyword.token() + " 3 c'")));
    }

    private record Parsed<T>(T value, Cursor rest) {
    }

    /** A position in the non-blank tokens; moving it makes a new one. */
    private record Cursor(List<String> tokens, int index) {

        static Cursor over(String[] tokens) {
            return new Cursor(Stream.of(tokens).filter(token -> !token.isBlank()).toList(), 0);
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
