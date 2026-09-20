package td.level;

import td.enemy.Rank;
import td.wave.PathColor;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.List;

/**
 * Two lanes cross a small, tight board - fuchsia loops back on itself in a fish-hook before
 * cutting across to its exit, lime sweeps diagonally corner to corner through a rounded bulge,
 * and the two cross twice along the way. A 16x11 board, 8 waves per lane, starting with $75 and
 * only 3 lives.
 */
final class ZigZagPathLevel {

    // cornerPull=0.4 rounds every corner noticeably more than Curly Path's 0.3 - this level's
    // own tighter, rounder character - while staying safely under half of every adjacent leg:
    // the shortest legs here are 2 cells (64px), and two corners sharing one such leg each pull
    // back 0.4*64=25.6px, leaving a comfortable ~13px gap between them.
    private static final ArcCornerSmoothing SMOOTHING = new ArcCornerSmoothing(0.4, 10);

    // Fuchsia enters near the top-left corner and exits near the bottom-right one, spanning
    // nearly the board's full 11-row height on its way across - the wide, flattened loop sits
    // left-of-center, well below the entry line, its open end pointing back to the right.
    private static final PathDefinition FUCHSIA_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(-1, 2), new Point(13, 2), new Point(13, 5), new Point(2, 5),
                            new Point(2, 8), new Point(16, 8)),
                    List.of(
                            new WaveDefinition("c e c e c e c", Rank.GRUNT),
                            new WaveDefinition("5 c", Rank.GRUNT),
                            new WaveDefinition("s e s e s", Rank.SOLDIER),
                            new WaveDefinition("t e t e t e t", Rank.SOLDIER),
                            new WaveDefinition("3 s 2 e 3 c", Rank.SOLDIER),
                            new WaveDefinition("g e g e g", Rank.VETERAN),
                            new WaveDefinition("2 g 2 t 2 s 2 c", Rank.VETERAN),
                            new WaveDefinition("10 c e 5 s e 3 t e g", Rank.ELITE)),
                    SMOOTHING)
            .withColor(PathColor.of(230, 60, 200));

    // Placeholder content only - hand-authored waves for this brand-new second lane come later;
    // this exists to satisfy LevelDefinition's equal-round-count check and give something to
    // playtest the crossing layout against in the meantime.
    private static final PathDefinition LIME_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(12, -1), new Point(12, 2), new Point(9, 10), new Point(7, 10),
                            new Point(3, 2), new Point(3, -1)),
                    List.of(
                            new WaveDefinition("c e c e c", Rank.GRUNT),
                            new WaveDefinition("5 c", Rank.GRUNT),
                            new WaveDefinition("s e s e s", Rank.SOLDIER),
                            new WaveDefinition("t e t e t", Rank.SOLDIER),
                            new WaveDefinition("3 s 2 e 3 c", Rank.SOLDIER),
                            new WaveDefinition("g e g e g", Rank.VETERAN),
                            new WaveDefinition("2 g 2 t 2 s", Rank.VETERAN),
                            new WaveDefinition("10 c e 5 s e g", Rank.ELITE)),
                    SMOOTHING)
            .withColor(PathColor.of(170, 230, 40));

    static final LevelDefinition DEFINITION = LevelDefinition
            .of("Zigzag Path", 16, 11, List.of(FUCHSIA_PATH, LIME_PATH))
            .withDescription("Two lanes weave through the same cramped arena - fuchsia loops back on itself "
                    + "before lime cuts clean across it. 8 waves per lane, starting with $75 and only 3 lives.")
            .withStartingCredits(75)
            .withStartingLives(3);

    private ZigZagPathLevel() {
    }
}
