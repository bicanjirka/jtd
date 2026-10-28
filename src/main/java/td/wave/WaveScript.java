package td.wave;

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
 * {@link GameStartupException}.
 * <p>
 * <strong>A count applies to the token right after it.</strong> Before an id or {@code e} it
 * repeats the slot; before a rank or shape keyword it repeats the whole slot. After a shape keyword
 * it sets that slot's member count: required for {@code swarm}, {@code line}, {@code column} and
 * {@code drip}, rejected for {@code plated} and {@code flank}.
 * <p>
 * A leading {@code w<number>} (such as {@code w30}) sets how many ticks one slot of spacing is
 * worth, in place of {@link SpawnParameters#DEFAULT_DELAY_TICKS_PER_SLOT}. It may only be the first
 * token.
 */
public final class WaveScript {

    static final String EMPTY_TOKEN = "e";

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
    public static final Set<String> RESERVED_TOKENS = Stream.concat(
            Stream.of(EMPTY_TOKEN, GRUNT_TOKEN, SOLDIER_TOKEN, VETERAN_TOKEN, ELITE_TOKEN, BOSS_TOKEN),
            Stream.of(ShapeKeyword.values()).map(ShapeKeyword::token)).collect(Collectors.toUnmodifiableSet());

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

    public static WaveContent parse(String tokens, Rank defaultRank, EnemyCatalog catalog) {
        return parse(tokens.split(" "), defaultRank, catalog);
    }

    public static WaveContent parse(String[] tokens, Rank defaultRank, EnemyCatalog catalog) {
        return new WaveScriptGrammar(defaultRank, catalog).script(tokens);
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
