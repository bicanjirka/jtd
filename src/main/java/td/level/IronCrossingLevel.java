package td.level;

import td.board.BoardGround;
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
    private static final int STARTING_CREDITS = 90;
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
                            new WaveDefinition("4 walker e 2 knight e 4 walker"),
                            new WaveDefinition("3 knight e 5 walker e golem"),
                            new WaveDefinition("column 3 knight e warding armored knight 4 walker"),
                            new WaveDefinition("soldier: golem e mender 2 knight e relay 3 walker"),
                            new WaveDefinition("soldier: 3 steadfast knight e golem e 4 walker"),
                            new WaveDefinition("soldier: magma e warding armored knight 3 knight e 4 walker"),
                            new WaveDefinition("soldier: fireproof juggernaut e line 3 knight e yeti"),
                            new WaveDefinition("soldier: 2 golem e mender 2 knight e column 6 walker"),
                            new WaveDefinition("veteran: magma e warding armored knight column 3 knight e 2 coldblooded salamander"),
                            new WaveDefinition("veteran: plated golem e juggernaut e column 6 walker"),
                            new WaveDefinition("veteran: juggernaut e relay e line 3 knight e mender"),
                            new WaveDefinition("veteran: 2 yeti e warding armored knight armored knight e golem"),
                            new WaveDefinition("veteran: elite magma e commanding knight column 3 knight e mender e 6 walker"),
                            new WaveDefinition("veteran: elite unstoppable juggernaut warding armored knight e elite golem e armored knight e column 8 walker"),
                            new WaveDefinition("veteran: ironColossus 2 e juggernaut warding armored knight mender e elite knight e column 6 walker")),
                    SMOOTHING)
            .withColor(STEEL_COLOR);

    /** The rush lane: short and quick, so whatever guards it has to strike hard and early. */
    private static final PathColor RUSH_COLOR = PathColor.of(235, 95, 45);
    private static final PathDefinition RUSH_PATH = PathDefinition.smoothed(
                    List.of(new Point(8, -1), new Point(8, 3), new Point(11, 8), new Point(11, 13)),
                    List.of(
                            new WaveDefinition("3 runner e courier"),
                            new WaveDefinition("drip 3 courier e 2 runner"),
                            new WaveDefinition("swarm 8 mite e 2 runner"),
                            new WaveDefinition("soldier: skirmisher e drip 3 courier"),
                            new WaveDefinition("soldier: 2 ghost e 3 runner"),
                            new WaveDefinition("soldier: swarm 12 mite e skirmisher"),
                            new WaveDefinition("soldier: drip 4 courier e 3 runner e ghost"),
                            new WaveDefinition("soldier: line 3 skirmisher e flank runner"),
                            new WaveDefinition("soldier: swarm 12 mite e ghost e 3 runner"),
                            new WaveDefinition("veteran: drip 5 courier e line 3 skirmisher"),
                            new WaveDefinition("veteran: elite runner e swarm 12 mite e 2 soldier ghost"),
                            new WaveDefinition("veteran: line 3 skirmisher e elite ghost e swarm 12 mite"),
                            new WaveDefinition("veteran: drip 6 courier e elite withered runner"),
                            new WaveDefinition("veteran: swarm 12 mite elite vengeful ghost swarm 12 mite e line 3 skirmisher"),
                            new WaveDefinition("veteran: boss runner e elite swarm 12 mite e elite skirmisher e 2 elite ghost")),
                    SMOOTHING)
            .withColor(RUSH_COLOR);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 18, 13, List.of(STEEL_PATH, RUSH_PATH))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES)
            .withCardColor(RUSH_COLOR)
            .withGround(BoardGround.of(0x08090C, GroundMotif.PLATES, 0x14171D))
            .withCustomEnemies(List.of(ColossusEnemy.ember()))
            .withCustomRankedEnemies(ColossusEnemy.chain());

    private IronCrossingLevel() {
    }
}
