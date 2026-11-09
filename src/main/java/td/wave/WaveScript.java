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
 * A head, ended by {@code :}, holds the wave's defaults: {@code w<number>} (such as {@code w30}),
 * how many ticks one slot of spacing is worth in place of
 * {@link SpawnParameters#DEFAULT_DELAY_TICKS_PER_SLOT}, then the rank every slot takes unless it
 * names its own, then affixes every slot carries besides its own ({@code w30 soldier armored: 3 walker}).
 */
public final class WaveScript {

    /** Catches a typo like {@code 99999 walker} at parse time, before it builds a slot for every repeat. */
    public static final int MAX_REPEAT = 100;

    static final String EMPTY_TOKEN = "e";
    static final String HEAD_END = ":";

    private static final String GRUNT_TOKEN = "grunt";
    private static final String SOLDIER_TOKEN = "soldier";
    private static final String VETERAN_TOKEN = "veteran";
    private static final String ELITE_TOKEN = "elite";
    private static final String BOSS_TOKEN = "boss";
    private static final String SPACING_PREFIX = "w";
    private static final Pattern SPACING_TOKEN = Pattern.compile("w\\d+(\\.\\d+)?");

    /**
     * Tokens recognized before any catalog lookup; the catalog rejects ids that collide with them.
     */
    public static final Set<String> RESERVED_TOKENS = Stream.of(
                    Stream.of(EMPTY_TOKEN, GRUNT_TOKEN, SOLDIER_TOKEN, VETERAN_TOKEN, ELITE_TOKEN, BOSS_TOKEN),
                    Stream.of(ShapeKeyword.values()).map(ShapeKeyword::token),
                    Stream.of(Affix.values()).map(Affix::word))
            .flatMap(tokens -> tokens).collect(Collectors.toUnmodifiableSet());

    /** How many affixes one slot may carry, its head's included. */
    public static final int MAX_AFFIXES = 2;

    private WaveScript() {
    }

    /** Whether {@code id} is a reserved keyword or has the shape of a spacing token, so no enemy may take it. */
    public static boolean isReserved(String id) {
        return RESERVED_TOKENS.contains(id) || isSpacingToken(id);
    }

    static boolean isSpacingToken(String token) {
        return SPACING_TOKEN.matcher(token).matches();
    }

    /** The ticks per slot a spacing token sets. */
    static float spacingTicks(String spacingToken) {
        return Float.parseFloat(spacingToken.substring(SPACING_PREFIX.length()));
    }

    /** A level's wave: Grunt unless the head names a rank. */
    public static WaveContent parse(String script, EnemyCatalog catalog) {
        return parse(script, Rank.GRUNT, catalog);
    }

    /** {@code fallbackRank} is the rank of a script whose head names none. */
    public static WaveContent parse(String script, Rank fallbackRank, EnemyCatalog catalog) {
        return new WaveScriptGrammar(fallbackRank, catalog).script(script.split(" "));
    }

    static boolean isShapeToken(String token) {
        return ShapeKeyword.of(token).isPresent();
    }

    /** Whether the shape keyword {@code token} takes a member count (the others forbid one). */
    static boolean takesMemberCount(String token) {
        return ShapeKeyword.of(token).filter(ShapeKeyword::takesMemberCount).isPresent();
    }

    static boolean isRankToken(String token) {
        return switch (token) {
            case GRUNT_TOKEN, SOLDIER_TOKEN, VETERAN_TOKEN, ELITE_TOKEN, BOSS_TOKEN -> true;
            default -> false;
        };
    }

    /** {@code slots} under a head that sets {@code rank}. */
    public static String headed(Rank rank, String slots) {
        return rankToken(rank) + HEAD_END + " " + slots;
    }

    static String rankToken(Rank rank) {
        return switch (rank) {
            case GRUNT -> GRUNT_TOKEN;
            case SOLDIER -> SOLDIER_TOKEN;
            case VETERAN -> VETERAN_TOKEN;
            case ELITE -> ELITE_TOKEN;
            case BOSS -> BOSS_TOKEN;
        };
    }

    static Rank rankFor(String token) {
        return switch (token) {
            case GRUNT_TOKEN -> Rank.GRUNT;
            case SOLDIER_TOKEN -> Rank.SOLDIER;
            case VETERAN_TOKEN -> Rank.VETERAN;
            case ELITE_TOKEN -> Rank.ELITE;
            case BOSS_TOKEN -> Rank.BOSS;
            default -> throw new IllegalStateException("Not a rank token: " + token);
        };
    }
}
