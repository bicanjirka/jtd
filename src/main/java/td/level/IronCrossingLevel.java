package td.level;

import td.board.BoardGround;
import td.board.BoardLight;
import td.board.GroundMotif;
import td.wave.PathColor;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.List;

final class IronCrossingLevel {

    private static final String LEVEL_NAME = "Iron Crossing";
    private static final String LEVEL_DESCRIPTION = "A long iron road winds the heavy ranks back and forth across the board, "
            + "while a short rush lane knifes straight through it three times.";
    private static final int STARTING_CREDITS = 50;
    private static final int STARTING_LIVES = 3;

    private static final ArcCornerSmoothing SMOOTHING = new ArcCornerSmoothing(0.3, 10);

    /** The heavy lane: long, so armor and health have to be worn down along all of it. */
    private static final PathColor STEEL_COLOR = PathColor.of(110, 140, 190);
    private static final PathDefinition STEEL_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(-1, 1), new Point(15, 1), new Point(16, 2), new Point(16, 5),
                            new Point(15, 6), new Point(3, 6), new Point(2, 7), new Point(2, 10),
                            new Point(3, 11), new Point(18, 11)),
                    List.of(
                            new WaveDefinition("3 walker e knight e 3 walker"),
                            new WaveDefinition("soldier (knight e 3 walker)"),
                            new WaveDefinition("soldier (column 2 knight e warding armored knight 2 walker)"),
                            new WaveDefinition("veteran (golem e 3 knight)"),
                            new WaveDefinition("veteran (2 steadfast knight e templar e 2 walker)"),
                            new WaveDefinition("veteran (magma e warding armored knight 3 knight e column 4 walker)"),
                            new WaveDefinition("veteran (fireproof juggernaut e line 3 knight e 2 yeti e column 4 walker)"),
                            new WaveDefinition("veteran (2 golem e mender 2 knight e relay e yeti e column 6 walker)"),
                            new WaveDefinition("veteran (magma e warding armored knight column 3 knight e 2 coldblooded salamander e 2 golem e elite knight)"),
                            new WaveDefinition("veteran (2 plated golem e 2 juggernaut e 2 armored knight e 2 elite walker e column 6 walker e elite knight e 2 knight)"),
                            new WaveDefinition("veteran (2 elite juggernaut e relay e line 3 knight e templar mender e 2 juggernaut)"),
                            new WaveDefinition("veteran (2 elite yeti e warding armored knight armored knight e 2 elite golem e column 6 walker)"),
                            new WaveDefinition("veteran (elite magma e commanding knight column 3 knight e mender e 4 templar e elite templar e 3 elite knight e 2 elite golem)"),
                            new WaveDefinition("veteran (2 elite unstoppable juggernaut warding armored knight e 3 elite golem e 2 elite armored knight e column 10 walker)"),
                            new WaveDefinition("veteran (ironColossus 2 e 2 elite juggernaut warding armored knight mender e elite knight e 2 elite golem e column 6 walker)")),
                    SMOOTHING)
            .withColor(STEEL_COLOR);

    /** The rush lane: short and quick, so whatever guards it has to strike hard and early. */
    private static final PathColor RUSH_COLOR = PathColor.of(235, 95, 45);
    private static final PathDefinition RUSH_PATH = PathDefinition.smoothed(
                    List.of(new Point(8, -1), new Point(8, 3), new Point(11, 8), new Point(11, 13)),
                    List.of(
                            new WaveDefinition("2 runner e courier"),
                            new WaveDefinition("soldier (2 runner)"),
                            new WaveDefinition("soldier (swarm 6 mite e 2 runner)"),
                            new WaveDefinition("soldier (skirmisher e drip 2 courier e runner)"),
                            new WaveDefinition("soldier (2 ghost e 2 runner)"),
                            new WaveDefinition("soldier (swarm 12 mite e 2 skirmisher e 2 veteran runner)"),
                            new WaveDefinition("veteran (drip 3 courier e 3 runner e 2 ghost)"),
                            new WaveDefinition("veteran (line 3 skirmisher e flank runner e 2 runner)"),
                            new WaveDefinition("veteran (swarm 12 mite e 2 ghost e 4 runner)"),
                            new WaveDefinition("veteran (drip 5 courier e line 4 skirmisher e 2 runner e 2 elite runner)"),
                            new WaveDefinition("veteran (elite runner e swarm 12 mite e 3 ghost e line 3 skirmisher)"),
                            new WaveDefinition("veteran (line 3 skirmisher e elite ghost e swarm 12 mite e 3 runner e 2 elite runner)"),
                            new WaveDefinition("veteran (drip 6 courier e 2 elite withered runner e line 3 skirmisher e 2 elite skirmisher)"),
                            new WaveDefinition("veteran (swarm 12 mite elite vengeful ghost swarm 12 mite e line 4 skirmisher e 3 elite runner)"),
                            new WaveDefinition("veteran (boss runner e elite swarm 12 mite e elite skirmisher e elite ghost e 2 elite runner)")),
                    SMOOTHING)
            .withColor(RUSH_COLOR);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 18, 13, List.of(STEEL_PATH, RUSH_PATH))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES)
            .withCardColor(RUSH_COLOR)
            .withGround(BoardGround.of(0x24262A, GroundMotif.PLATES, 0x2F3237)
                    .withLight(BoardLight.of(0xFFD8B0, 0x101828, 0x5A5650)).withHaze(0.2f))
            .withCustomEnemies(List.of(ColossusEnemy.ember()))
            .withCustomRankedEnemies(ColossusEnemy.chain());

    private IronCrossingLevel() {
    }
}
