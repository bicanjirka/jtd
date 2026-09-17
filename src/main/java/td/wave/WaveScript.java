package td.wave;

import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.util.GameStartupException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parses a wave's token string (see the wave mini-language table in CLAUDE.md) into a
 * {@link WaveContent} against a given {@link EnemyCatalog} - no {@code GameWorld} needed,
 * unlike {@link Wave}, which is what turns that content into live, world-bound enemies. The
 * {@code catalog} argument is what lets a per-level custom or cloned enemy id resolve exactly
 * like a built-in one; there is no separate syntax for the two, since every non-reserved token
 * is looked up the same way. {@link #RESERVED_TOKENS} - the spacer plus seven spawn-type
 * keywords - are recognized before any catalog lookup. A token this can't recognize as one of
 * those, a registered id, or an integer repeat count fails the parse with a
 * {@link GameStartupException}: a wave the author did not write is content corruption, not
 * something to recover from silently.
 * <p>
 * <strong>A count applies to the token immediately following it</strong>, in either of two
 * positions: before an enemy id (or {@code e}) it repeats the slot; before a spawn-type token
 * it repeats the whole shaped slot that token produces; and a count immediately after a
 * spawn-type token (before its enemy id) sets that one slot's member count instead - required
 * for {@code swarm}/{@code line}/{@code column}/{@code drip}, rejected for
 * {@code boss}/{@code elite}/{@code flank}, whose member count is fixed by the shape.
 */
public final class WaveScript {

    private static final String EMPTY_TOKEN = "e";
    private static final String BOSS_TOKEN = "boss";
    private static final String ELITE_TOKEN = "elite";
    private static final String SWARM_TOKEN = "swarm";
    private static final String LINE_TOKEN = "line";
    private static final String FLANK_TOKEN = "flank";
    private static final String COLUMN_TOKEN = "column";
    private static final String DRIP_TOKEN = "drip";

    /**
     * Every token recognized before any catalog lookup. {@code EnemyCatalog.register} rejects
     * an id colliding with one of these, so a level can never silently shadow the grammar.
     */
    public static final Set<String> RESERVED_TOKENS = Set.of(EMPTY_TOKEN, BOSS_TOKEN, ELITE_TOKEN,
            SWARM_TOKEN, LINE_TOKEN, FLANK_TOKEN, COLUMN_TOKEN, DRIP_TOKEN);

    private WaveScript() {
    }

    public static WaveContent parse(String tokens, EnemyCatalog catalog) {
        return parse(tokens.split(" "), catalog);
    }

    public static WaveContent parse(String[] tokens, EnemyCatalog catalog) {
        List<WaveSlot> spawnSequence = new ArrayList<>();
        int repeat = 1;
        boolean repeatExplicit = false;
        String pendingShape = null;
        int pendingShapeSlotRepeat = 1;

        for (String token : tokens) {
            // An empty string is whitespace, not a token: "".split(" ") yields one blank, and
            // so does any run of spaces between real tokens.
            if (token.isBlank()) {
                continue;
            }
            if (isShapeToken(token)) {
                if (pendingShape != null) {
                    throw new GameStartupException("Spawn-type token '" + token + "' follows '" + pendingShape
                            + "' with no enemy id between them");
                }
                pendingShape = token;
                pendingShapeSlotRepeat = repeat;
                repeat = 1;
                repeatExplicit = false;
            } else if (token.equals(EMPTY_TOKEN)) {
                if (pendingShape != null) {
                    throw new GameStartupException(
                            "The spacer 'e' cannot follow spawn-type token '" + pendingShape + "'");
                }
                for (int i = 0; i < repeat; i++) {
                    spawnSequence.add(new EmptySlot());
                }
                repeat = 1;
                repeatExplicit = false;
            } else if (catalog.contains(token)) {
                EnemyDefinition definition = catalog.get(token);
                if (pendingShape == null) {
                    WaveSlot slot = new EnemySlot(definition, SpawnShape.normal());
                    for (int i = 0; i < repeat; i++) {
                        spawnSequence.add(slot);
                    }
                } else {
                    WaveSlot slot = new EnemySlot(definition, shapeFor(pendingShape, repeat, repeatExplicit));
                    for (int i = 0; i < pendingShapeSlotRepeat; i++) {
                        spawnSequence.add(slot);
                    }
                    pendingShape = null;
                    pendingShapeSlotRepeat = 1;
                }
                repeat = 1;
                repeatExplicit = false;
            } else {
                try {
                    repeat = Integer.parseInt(token);
                    repeatExplicit = true;
                } catch (NumberFormatException ex) {
                    throw new GameStartupException("Unrecognized wave token '" + token
                            + "': expected a spawn-type keyword (" + RESERVED_TOKENS + "), a repeat count, or an id"
                            + " registered in the enemy catalog (" + catalog.ids() + ")", ex);
                }
            }
        }
        if (pendingShape != null) {
            throw new GameStartupException("Wave ends with spawn-type token '" + pendingShape + "' and no enemy id");
        }
        return new WaveContent(spawnSequence);
    }

    private static boolean isShapeToken(String token) {
        return switch (token) {
            case BOSS_TOKEN, ELITE_TOKEN, SWARM_TOKEN, LINE_TOKEN, FLANK_TOKEN, COLUMN_TOKEN, DRIP_TOKEN -> true;
            default -> false;
        };
    }

    private static SpawnShape shapeFor(String shapeToken, int memberCount, boolean memberCountGiven) {
        return switch (shapeToken) {
            case BOSS_TOKEN -> fixedMemberShape(shapeToken, memberCountGiven, SpawnShape.boss());
            case ELITE_TOKEN -> fixedMemberShape(shapeToken, memberCountGiven, SpawnShape.elite());
            case FLANK_TOKEN -> fixedMemberShape(shapeToken, memberCountGiven, SpawnShape.flank());
            case SWARM_TOKEN -> SpawnShape.swarm(requiredMemberCount(shapeToken, memberCountGiven, memberCount));
            case LINE_TOKEN -> SpawnShape.line(requiredMemberCount(shapeToken, memberCountGiven, memberCount));
            case COLUMN_TOKEN -> SpawnShape.column(requiredMemberCount(shapeToken, memberCountGiven, memberCount));
            case DRIP_TOKEN -> SpawnShape.drip(requiredMemberCount(shapeToken, memberCountGiven, memberCount));
            default -> throw new IllegalStateException("Not a spawn-type token: " + shapeToken);
        };
    }

    private static SpawnShape fixedMemberShape(String shapeToken, boolean memberCountGiven, SpawnShape shape) {
        if (memberCountGiven) {
            throw new GameStartupException(
                    "Spawn type '" + shapeToken + "' has a fixed member count and cannot take one");
        }
        return shape;
    }

    private static int requiredMemberCount(String shapeToken, boolean memberCountGiven, int memberCount) {
        if (!memberCountGiven) {
            throw new GameStartupException(
                    "Spawn type '" + shapeToken + "' needs a member count, e.g. '" + shapeToken + " 3 c'");
        }
        return memberCount;
    }
}
