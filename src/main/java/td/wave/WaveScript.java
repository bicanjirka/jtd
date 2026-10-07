package td.wave;

import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.enemy.SpawnParameters;
import td.util.GameStartupException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses a wave's token string into {@link WaveContent} against an {@link EnemyCatalog}, so
 * level-defined enemies resolve like built-ins. {@link #RESERVED_TOKENS} are recognized before any
 * lookup. Anything else that is not a registered id or a count fails with
 * {@link GameStartupException}.
 * <p>
 * <strong>A count applies to the token right after it.</strong> Before an id or {@code e} it
 * repeats the slot; before a rank or shape keyword it repeats the whole slot. After a shape keyword
 * it sets that slot's member count: required for {@code swarm}, {@code line}, {@code column} and
 * {@code drip}, rejected for {@code armored} and {@code flank}.
 * <p>
 * A leading {@code w<number>} (such as {@code w30}) sets how many ticks one slot of spacing is
 * worth, in place of {@link SpawnParameters#DEFAULT_DELAY_TICKS_PER_SLOT}. It may only be the first
 * token.
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
    private static final String SPACING_PREFIX = "w";
    private static final Pattern SPACING_TOKEN = Pattern.compile("w\\d+(\\.\\d+)?");

    /**
     * Tokens recognized before any catalog lookup; the catalog rejects ids that collide with them.
     */
    public static final Set<String> RESERVED_TOKENS = Set.of(EMPTY_TOKEN, ARMORED_TOKEN,
            SWARM_TOKEN, LINE_TOKEN, FLANK_TOKEN, COLUMN_TOKEN, DRIP_TOKEN,
            GRUNT_TOKEN, SOLDIER_TOKEN, VETERAN_TOKEN, ELITE_TOKEN, BOSS_TOKEN);

    private WaveScript() {
    }

    /** Whether {@code id} is a reserved keyword or has the shape of a spacing token, so no enemy may take it. */
    public static boolean isReserved(String id) {
        return RESERVED_TOKENS.contains(id) || SPACING_TOKEN.matcher(id).matches();
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
        float delayTicksPerSlot = SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT;
        boolean firstToken = true;

        for (String token : tokens) {
            // "".split(" ") yields a blank, as does a run of spaces.
            if (token.isBlank()) {
                continue;
            }
            boolean leading = firstToken;
            firstToken = false;
            if (SPACING_TOKEN.matcher(token).matches()) {
                if (!leading) {
                    throw new GameStartupException("Spacing token '" + token + "' must be the first token of the wave");
                }
                delayTicksPerSlot = Float.parseFloat(token.substring(SPACING_PREFIX.length()));
                if (delayTicksPerSlot <= 0f) {
                    throw new GameStartupException("Spacing token '" + token + "' must be greater than zero");
                }
            } else if (isRankToken(token)) {
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
        return new WaveContent(spawnSequence, delayTicksPerSlot);
    }

    /**
     * An explicit count before the current token wins; otherwise a count captured before a rank
     * keyword carries forward.
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
