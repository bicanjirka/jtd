package td.board;

/**
 * What a level's board is painted on: a floor colour and a motif drawn faintly over it in a second colour, both as
 * plain {@code 0xRRGGBB}, so a frame can carry them without AWT. {@link #plain()} is the black floor with no motif.
 */
public record BoardGround(int floorRgb, GroundMotif motif, int motifRgb) {

    private static final BoardGround PLAIN = new BoardGround(0x000000, GroundMotif.NONE, 0x000000);

    public static BoardGround plain() {
        return PLAIN;
    }

    /** A floor of its own colour, with no motif. */
    public static BoardGround floor(int floorRgb) {
        return new BoardGround(floorRgb, GroundMotif.NONE, floorRgb);
    }

    public static BoardGround of(int floorRgb, GroundMotif motif, int motifRgb) {
        return new BoardGround(floorRgb, motif, motifRgb);
    }
}
