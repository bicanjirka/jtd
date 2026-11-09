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
                            new WaveDefinition("4 walker e 2 knight e 4 walker", Rank.GRUNT),
                            new WaveDefinition("3 knight e 5 walker e golem", Rank.GRUNT),
                            new WaveDefinition("column 3 knight e shieldbearer 4 walker", Rank.GRUNT),
                            new WaveDefinition("golem e mender 2 knight e 5 walker", Rank.SOLDIER),
                            new WaveDefinition("3 knight e golem e 4 walker", Rank.SOLDIER),
                            new WaveDefinition("magma e shieldbearer 3 knight e 4 walker", Rank.SOLDIER),
                            new WaveDefinition("juggernaut e line 3 knight e yeti", Rank.SOLDIER),
                            new WaveDefinition("2 golem e mender 2 knight e column 6 walker", Rank.SOLDIER),
                            new WaveDefinition("magma e shieldbearer column 3 knight e 2 salamander", Rank.VETERAN),
                            new WaveDefinition("golem e juggernaut e column 6 walker", Rank.VETERAN),
                            new WaveDefinition("juggernaut e jammer e line 3 knight e mender", Rank.VETERAN),
                            new WaveDefinition("2 yeti e shieldbearer plated knight e golem", Rank.VETERAN),
                            new WaveDefinition("elite magma e column 4 knight e mender e 6 walker", Rank.VETERAN),
                            new WaveDefinition("elite juggernaut shieldbearer e elite golem e plated knight e column 8 walker", Rank.VETERAN),
                            new WaveDefinition("ironColossus 2 e juggernaut shieldbearer mender e elite knight e column 6 walker", Rank.VETERAN)),
                    SMOOTHING)
            .withColor(STEEL_COLOR);

    /** The rush lane: short and quick, so whatever guards it has to strike hard and early. */
    private static final PathColor RUSH_COLOR = PathColor.of(235, 95, 45);
    private static final PathDefinition RUSH_PATH = PathDefinition.smoothed(
                    List.of(new Point(8, -1), new Point(8, 3), new Point(11, 8), new Point(11, 13)),
                    List.of(
                            new WaveDefinition("3 runner e courier", Rank.GRUNT),
                            new WaveDefinition("drip 3 courier e 2 runner", Rank.GRUNT),
                            new WaveDefinition("swarm 8 mite e 2 runner", Rank.GRUNT),
                            new WaveDefinition("skirmisher e drip 3 courier", Rank.SOLDIER),
                            new WaveDefinition("2 ghost e 3 runner", Rank.SOLDIER),
                            new WaveDefinition("swarm 12 mite e skirmisher", Rank.SOLDIER),
                            new WaveDefinition("drip 4 courier e 3 runner e ghost", Rank.SOLDIER),
                            new WaveDefinition("line 3 skirmisher e flank runner", Rank.SOLDIER),
                            new WaveDefinition("swarm 12 mite e ghost e 3 runner", Rank.SOLDIER),
                            new WaveDefinition("drip 5 courier e line 3 skirmisher", Rank.VETERAN),
                            new WaveDefinition("elite runner e swarm 12 mite e 2 soldier ghost", Rank.VETERAN),
                            new WaveDefinition("line 3 skirmisher e elite ghost e swarm 12 mite", Rank.VETERAN),
                            new WaveDefinition("drip 6 courier e elite runner", Rank.VETERAN),
                            new WaveDefinition("swarm 12 mite elite ghost swarm 12 mite e line 3 skirmisher", Rank.VETERAN),
                            new WaveDefinition("boss runner e elite swarm 12 mite e elite skirmisher e 2 elite ghost", Rank.VETERAN)),
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
