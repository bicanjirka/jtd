package td.ui;

import td.enemy.Rank;
import td.ui.render.IconFace;
import td.ui.render.Palette;

import java.util.Locale;

/** How a round's skull looks: its art by the highest rank it sends, its colour by how it stands. Pure. */
final class RoundSkulls {

    private RoundSkulls() {
    }

    /** The art's key: one skull per rank above Soldier, cracked once the round is over. */
    static String pictogram(Rank rank, RoundStrip.State state) {
        String skull = switch (rank) {
            case GRUNT, SOLDIER -> StatusPictograms.SKULL;
            case VETERAN -> StatusPictograms.SKULL_VETERAN;
            case ELITE -> StatusPictograms.SKULL_ELITE;
            case BOSS -> StatusPictograms.SKULL_BOSS;
        };
        return switch (state) {
            case AHEAD, CURRENT -> skull;
            case CLEARED, LEAKED -> skull + StatusPictograms.CRACKED;
        };
    }

    static Palette tone(Rank rank, RoundStrip.State state) {
        return switch (state) {
            case CLEARED -> Palette.ROUND_CLEARED;
            case LEAKED -> Palette.ROUND_LEAKED;
            case AHEAD, CURRENT -> rankTone(rank);
        };
    }

    static Palette rankTone(Rank rank) {
        return switch (rank) {
            case GRUNT, SOLDIER -> Palette.ROUND_SKULL;
            case VETERAN -> Palette.ROUND_SKULL_VETERAN;
            case ELITE -> Palette.ROUND_SKULL_ELITE;
            case BOSS -> Palette.ROUND_SKULL_BOSS;
        };
    }

    /** Round {@code round}'s face on a sheet, its 1-based number under it. */
    static IconFace face(RoundStrip strip, int round) {
        Rank rank = strip.rankAt(round);
        RoundStrip.State state = strip.stateAt(round);
        Palette frame = state == RoundStrip.State.CURRENT ? Palette.ROUND_CURRENT : Palette.TERM_FRAME_STATUS;
        return IconFace.of(pictogram(rank, state), Integer.toString(round + 1), tone(rank, state), frame)
                .withBelow(Integer.toString(round + 1));
    }

    static String rankName(Rank rank) {
        String name = rank.name().toLowerCase(Locale.ROOT);
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
