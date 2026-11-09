package td.wave;

import td.enemy.Affix;
import td.enemy.AffixContext;
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
 * head   := spacing? rank? affix* ':'
 * item   := count* rank? count* (shape count*)? affix* (enemy | 'e')
 * </pre>
 * The head holds the wave's defaults, and is the only place a spacing may stand; a script without one takes the
 * fallback rank. A token ending in {@code :} reads as the word and then the {@code :}, and the first {@code :} ends
 * the head. A slot's rank replaces the head's, and its affixes add to the head's. The last count before the shape
 * (or enemy) repeats the slot; the last one after a shape is its member count. A count left over at the end of the
 * script repeats nothing.
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
        if (!at.holds(WaveScript.HEAD_END) || !at.upTo(WaveScript.HEAD_END).stream().allMatch(WaveScriptGrammar::isHeadWord)) {
            return new Parsed<>(new Head(SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT, this.fallbackRank, List.of(), false),
                    at);
        }
        Parsed<Float> spacing = spacing(at);
        Parsed<Optional<Rank>> rank = rank(spacing.rest());
        Parsed<List<Affix>> affixes = affixes(rank.rest(), List.of());
        Cursor end = affixes.rest();
        if (!end.peek().equals(WaveScript.HEAD_END)) {
            throw end.fail("A head holds only w<n>, a rank and affixes, in that order, and ends with '"
                    + WaveScript.HEAD_END + "'");
        }
        return new Parsed<>(new Head(spacing.value(), rank.value().orElse(this.fallbackRank), affixes.value(), true),
                end.next());
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
        Cursor afterShape = memberCount.rest();
        if (shape.value().isPresent() && isRank(afterShape)) {
            throw afterShape.fail("Rank token '" + afterShape.peek() + "' follows spawn-type token '"
                    + shape.value().get().token() + "' - a rank token must come before the spawn-type token it shapes, not after");
        }
        if (shape.value().isPresent() && isShape(afterShape)) {
            throw afterShape.fail("Spawn-type token '" + afterShape.peek() + "' follows '" + shape.value().get().token()
                    + "' with no enemy id between them");
        }
        Parsed<List<Affix>> affixes = affixes(afterShape, head.affixes());
        Cursor target = affixes.rest();
        if (!affixes.value().isEmpty() && (isRank(target) || isShape(target))) {
            throw target.fail("'" + target.peek() + "' follows an affix - affixes go right before the enemy id,"
                    + " after its rank and spawn type");
        }
        int slotRepeat = countAfterRank.value().orElse(leadingCount.value().orElse(1));
        if (slotRepeat > WaveScript.MAX_REPEAT) {
            throw target.fail("Repeat count " + slotRepeat + " is more than the most a slot may repeat, "
                    + WaveScript.MAX_REPEAT);
        }
        return slot(target, head, new Words(slotRepeat, rank.value(),
                shape.value().map(keyword -> new ShapeWords(keyword, memberCount.value())), affixes.value()));
    }

    /**
     * The affix words from here on, each checked against {@code held} (the head's) and the ones read before it; the
     * result holds only the ones read here.
     */
    private static Parsed<List<Affix>> affixes(Cursor at, List<Affix> held) {
        List<Affix> read = new ArrayList<>();
        Cursor here = at;
        while (!here.atEnd() && Affix.of(here.peek()).isPresent()) {
            Affix affix = Affix.of(here.peek()).orElseThrow();
            List<Affix> carried = Stream.concat(held.stream(), read.stream()).toList();
            if (carried.contains(affix)) {
                throw here.fail("Affix '" + affix.word() + "' is on this slot already");
            }
            if (carried.size() >= WaveScript.MAX_AFFIXES) {
                throw here.fail("A slot carries at most " + WaveScript.MAX_AFFIXES + " affixes, the head's included");
            }
            for (Affix other : carried) {
                if (affix.excludes(other)) {
                    throw here.fail("Affix '" + affix.word() + "' cannot go with '" + other.word() + "'");
                }
            }
            read.add(affix);
            here = here.next();
        }
        return new Parsed<>(List.copyOf(read), here);
    }

    private Parsed<List<WaveSlot>> slot(Cursor at, Head head, Words words) {
        if (at.atEnd()) {
            if (!words.affixes().isEmpty()) {
                throw at.fail("Wave ends with affix '" + words.affixes().getLast().word() + "' and no enemy id");
            }
            if (words.shape().isPresent()) {
                throw at.fail("Wave ends with spawn-type token '" + words.shape().get().keyword().token()
                        + "' and no enemy id");
            }
            if (words.rank().isPresent()) {
                throw at.fail("Wave ends with rank token '" + words.rank().get() + "' and no enemy id");
            }
            return new Parsed<>(List.of(), at);
        }
        String token = at.peek();
        if (token.equals(WaveScript.HEAD_END)) {
            throw at.fail(head.written() ? "A script has one head, so one '" + WaveScript.HEAD_END + "'"
                    : "A head opens the script: move it before the first slot");
        }
        if (WaveScript.isSpacingToken(token)) {
            throw at.fail("Spacing token '" + token + "' belongs in the head: end the head with '" + WaveScript.HEAD_END
                    + "', e.g. '" + token + WaveScript.HEAD_END + " 3 walker'");
        }
        if (token.equals(WaveScript.EMPTY_TOKEN)) {
            if (words.shape().isPresent()) {
                throw at.fail("The spacer 'e' cannot follow spawn-type token '" + words.shape().get().keyword().token()
                        + "'");
            }
            if (words.rank().isPresent()) {
                throw at.fail("The spacer 'e' cannot follow rank token '" + words.rank().get() + "'");
            }
            if (!words.affixes().isEmpty()) {
                throw at.fail("The spacer 'e' cannot follow affix '" + words.affixes().getLast().word() + "'");
            }
            return new Parsed<>(repeated(new EmptySlot(), words.repeat()), at.next());
        }
        if (this.catalog.contains(token)) {
            return new Parsed<>(repeated(enemySlot(at, head, words), words.repeat()), at.next());
        }
        throw at.fail("Unrecognized wave token '" + token
                + "': expected a spawn-type, rank or affix keyword (" + WaveScript.RESERVED_TOKENS
                + "), a repeat count, or an id registered in the enemy catalog (" + this.catalog.ids() + ")");
    }

    private WaveSlot enemySlot(Cursor at, Head head, Words words) {
        RankedEnemy enemy = this.catalog.ranked(at.peek());
        if (enemy.isUnique() && words.rank().isPresent() && words.rank().get() != Rank.BOSS) {
            throw at.fail("'" + at.peek() + "' is unique and comes only at Boss rank, not " + words.rank().get());
        }
        List<Affix> affixes = Stream.concat(head.affixes().stream(), words.affixes().stream()).toList();
        if (enemy.isUnique() && !affixes.isEmpty()) {
            throw at.fail("'" + at.peek() + "' is unique and takes no affix");
        }
        Rank requested = words.rank().orElse(head.rank());
        Rank rank = enemy.effectiveRank(requested);
        EnemyDefinition definition = enemy.definitionFor(requested);
        for (Affix affix : affixes) {
            Optional<String> problem = affix.problemWith(definition);
            if (problem.isPresent()) {
                throw at.fail("Affix '" + affix.word() + "' cannot go on '" + at.peek() + "': " + problem.get());
            }
        }
        SpawnShape spawnShape = words.shape().map(shape -> shapeFor(at, shape.keyword(), shape.members()))
                .orElse(SpawnShape.normal());
        EnemyDefinition affixed = affixes.isEmpty() ? definition : definition.withAffixes(affixes,
                new AffixContext(rank, spawnShape.healthMultiplier() / definition.healthDivisor()));
        return new EnemySlot(affixed, rank, spawnShape, enemy.isUnique());
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

    /** Whether {@code token} may stand in a head: a spacing, a rank or an affix. */
    private static boolean isHeadWord(String token) {
        return WaveScript.isSpacingToken(token) || WaveScript.isRankToken(token) || Affix.of(token).isPresent();
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

    /** What a script's head sets for every slot; its affixes add to a slot's own. {@code written}: not the defaults. */
    private record Head(float ticksPerSlot, Rank rank, List<Affix> affixes, boolean written) {
    }

    /** What an item says before the token it spawns. */
    private record Words(int repeat, Optional<Rank> rank, Optional<ShapeWords> shape, List<Affix> affixes) {
    }

    /** A spawn-type keyword and the member count written after it, if any. */
    private record ShapeWords(ShapeKeyword keyword, OptionalInt members) {
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

        /** The tokens from here to the first {@code token}, which it must hold. */
        List<String> upTo(String token) {
            List<String> rest = this.tokens.subList(this.index, this.tokens.size());
            return rest.subList(0, rest.indexOf(token));
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
