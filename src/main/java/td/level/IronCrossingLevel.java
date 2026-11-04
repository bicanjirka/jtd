package td.level;

import td.board.BoardGround;
import td.board.GroundMotif;
import td.enemy.Rank;
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
                            new WaveDefinition("4 simple e 2 armored e 4 simple", Rank.GRUNT),
                            new WaveDefinition("3 armored e 5 simple e golem", Rank.GRUNT),
                            new WaveDefinition("column 3 armored e shield 4 simple", Rank.GRUNT),
                            new WaveDefinition("golem e mender 2 armored e 5 simple", Rank.SOLDIER),
                            new WaveDefinition("3 armored e golem e 4 simple", Rank.SOLDIER),
                            new WaveDefinition("magma e shield 3 armored e 4 simple", Rank.SOLDIER),
                            new WaveDefinition("titan e line 3 armored e yeti", Rank.SOLDIER),
                            new WaveDefinition("2 golem e mender 2 armored e column 6 simple", Rank.SOLDIER),
                            new WaveDefinition("magma e shield column 3 armored e 2 lizard", Rank.VETERAN),
                            new WaveDefinition("golem e titan e column 6 simple", Rank.VETERAN),
                            new WaveDefinition("titan e jammer e line 3 armored e mender", Rank.VETERAN),
                            new WaveDefinition("2 yeti e shield plated armored e golem", Rank.VETERAN),
                            new WaveDefinition("elite magma e column 4 armored e mender e 6 simple", Rank.VETERAN),
                            new WaveDefinition("elite titan shield e elite golem e plated armored e column 8 simple", Rank.VETERAN),
                            new WaveDefinition("colossus 2 e titan shield mender e elite armored e column 6 simple", Rank.VETERAN)),
                    SMOOTHING)
            .withColor(STEEL_COLOR);

    /** The rush lane: short and quick, so whatever guards it has to strike hard and early. */
    private static final PathColor RUSH_COLOR = PathColor.of(235, 95, 45);
    private static final PathDefinition RUSH_PATH = PathDefinition.smoothed(
                    List.of(new Point(8, -1), new Point(8, 3), new Point(11, 8), new Point(11, 13)),
                    List.of(
                            new WaveDefinition("3 fast e courier", Rank.GRUNT),
                            new WaveDefinition("drip 3 courier e 2 fast", Rank.GRUNT),
                            new WaveDefinition("swarm 8 mite e 2 fast", Rank.GRUNT),
                            new WaveDefinition("dodger e drip 3 courier", Rank.SOLDIER),
                            new WaveDefinition("2 ghost e 3 fast", Rank.SOLDIER),
                            new WaveDefinition("swarm 12 mite e dodger", Rank.SOLDIER),
                            new WaveDefinition("drip 4 courier e 3 fast e ghost", Rank.SOLDIER),
                            new WaveDefinition("line 3 dodger e flank fast", Rank.SOLDIER),
                            new WaveDefinition("swarm 12 mite e ghost e 3 fast", Rank.SOLDIER),
                            new WaveDefinition("drip 5 courier e line 3 dodger", Rank.VETERAN),
                            new WaveDefinition("elite fast e swarm 12 mite e 2 soldier ghost", Rank.VETERAN),
                            new WaveDefinition("line 3 dodger e elite ghost e swarm 12 mite", Rank.VETERAN),
                            new WaveDefinition("drip 6 courier e elite fast", Rank.VETERAN),
                            new WaveDefinition("swarm 12 mite elite ghost swarm 12 mite e line 3 dodger", Rank.VETERAN),
                            new WaveDefinition("boss fast e elite swarm 12 mite e elite dodger e 2 elite ghost", Rank.VETERAN)),
                    SMOOTHING)
            .withColor(RUSH_COLOR);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 18, 13, List.of(STEEL_PATH, RUSH_PATH))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES)
            .withGround(BoardGround.of(0x08090C, GroundMotif.PLATES, 0x14171D))
            .withCustomEnemies(List.of(ColossusEnemy.ember()))
            .withCustomRankedEnemies(ColossusEnemy.chain());

    private IronCrossingLevel() {
    }
}
