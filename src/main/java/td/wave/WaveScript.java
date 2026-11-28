package td.wave;

import td.enemy.Affix;
import td.enemy.EnemyCatalog;
import td.enemy.Rank;
import td.enemy.SpawnParameters;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses a wave's token string into {@link WaveContent} against an {@link EnemyCatalog}, so
 * level-defined enemies resolve like built-ins. {@link #RESERVED_TOKENS} are recognized before any
 * lookup. Anything else that is not a registered id or a count fails with
 * {@link GameStartupException}. {@link WaveScriptGrammar} states the grammar.
 * <p>
 * <strong>A count applies to the token right after it.</strong> Before an id or {@code e} it
 * repeats the slot; before a rank or shape keyword it repeats the whole slot. After a shape keyword
 * it sets that slot's member count: required for {@code swarm}, {@code line}, {@code column} and
 * {@code drip}, rejected for {@code flank}. {@link Affix} words go right before the id.
 * <p>
 * Brackets group items ({@code 3 elite regenerating (3 walker e knight)}). A count before them repeats the group, a
 * rank is what every slot inside takes unless it or an inner group names one, and affixes are carried by every slot
 * inside besides its own. Groups nest, and one round the whole script gives the wave its rank. A bracket needs no
 * space beside it.
 * <p>
 * The script's first word may be {@code w<number>} (such as {@code w30}): how many ticks one slot of spacing is worth
 * to an enemy of speed 1, in place of {@link SpawnParameters#DEFAULT_DELAY_TICKS_PER_SLOT}.
 */
public final class WaveScript {

    /** Catches a typo like {@code 99999 walker} at parse time, before it builds a slot for every repeat. */
    public static final int MAX_REPEAT = 100;

    /** How many slots a wave may hold, repeats expanded: the counts of nested groups multiply. */
    public static final int MAX_SLOTS = 1000;

    static final String EMPTY_TOKEN = "e";
    static final String GROUP_START = "(";
    static final String GROUP_END = ")";

    private static final String SPACING_PREFIX = "w";
    private static final Pattern SPACING_TOKEN = Pattern.compile("w\\d+(\\.\\d+)?");
    private static final Pattern COUNT_TOKEN = Pattern.compile("\\d+");

    /**
     * Tokens recognized before any catalog lookup; the catalog rejects ids that collide with them.
     */
    public static final Set<String> RESERVED_TOKENS = Stream.of(
                    Stream.of(EMPTY_TOKEN),
                    Stream.of(RankKeyword.values()).map(RankKeyword::token),
                    Stream.of(ShapeKeyword.values()).map(ShapeKeyword::token),
                    Stream.of(Affix.values()).map(Affix::word))
            .flatMap(tokens -> tokens).collect(Collectors.toUnmodifiableSet());

    /** How many affixes one slot may carry, its brackets' included. */
    public static final int MAX_AFFIXES = 2;

    private WaveScript() {
    }

    /** Whether {@code id} is a reserved keyword or has the shape of a spacing token or a count, so no enemy may take it. */
    public static boolean isReserved(String id) {
        return RESERVED_TOKENS.contains(id) || isSpacingToken(id) || isCountToken(id);
    }

    static boolean isCountToken(String token) {
        return COUNT_TOKEN.matcher(token).matches();
    }

    static boolean isSpacingToken(String token) {
        return SPACING_TOKEN.matcher(token).matches();
    }

    /** The ticks per slot a spacing token sets. */
    static float spacingTicks(String spacingToken) {
        return Float.parseFloat(spacingToken.substring(SPACING_PREFIX.length()));
    }

    /** A level's wave: Grunt wherever the script names no rank. */
    public static WaveContent parse(String script, EnemyCatalog catalog) {
        return parse(script, Rank.GRUNT, catalog);
    }

    /** {@code fallbackRank} is the rank of a slot that neither names one nor stands in a group that does. */
    public static WaveContent parse(String script, Rank fallbackRank, EnemyCatalog catalog) {
        try {
            return new WaveScriptGrammar(fallbackRank, catalog).script(script.split(" "));
        } catch (WaveScriptGrammar.ScriptFault fault) {
            throw fault.asStartupFailure();
        }
    }

    /** {@code slots} in a group that gives them {@code rank}. */
    public static String underRank(Rank rank, String slots) {
        return RankKeyword.tokenFor(rank) + " " + GROUP_START + slots + GROUP_END;
    }
}
