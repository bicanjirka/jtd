package td.wave;

import td.enemy.Affix;
import td.enemy.AffixContext;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.enemy.SpawnParameters;
import td.util.GameStartupException;

import java.io.Serial;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The wave script as a grammar of rules over an immutable {@link Cursor}; each rule returns what it
 * read and the cursor after it, so no rule remembers anything between calls.
 * <pre>
 * script := head? item*
 * head   := spacing? rank? affix* ':'
 * item   := count? rank? (shape count?)? affix* (enemy | 'e')
 * </pre>
 * The head holds the wave's defaults, and is the only place a spacing may stand; a script without one takes the
 * fallback rank. A token ending in {@code :} reads as the word and then the {@code :}. A script is headed only when
 * every token before its first {@code :} is a head word. A slot's rank replaces the head's, and its affixes add to the
 * head's. The count before a slot repeats it; the one after a shape is its member count.
 * <p>
 * {@link #script} reads a whole script; {@link #next} reads the start of one and says what the same rules would
 * accept after it.
 */
final class WaveScriptGrammar {

    private final Rank fallbackRank;
    private final EnemyCatalog catalog;

    WaveScriptGrammar(Rank fallbackRank, EnemyCatalog catalog) {
        this.fallbackRank = fallbackRank;
        this.catalog = catalog;
    }

    WaveContent script(String[] tokens) throws ScriptFault {
        Parsed<Head> head = head(Cursor.over(tokens));
        List<WaveSlot> slots = new ArrayList<>();
        Cursor at = head.rest();
        while (!at.atEnd()) {
            Parsed<Words> words = words(at, head.value());
            Parsed<List<WaveSlot>> slot = slot(words.rest(), head.value(), words.value());
            slots.addAll(slot.value());
            at = slot.rest();
        }
        return new WaveContent(slots, head.value().ticksPerSlot());
    }

    /** What may follow {@code typed}, the start of a script; fails where the start already does. */
    NextWords next(String[] typed) throws ScriptFault {
        Cursor start = Cursor.over(typed);
        if (start.atEnd()) {
            return new NextWords(EnumSet.of(NextWords.Kind.SPACING), List.of()).or(nextInSlot(defaultHead(),
                    Words.none()));
        }
        Optional<NextWords> openHead = openHead(start);
        if (openHead.isPresent()) {
            return openHead.get();
        }
        Parsed<Head> head = head(start);
        Cursor at = head.rest();
        while (true) {
            Parsed<Words> words = words(at, head.value());
            if (words.rest().atEnd()) {
                return nextInSlot(head.value(), words.value());
            }
            at = slot(words.rest(), head.value(), words.value()).rest();
        }
    }

    /**
     * When every token typed is a head word and no {@code :} has come yet, they may still become the head or the
     * first slot: what each would take next. Empty when the start can't be a head.
     */
    private Optional<NextWords> openHead(Cursor start) throws ScriptFault {
        if (start.holds(WaveScript.HEAD_END) || !start.upToEnd().stream().allMatch(WaveScriptGrammar::isHeadWord)) {
            return Optional.empty();
        }
        Parsed<Float> spacing = spacing(start);
        Parsed<Optional<Rank>> rank = rank(spacing.rest());
        Parsed<List<Affix>> affixes = affixes(rank.rest(), List.of());
        if (!affixes.rest().atEnd()) {
            return Optional.empty();
        }
        Set<NextWords.Kind> kinds = EnumSet.of(NextWords.Kind.HEAD_END);
        if (rank.value().isEmpty() && affixes.value().isEmpty()) {
            kinds.add(NextWords.Kind.RANK);
        }
        NextWords asHead = new NextWords(kinds, allowedAffixes(affixes.value()));
        if (!spacing.rest().equals(start)) {
            return Optional.of(asHead);
        }
        Head none = defaultHead();
        return Optional.of(asHead.or(nextInSlot(none, new Words(OptionalInt.empty(), rank.value(), Optional.empty(),
                affixes.value()))));
    }

    private Head defaultHead() {
        return new Head(SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT, this.fallbackRank, List.of(), false);
    }

    private Parsed<Head> head(Cursor at) throws ScriptFault {
        if (!at.holds(WaveScript.HEAD_END) || !at.upTo(WaveScript.HEAD_END).stream().allMatch(WaveScriptGrammar::isHeadWord)) {
            return new Parsed<>(defaultHead(), at);
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

    private static Parsed<Float> spacing(Cursor at) throws ScriptFault {
        if (at.atEnd() || !WaveScript.isSpacingToken(at.peek())) {
            return new Parsed<>(SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT, at);
        }
        float ticks = WaveScript.spacingTicks(at.peek());
        if (ticks <= 0f) {
            throw at.fail("Spacing token '" + at.peek() + "' must be greater than zero");
        }
        return new Parsed<>(ticks, at.next());
    }

    /** What an item says before the token it spawns; it stops at that token or at the end. */
    private static Parsed<Words> words(Cursor at, Head head) throws ScriptFault {
        Parsed<OptionalInt> repeat = count(at);
        if (repeat.value().isPresent() && repeat.value().getAsInt() > WaveScript.MAX_REPEAT) {
            throw at.fail("Repeat count " + repeat.value().getAsInt() + " is more than the most a slot may repeat, "
                    + WaveScript.MAX_REPEAT);
        }
        Parsed<Optional<Rank>> rank = rank(repeat.rest());
        if (rank.value().isPresent() && isRank(rank.rest())) {
            throw rank.rest().fail("Rank token '" + rank.rest().peek()
                    + "' follows another rank token with no id between them");
        }
        Parsed<Optional<ShapeKeyword>> shape = shape(rank.rest());
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
        return new Parsed<>(new Words(repeat.value(), rank.value(),
                shape.value().map(keyword -> new ShapeWords(keyword, memberCount.value())), affixes.value()), target);
    }

    /**
     * The affix words from here on, each checked against {@code held} (the head's) and the ones read before it; the
     * result holds only the ones read here.
     */
    private static Parsed<List<Affix>> affixes(Cursor at, List<Affix> held) throws ScriptFault {
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

    /** The affixes {@link #affixes} would still accept on a slot that carries {@code carried}. */
    private static List<Affix> allowedAffixes(List<Affix> carried) {
        if (carried.size() >= WaveScript.MAX_AFFIXES) {
            return List.of();
        }
        return Stream.of(Affix.values())
                .filter(affix -> !carried.contains(affix) && carried.stream().noneMatch(affix::excludes))
                .toList();
    }

    private Parsed<List<WaveSlot>> slot(Cursor at, Head head, Words words) throws ScriptFault {
        if (at.atEnd()) {
            throw endsEarly(at, words);
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
        if (asCount(token).isPresent()) {
            throw at.fail("A count goes first in its slot, to repeat it, or right after a spawn type, for its members:"
                    + " e.g. '2 elite swarm 4 walker'");
        }
        int repeat = words.repeat().orElse(1);
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
            return new Parsed<>(repeated(new EmptySlot(), repeat), at.next());
        }
        if (this.catalog.contains(token)) {
            return new Parsed<>(repeated(enemySlot(at, head, words), repeat), at.next());
        }
        throw at.fail("Unrecognized wave token '" + token
                + "': expected a spawn-type, rank or affix keyword (" + WaveScript.RESERVED_TOKENS
                + "), a repeat count, or an id registered in the enemy catalog (" + this.catalog.ids() + ")");
    }

    /** A script that stops after {@code words}, which name no enemy. */
    private static ScriptFault endsEarly(Cursor at, Words words) {
        if (!words.affixes().isEmpty()) {
            return at.fail("Wave ends with affix '" + words.affixes().getLast().word() + "' and no enemy id");
        }
        if (words.shape().isPresent()) {
            return at.fail("Wave ends with spawn-type token '" + words.shape().get().keyword().token()
                    + "' and no enemy id");
        }
        if (words.rank().isPresent()) {
            return at.fail("Wave ends with rank token '" + words.rank().get() + "' and no enemy id");
        }
        return at.fail("Wave ends with count " + words.repeat().orElseThrow() + " and nothing for it to repeat");
    }

    /** What may come next in a slot after {@code words}, under {@code head}. */
    private NextWords nextInSlot(Head head, Words words) {
        Optional<ShapeWords> shape = words.shape();
        if (shape.isPresent() && shape.get().keyword().takesMemberCount() && shape.get().members().isEmpty()) {
            return new NextWords(EnumSet.of(NextWords.Kind.MEMBER_COUNT), List.of());
        }
        List<Affix> carried = Stream.concat(head.affixes().stream(), words.affixes().stream()).toList();
        Set<NextWords.Kind> kinds = EnumSet.of(NextWords.Kind.ENEMY);
        boolean bare = words.affixes().isEmpty() && shape.isEmpty();
        if (bare && words.rank().isEmpty()) {
            kinds.add(NextWords.Kind.RANK);
            kinds.add(NextWords.Kind.SPACER);
        }
        if (bare) {
            kinds.add(NextWords.Kind.SHAPE);
        }
        if (carried.isEmpty() && words.rank().filter(rank -> rank != Rank.BOSS).isEmpty()) {
            kinds.add(NextWords.Kind.UNIQUE_ENEMY);
        }
        return new NextWords(kinds, allowedAffixes(carried));
    }

    private WaveSlot enemySlot(Cursor at, Head head, Words words) throws ScriptFault {
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
        SpawnShape spawnShape = words.shape().isPresent()
                ? shapeFor(at, words.shape().get().keyword(), words.shape().get().members())
                : SpawnShape.normal();
        EnemyDefinition affixed = affixes.isEmpty() ? definition : definition.withAffixes(affixes,
                new AffixContext(rank, spawnShape.healthMultiplier() / definition.healthDivisor()));
        return new EnemySlot(affixed, rank, spawnShape, enemy.isUnique());
    }

    private static List<WaveSlot> repeated(WaveSlot slot, int times) {
        return Stream.generate(() -> slot).limit(Math.max(times, 0)).toList();
    }

    private static Parsed<Optional<Rank>> rank(Cursor at) {
        Optional<Rank> rank = at.atEnd() ? Optional.empty() : RankKeyword.of(at.peek()).map(RankKeyword::rank);
        return new Parsed<>(rank, rank.isPresent() ? at.next() : at);
    }

    private static Parsed<Optional<ShapeKeyword>> shape(Cursor at) {
        Optional<ShapeKeyword> keyword = at.atEnd() ? Optional.empty() : ShapeKeyword.of(at.peek());
        return new Parsed<>(keyword, keyword.isPresent() ? at.next() : at);
    }

    private static Parsed<OptionalInt> count(Cursor at) {
        OptionalInt count = at.atEnd() ? OptionalInt.empty() : asCount(at.peek());
        return new Parsed<>(count, count.isPresent() ? at.next() : at);
    }

    private static OptionalInt asCount(String token) {
        if (!WaveScript.isCountToken(token)) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(token));
        } catch (NumberFormatException ex) {
            // Too many digits for an int: as a count it is over any limit.
            return OptionalInt.of(Integer.MAX_VALUE);
        }
    }

    /** Whether {@code token} may stand in a head: a spacing, a rank or an affix. */
    private static boolean isHeadWord(String token) {
        return WaveScript.isSpacingToken(token) || RankKeyword.of(token).isPresent() || Affix.of(token).isPresent();
    }

    private static boolean isRank(Cursor at) {
        return !at.atEnd() && RankKeyword.of(at.peek()).isPresent();
    }

    private static boolean isShape(Cursor at) {
        return !at.atEnd() && ShapeKeyword.of(at.peek()).isPresent();
    }

    private static SpawnShape shapeFor(Cursor at, ShapeKeyword keyword, OptionalInt memberCount) throws ScriptFault {
        if (!keyword.takesMemberCount()) {
            if (memberCount.isPresent()) {
                throw at.fail("Spawn type '" + keyword.token() + "' has a fixed member count and cannot take one;"
                        + " to repeat it, put the count before the keyword, e.g. '3 " + keyword.token() + " walker'");
            }
            return keyword.shape(1);
        }
        if (memberCount.isEmpty()) {
            throw at.fail("Spawn type '" + keyword.token() + "' needs a member count, e.g. '" + keyword.token()
                    + " 3 walker'");
        }
        return keyword.shape(memberCount.getAsInt());
    }

    /**
     * What the rules accept next: the kinds of token, and which affixes. {@link Kind#MEMBER_COUNT} stands alone, since
     * a shape that needs one takes nothing else first.
     */
    record NextWords(Set<Kind> kinds, List<Affix> affixes) {

        NextWords {
            kinds = Set.copyOf(kinds);
            affixes = List.copyOf(affixes);
        }

        enum Kind {
            SPACING, HEAD_END, RANK, SPACER, SHAPE, MEMBER_COUNT, ENEMY, UNIQUE_ENEMY
        }

        boolean accepts(Kind kind) {
            return this.kinds.contains(kind);
        }

        /** What either accepts; affixes in their declared order. */
        NextWords or(NextWords other) {
            Set<Kind> both = EnumSet.noneOf(Kind.class);
            both.addAll(this.kinds);
            both.addAll(other.kinds);
            return new NextWords(both, Stream.of(Affix.values())
                    .filter(affix -> this.affixes.contains(affix) || other.affixes.contains(affix)).toList());
        }
    }

    /** A script that breaks a rule, at the token {@link Cursor#fail} marks. */
    static final class ScriptFault extends Exception {
        @Serial
        private static final long serialVersionUID = 1L;

        ScriptFault(String message) {
            super(message);
        }

        GameStartupException asStartupFailure() {
            return new GameStartupException(this.getMessage(), this);
        }
    }

    private record Parsed<T>(T value, Cursor rest) {
    }

    /** What a script's head sets for every slot; its affixes add to a slot's own. {@code written}: not the defaults. */
    private record Head(float ticksPerSlot, Rank rank, List<Affix> affixes, boolean written) {
    }

    /** What an item says before the token it spawns. */
    private record Words(OptionalInt repeat, Optional<Rank> rank, Optional<ShapeWords> shape, List<Affix> affixes) {

        static Words none() {
            return new Words(OptionalInt.empty(), Optional.empty(), Optional.empty(), List.of());
        }
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
            return upToEnd().contains(token);
        }

        /** The tokens from here to the first {@code token}, which it must hold. */
        List<String> upTo(String token) {
            List<String> rest = upToEnd();
            return rest.subList(0, rest.indexOf(token));
        }

        List<String> upToEnd() {
            return this.tokens.subList(this.index, this.tokens.size());
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
        ScriptFault fail(String message) {
            String before = String.join(" ", this.tokens.subList(0, Math.min(this.index, this.tokens.size())));
            if (atEnd()) {
                return new ScriptFault("Wave script ends early, in \"" + before + " »«\": " + message);
            }
            String after = String.join(" ", this.tokens.subList(this.index + 1, this.tokens.size()));
            return new ScriptFault("Wave script token " + (this.index + 1) + " '" + peek() + "', in \""
                    + before + " »" + peek() + "« " + after + "\": " + message);
        }
    }
}
