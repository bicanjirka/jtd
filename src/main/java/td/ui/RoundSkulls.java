package td.ui;

import td.ui.render.IconFace;
import td.ui.render.Palette;
import td.wave.RoundSummary;

import java.util.Locale;

/** How a round's skull looks: its art by the strongest enemy it sends, its colour by how it stands. Pure. */
final class RoundSkulls {

    /** Which skull a round shows: one per rank above Soldier, and one above them all for a unique enemy. */
    enum Kind {
        PLAIN,
        VETERAN,
        ELITE,
        BOSS,
        UNIQUE;

        static Kind of(RoundSummary round) {
            if (round.unique()) {
                return UNIQUE;
            }
            return switch (round.highestRank()) {
                case GRUNT, SOLDIER -> PLAIN;
                case VETERAN -> VETERAN;
                case ELITE -> ELITE;
                case BOSS -> BOSS;
            };
        }
    }

    private RoundSkulls() {
    }

    /** The art's key, cracked once the round is over. */
    static String pictogram(Kind kind, RoundStrip.State state) {
        String skull = switch (kind) {
            case PLAIN -> StatusPictograms.SKULL;
            case VETERAN -> StatusPictograms.SKULL_VETERAN;
            case ELITE -> StatusPictograms.SKULL_ELITE;
            case BOSS -> StatusPictograms.SKULL_BOSS;
            case UNIQUE -> StatusPictograms.SKULL_UNIQUE;
        };
        return state.isOver() ? skull + StatusPictograms.CRACKED : skull;
    }

    static Palette tone(Kind kind, RoundStrip.State state) {
        return switch (state) {
            case CLEARED -> Palette.ROUND_CLEARED;
            case LEAKED -> Palette.ROUND_LEAKED;
            case AHEAD, CURRENT -> kindTone(kind);
        };
    }

    static Palette kindTone(Kind kind) {
        return switch (kind) {
            case PLAIN -> Palette.ROUND_SKULL;
            case VETERAN -> Palette.ROUND_SKULL_VETERAN;
            case ELITE -> Palette.ROUND_SKULL_ELITE;
            case BOSS -> Palette.ROUND_SKULL_BOSS;
            case UNIQUE -> Palette.ROUND_SKULL_UNIQUE;
        };
    }

    /** Round {@code round}'s face on a sheet, its 1-based number under it. */
    static IconFace face(RoundStrip strip, int round) {
        Kind kind = strip.kindAt(round);
        RoundStrip.State state = strip.stateAt(round);
        Palette frame = state == RoundStrip.State.CURRENT ? Palette.ROUND_CURRENT : Palette.TERM_FRAME_STATUS;
        return IconFace.of(pictogram(kind, state), Integer.toString(round + 1), tone(kind, state), frame)
                .withBelow(Integer.toString(round + 1));
    }

    /** What a round's tooltip calls its strongest enemy: its rank, and a unique enemy as such. */
    static String strongest(RoundSummary round) {
        if (round.unique()) {
            return "Unique boss";
        }
        String name = round.highestRank().name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    static String stateName(RoundStrip.State state) {
        return switch (state) {
            case AHEAD -> "to come";
            case CURRENT -> "now";
            case CLEARED -> "cleared";
            case LEAKED -> "enemies escaped";
        };
    }
}
