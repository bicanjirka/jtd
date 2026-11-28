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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The wave script as a grammar of rules over an immutable {@link Cursor}; each rule returns what it
 * read and the cursor after it. The brackets still open are the one thing carried from rule to rule, in
 * {@link OpenGroups}.
 * <pre>
 * script := spacing? item*
 * </pre>
 * {@link #RULES} states a script and an item; every fault repeats it, so a script typed in the dev panel shows the order
 * it broke. A spacing is the script's first word, since it times the whole wave. An item is a slot, or a group of items
 * in brackets. A group's rank and affixes are the defaults of everything inside it: an inner rank replaces them, inner
 * affixes add to them, and a slot no rank reaches takes the fallback rank. The count before a slot or a group repeats
 * it; the one after a shape is its member count.
 * <p>
 * {@link #script} reads a whole script; {@link #next} reads the start of one and says what the same rules would
 * accept after it.
 */
final class WaveScriptGrammar {

    static final String RULES = "script := w<n>? item*; item := count? rank? (shape count?)? affix* (enemy | e)"
            + " | count? rank? affix* ( item+ )";

    private final Rank fallbackRank;
    private final EnemyCatalog catalog;

    WaveScriptGrammar(Rank fallbackRank, EnemyCatalog catalog) {
        this.fallbackRank = fallbackRank;
        this.catalog = catalog;
    }

    WaveContent script(String[] tokens) throws ScriptFault {
        Parsed<Float> spacing = spacing(Cursor.over(tokens));
        OpenGroups groups = this.noGroups();
        Cursor at = spacing.rest();
        while (!at.atEnd()) {
            if (at.peek().equals(WaveScript.GROUP_END)) {
                at = groups.close(at);
            } else {
                Parsed<Words> words = words(at, groups.defaults());
                at = this.item(words.rest(), words.value(), groups);
            }
        }
        if (groups.isNested()) {
            throw at.fail("A '" + WaveScript.GROUP_START + "' is never closed");
        }
        return new WaveContent(groups.slots(), spacing.value());
    }

    /** What may follow {@code typed}, the start of a script; fails where the start already does. */
    NextWords next(String[] typed) throws ScriptFault {
        Cursor start = Cursor.over(typed);
        OpenGroups groups = this.noGroups();
        Cursor at = spacing(start).rest();
        while (!at.atEnd()) {
            if (at.peek().equals(WaveScript.GROUP_END)) {
                at = groups.close(at);
            } else {
                Parsed<Words> words = words(at, groups.defaults());
                if (words.rest().atEnd()) {
                    return nextInItem(groups.defaults(), words.value());
                }
                at = this.item(words.rest(), words.value(), groups);
            }
        }
        NextWords next = nextInItem(groups.defaults(), Words.none());
        if (start.atEnd()) {
            return next.with(NextWords.Kind.SPACING);
        }
        return groups.canClose() ? next.with(NextWords.Kind.GROUP_END) : next;
    }

    private OpenGroups noGroups() {
        return new OpenGroups(new Defaults(this.fallbackRank, List.of()));
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

    /** What an item says before the token it spawns or the bracket it opens; it stops there or at the end. */
    private static Parsed<Words> words(Cursor at, Defaults defaults) throws ScriptFault {
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
        Parsed<List<Affix>> affixes = affixes(afterShape, defaults.affixes());
        Cursor target = affixes.rest();
        if (!affixes.value().isEmpty() && (isRank(target) || isShape(target))) {
            throw target.fail("'" + target.peek() + "' follows an affix - affixes go right before the enemy id or the"
                    + " bracket, after the rank and spawn type");
        }
        return new Parsed<>(new Words(repeat.value(), rank.value(),
                shape.value().map(keyword -> new ShapeWords(keyword, memberCount.value())), affixes.value()), target);
    }

    /**
     * The affix words from here on, each checked against {@code held} (the open groups') and the ones read before it;
     * the result holds only the ones read here.
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
                throw here.fail("A slot carries at most " + WaveScript.MAX_AFFIXES + " affixes, its brackets' included");
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

    /** What {@code words} lead to: the group they open, or the slot they describe, added to the innermost group. */
    private Cursor item(Cursor at, Words words, OpenGroups groups) throws ScriptFault {
        if (!at.atEnd() && at.peek().equals(WaveScript.GROUP_START)) {
            if (words.shape().isPresent()) {
                throw at.fail("Spawn-type token '" + words.shape().get().keyword().token()
                        + "' shapes one enemy, so it cannot go on brackets");
            }
            groups.open(words);
            return at.next();
        }
        Parsed<WaveSlot> slot = this.slot(at, groups.defaults(), words);
        groups.add(at, List.of(slot.value()), words.repeatTimes());
        return slot.rest();
    }

    private Parsed<WaveSlot> slot(Cursor at, Defaults defaults, Words words) throws ScriptFault {
        if (at.atEnd()) {
            throw at.fail("Wave ends with " + leftOver(words));
        }
        String token = at.peek();
        if (token.equals(WaveScript.GROUP_END)) {
            throw at.fail("Brackets close after " + leftOver(words));
        }
        if (WaveScript.isSpacingToken(token)) {
            throw at.fail("Spacing token '" + token + "' times the whole wave, so it is the script's first word, e.g. '"
                    + token + " 3 walker'");
        }
        if (asCount(token).isPresent()) {
            throw at.fail("A count goes first in its slot, to repeat it, or right after a spawn type, for its members:"
                    + " e.g. '2 elite swarm 4 walker'");
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
            return new Parsed<>(new EmptySlot(), at.next());
        }
        if (this.catalog.contains(token)) {
            return new Parsed<>(this.enemySlot(at, defaults, words), at.next());
        }
        throw at.fail("Unrecognized wave token '" + token
                + "': expected a bracket, a spawn-type, rank or affix keyword (" + WaveScript.RESERVED_TOKENS
                + "), a repeat count, or an id registered in the enemy catalog (" + this.catalog.ids() + ")");
    }

    /** What {@code words} leave hanging when nothing they could describe follows them. */
    private static String leftOver(Words words) {
        if (!words.affixes().isEmpty()) {
            return "affix '" + words.affixes().getLast().word() + "' and no enemy id";
        }
        if (words.shape().isPresent()) {
            return "spawn-type token '" + words.shape().get().keyword().token() + "' and no enemy id";
        }
        if (words.rank().isPresent()) {
            return "rank token '" + words.rank().get() + "' and no enemy id";
        }
        return "count " + words.repeat().orElseThrow() + " and nothing for it to repeat";
    }

    /** What may come next in an item after {@code words}, under {@code defaults}. */
    private NextWords nextInItem(Defaults defaults, Words words) {
        Optional<ShapeWords> shape = words.shape();
        if (shape.isPresent() && shape.get().keyword().takesMemberCount() && shape.get().members().isEmpty()) {
            return new NextWords(EnumSet.of(NextWords.Kind.MEMBER_COUNT), List.of());
        }
        List<Affix> carried = Stream.concat(defaults.affixes().stream(), words.affixes().stream()).toList();
        Set<NextWords.Kind> kinds = EnumSet.of(NextWords.Kind.ENEMY);
        boolean bare = words.affixes().isEmpty() && shape.isEmpty();
        if (bare && words.rank().isEmpty()) {
            kinds.add(NextWords.Kind.RANK);
            kinds.add(NextWords.Kind.SPACER);
        }
        if (bare) {
            kinds.add(NextWords.Kind.SHAPE);
        }
        if (shape.isEmpty()) {
            kinds.add(NextWords.Kind.GROUP_START);
        }
        if (carried.isEmpty() && words.rank().filter(rank -> rank != Rank.BOSS).isEmpty()) {
            kinds.add(NextWords.Kind.UNIQUE_ENEMY);
        }
        boolean swarm = shape.filter(written -> written.keyword() == ShapeKeyword.SWARM).isPresent();
        return new NextWords(kinds, allowedAffixes(carried).stream()
                .filter(affix -> !swarm || !affix.actsOnAllies()).toList());
    }

    private WaveSlot enemySlot(Cursor at, Defaults defaults, Words words) throws ScriptFault {
        RankedEnemy enemy = this.catalog.ranked(at.peek());
        if (enemy.isUnique() && words.rank().isPresent() && words.rank().get() != Rank.BOSS) {
            throw at.fail("'" + at.peek() + "' is unique and comes only at Boss rank, not " + words.rank().get());
        }
        List<Affix> affixes = Stream.concat(defaults.affixes().stream(), words.affixes().stream()).toList();
        if (enemy.isUnique() && !affixes.isEmpty()) {
            throw at.fail("'" + at.peek() + "' is unique and takes no affix");
        }
        Rank requested = words.rank().orElse(defaults.rank());
        Rank rank = enemy.effectiveRank(requested);
        EnemyDefinition definition = enemy.definitionFor(requested);
        boolean swarm = words.shape().filter(shape -> shape.keyword() == ShapeKeyword.SWARM).isPresent();
        for (Affix affix : affixes) {
            if (swarm && affix.actsOnAllies()) {
                throw at.fail("Affix '" + affix.word() + "' works on the allies near it, so it cannot go on a swarm,"
                        + " whose every member would cast it");
            }
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
            SPACING, GROUP_START, GROUP_END, RANK, SPACER, SHAPE, MEMBER_COUNT, ENEMY, UNIQUE_ENEMY
        }

        boolean accepts(Kind kind) {
            return this.kinds.contains(kind);
        }

        /** This, accepting {@code kind} too. */
        NextWords with(Kind kind) {
            Set<Kind> more = EnumSet.of(kind);
            more.addAll(this.kinds);
            return new NextWords(more, this.affixes);
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

    /** What the brackets round an item give it: the rank it takes unless it names one, and affixes it carries. */
    private record Defaults(Rank rank, List<Affix> affixes) {

        /** The defaults inside a group that {@code words} stand before. */
        Defaults within(Words words) {
            return new Defaults(words.rank().orElse(this.rank),
                    Stream.concat(this.affixes.stream(), words.affixes().stream()).toList());
        }
    }

    /** A group not yet closed, or the script itself: its defaults, how often it repeats, and what it holds so far. */
    private record Group(Defaults defaults, int repeat, List<WaveSlot> slots) {
    }

    /** The groups open at a point of a script, the innermost on top and the script itself at the bottom. */
    private static final class OpenGroups {

        private final Deque<Group> groups = new ArrayDeque<>();

        OpenGroups(Defaults outermost) {
            this.groups.push(new Group(outermost, 1, new ArrayList<>()));
        }

        Defaults defaults() {
            return this.groups.element().defaults();
        }

        boolean isNested() {
            return this.groups.size() > 1;
        }

        boolean canClose() {
            return this.isNested() && !this.groups.element().slots().isEmpty();
        }

        void open(Words words) {
            this.groups.push(new Group(this.defaults().within(words), words.repeatTimes(), new ArrayList<>()));
        }

        /** Repeats the innermost group into the one round it; {@code at} is its closing bracket. */
        Cursor close(Cursor at) throws ScriptFault {
            if (!this.isNested()) {
                throw at.fail("'" + WaveScript.GROUP_END + "' closes no '" + WaveScript.GROUP_START + "'");
            }
            if (!this.canClose()) {
                throw at.fail("Brackets need at least one slot between them");
            }
            Group closed = this.groups.pop();
            this.add(at, closed.slots(), closed.repeat());
            return at.next();
        }

        /** Adds {@code slots}, {@code times} over, to the innermost group; checked before any of them is added. */
        void add(Cursor at, List<WaveSlot> slots, int times) throws ScriptFault {
            List<WaveSlot> into = this.groups.element().slots();
            if (into.size() + (long) slots.size() * times > WaveScript.MAX_SLOTS) {
                throw at.fail("A wave has at most " + WaveScript.MAX_SLOTS + " slots");
            }
            for (int i = 0; i < times; i++) {
                into.addAll(slots);
            }
        }

        /** The script's own slots, once every group is closed. */
        List<WaveSlot> slots() {
            return this.groups.element().slots();
        }
    }

    /** What an item says before the token it spawns or the bracket it opens. */
    private record Words(OptionalInt repeat, Optional<Rank> rank, Optional<ShapeWords> shape, List<Affix> affixes) {

        static Words none() {
            return new Words(OptionalInt.empty(), Optional.empty(), Optional.empty(), List.of());
        }

        int repeatTimes() {
            return this.repeat.orElse(1);
        }
    }

    /** A spawn-type keyword and the member count written after it, if any. */
    private record ShapeWords(ShapeKeyword keyword, OptionalInt members) {
    }

    /** A position in the non-blank tokens, each bracket a token of its own; moving it makes a new one. */
    private record Cursor(List<String> tokens, int index) {

        private static final Pattern BRACKET_EDGE = Pattern.compile("(?<=[()])|(?=[()])");

        static Cursor over(String[] tokens) {
            return new Cursor(Stream.of(tokens).filter(token -> !token.isBlank()).flatMap(BRACKET_EDGE::splitAsStream)
                    .toList(), 0);
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
                return new ScriptFault("Wave script ends early, in \"" + before + " »«\": " + message + ". " + RULES);
            }
            String after = String.join(" ", this.tokens.subList(this.index + 1, this.tokens.size()));
            return new ScriptFault("Wave script token " + (this.index + 1) + " '" + peek() + "', in \""
                    + before + " »" + peek() + "« " + after + "\": " + message + ". " + RULES);
        }
    }
}
