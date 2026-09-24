package td.wave;

import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.util.GameStartupException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parses a wave's token string (see the wave mini-language table in CLAUDE.md) into a
 * {@link WaveContent} against a given {@link EnemyCatalog} and the wave's own default
 * {@link Rank} - no {@code GameWorld} needed, unlike {@link Wave}, which is what turns that
 * content into live, world-bound enemies. The {@code catalog} argument is what lets a per-level
 * custom or cloned enemy id resolve exactly like a built-in one; there is no separate syntax for
 * the two, since every non-reserved token is looked up the same way. {@link #RESERVED_TOKENS} -
 * the spacer, six spawn-type keywords and five rank keywords - are recognized before any catalog
 * lookup. A token this can't recognize as one of those, a registered id, or an integer repeat
 * count fails the parse with a {@link GameStartupException}: a wave the author did not write is
 * content corruption, not something to recover from silently.
 * <p>
 * <strong>A count applies to the token immediately following it</strong>, in any of three
 * positions: before an enemy id (or {@code e}) it repeats the slot; before a rank token or a
 * spawn-type token it repeats the whole ranked-and/or-shaped slot that token produces - whichever
 * of the two keywords comes first in a slot is the one a leading count attaches to, since a rank
 * token (if present) always precedes a spawn-type token, never the reverse. A count immediately
 * after a spawn-type token (before its enemy id) sets that one slot's member count instead -
 * required for {@code swarm}/{@code line}/{@code column}/{@code drip}, rejected for
 * {@code armored}/{@code flank}, whose member count is fixed by the shape.
 */
public final class WaveScript {

    private static final String EMPTY_TOKEN = "e";
    private static final String ARMORED_TOKEN = "armored";
    private static final String SWARM_TOKEN = "swarm";
    private static final String LINE_TOKEN = "line";
    private static final String FLANK_TOKEN = "flank";
    private static final String COLUMN_TOKEN = "column";
    private static final String DRIP_TOKEN = "drip";

    private static final String GRUNT_TOKEN = "grunt";
    private static final String SOLDIER_TOKEN = "soldier";
    private static final String VETERAN_TOKEN = "veteran";
    private static final String ELITE_TOKEN = "elite";
    private static final String BOSS_TOKEN = "boss";

    /**
     * Every token recognized before any catalog lookup. {@code EnemyCatalog.register} rejects
     * an id colliding with one of these, so a level can never silently shadow the grammar.
     */
    public static final Set<String> RESERVED_TOKENS = Set.of(EMPTY_TOKEN, ARMORED_TOKEN,
            SWARM_TOKEN, LINE_TOKEN, FLANK_TOKEN, COLUMN_TOKEN, DRIP_TOKEN,
            GRUNT_TOKEN, SOLDIER_TOKEN, VETERAN_TOKEN, ELITE_TOKEN, BOSS_TOKEN);

    private WaveScript() {
    }

    public static WaveContent parse(String tokens, Rank defaultRank, EnemyCatalog catalog) {
        return parse(tokens.split(" "), defaultRank, catalog);
    }

    public static WaveContent parse(String[] tokens, Rank defaultRank, EnemyCatalog catalog) {
        List<WaveSlot> spawnSequence = new ArrayList<>();
        int repeat = 1;
        boolean repeatExplicit = false;
        String pendingShape = null;
        int pendingShapeSlotRepeat = 1;
        Rank pendingRank = null;
        int pendingRankSlotRepeat = 1;

        for (String token : tokens) {
            // "".split(" ") yields a blank, as does a run of spaces.
            if (token.isBlank()) {
                continue;
            }
            if (isRankToken(token)) {
                if (pendingRank != null) {
                    throw new GameStartupException(
                            "Rank token '" + token + "' follows another rank token with no id between them");
                }
                if (pendingShape != null) {
                    throw new GameStartupException(
                            "Rank token '" + token + "' follows spawn-type token '" + pendingShape
                                    + "' - a rank token must come before the spawn-type token it shapes, not after");
                }
                pendingRank = rankFor(token);
                pendingRankSlotRepeat = repeatExplicit ? repeat : 1;
                repeat = 1;
                repeatExplicit = false;
            } else if (isShapeToken(token)) {
                if (pendingShape != null) {
                    throw new GameStartupException("Spawn-type token '" + token + "' follows '" + pendingShape
                            + "' with no enemy id between them");
                }
                pendingShape = token;
                pendingShapeSlotRepeat = slotRepeat(repeat, repeatExplicit, pendingRank, pendingRankSlotRepeat);
                repeat = 1;
                repeatExplicit = false;
            } else if (token.equals(EMPTY_TOKEN)) {
                if (pendingShape != null) {
                    throw new GameStartupException(
                            "The spacer 'e' cannot follow spawn-type token '" + pendingShape + "'");
                }
                if (pendingRank != null) {
                    throw new GameStartupException("The spacer 'e' cannot follow rank token '" + pendingRank + "'");
                }
                for (int i = 0; i < repeat; i++) {
                    spawnSequence.add(new EmptySlot());
                }
                repeat = 1;
                repeatExplicit = false;
            } else if (catalog.contains(token)) {
                Rank requestedRank = pendingRank != null ? pendingRank : defaultRank;
                RankedEnemy rankedEnemy = catalog.ranked(token);
                EnemyDefinition definition = rankedEnemy.definitionFor(requestedRank);
                Rank effectiveRank = rankedEnemy.effectiveRank(requestedRank);
                if (pendingShape == null) {
                    int slotRepeat = slotRepeat(repeat, repeatExplicit, pendingRank, pendingRankSlotRepeat);
                    WaveSlot slot = new EnemySlot(definition, effectiveRank, SpawnShape.normal());
                    for (int i = 0; i < slotRepeat; i++) {
                        spawnSequence.add(slot);
                    }
                } else {
                    WaveSlot slot = new EnemySlot(definition, effectiveRank, shapeFor(pendingShape, repeat, repeatExplicit));
                    for (int i = 0; i < pendingShapeSlotRepeat; i++) {
                        spawnSequence.add(slot);
                    }
                    pendingShape = null;
                    pendingShapeSlotRepeat = 1;
                }
                pendingRank = null;
                pendingRankSlotRepeat = 1;
                repeat = 1;
                repeatExplicit = false;
            } else {
                try {
                    repeat = Integer.parseInt(token);
                    repeatExplicit = true;
                } catch (NumberFormatException ex) {
                    throw new GameStartupException("Unrecognized wave token '" + token
                            + "': expected a spawn-type or rank keyword (" + RESERVED_TOKENS + "), a repeat count, or an id"
                            + " registered in the enemy catalog (" + catalog.ids() + ")", ex);
                }
            }
        }
        if (pendingShape != null) {
            throw new GameStartupException("Wave ends with spawn-type token '" + pendingShape + "' and no enemy id");
        }
        if (pendingRank != null) {
            throw new GameStartupException("Wave ends with rank token '" + pendingRank + "' and no enemy id");
        }
        return new WaveContent(spawnSequence);
    }

    /**
     * A slot's own repeat count: an explicit count immediately preceding the current token wins
     * (the ordinary "applies to the token immediately following it" rule); failing that, a rank
     * prefix's own captured count carries forward, since a rank token (if present) is always the
     * first keyword in a slot and would otherwise have its leading count silently dropped once
     * the shape or id token after it resets {@code repeat} back to its default.
     */
    private static int slotRepeat(int repeat, boolean repeatExplicit, Rank pendingRank, int pendingRankSlotRepeat) {
        if (repeatExplicit) {
            return repeat;
        }
        return pendingRank != null ? pendingRankSlotRepeat : repeat;
    }

    private static boolean isShapeToken(String token) {
        return switch (token) {
            case ARMORED_TOKEN, SWARM_TOKEN, LINE_TOKEN, FLANK_TOKEN, COLUMN_TOKEN, DRIP_TOKEN -> true;
            default -> false;
        };
    }

    private static boolean isRankToken(String token) {
        return switch (token) {
            case GRUNT_TOKEN, SOLDIER_TOKEN, VETERAN_TOKEN, ELITE_TOKEN, BOSS_TOKEN -> true;
            default -> false;
        };
    }

    private static Rank rankFor(String token) {
        return switch (token) {
            case GRUNT_TOKEN -> Rank.GRUNT;
            case SOLDIER_TOKEN -> Rank.SOLDIER;
            case VETERAN_TOKEN -> Rank.VETERAN;
            case ELITE_TOKEN -> Rank.ELITE;
            case BOSS_TOKEN -> Rank.BOSS;
            default -> throw new IllegalStateException("Not a rank token: " + token);
        };
    }

    private static SpawnShape shapeFor(String shapeToken, int memberCount, boolean memberCountGiven) {
        return switch (shapeToken) {
            case ARMORED_TOKEN -> fixedMemberShape(shapeToken, memberCountGiven, SpawnShape.armored());
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
