package td.board;

/**
 * What a level's board is painted on and lit by: a floor colour, a motif drawn faintly over it in a second colour (both
 * plain {@code 0xRRGGBB}, so a frame can carry them without AWT), the level's {@link BoardLight}, and how far the floor
 * leans toward the light's haze at its far (north) edge, from 0 (not at all) to 1 (all haze). {@link #plain()} is the
 * black floor with no motif under the house light.
 */
public record BoardGround(int floorRgb, GroundMotif motif, int motifRgb, BoardLight light, float haze) {

    private static final BoardGround PLAIN = of(0x000000, GroundMotif.NONE, 0x000000);

    public static BoardGround plain() {
        return PLAIN;
    }

    /** A floor of its own colour, with no motif. */
    public static BoardGround floor(int floorRgb) {
        return of(floorRgb, GroundMotif.NONE, floorRgb);
    }

    /** A floor with its motif, under the house light and with no haze. */
    public static BoardGround of(int floorRgb, GroundMotif motif, int motifRgb) {
        return new BoardGround(floorRgb, motif, motifRgb, BoardLight.house(), 0f);
    }

    public BoardGround withLight(BoardLight light) {
        return new BoardGround(this.floorRgb, this.motif, this.motifRgb, light, this.haze);
    }

    public BoardGround withHaze(float haze) {
        return new BoardGround(this.floorRgb, this.motif, this.motifRgb, this.light, haze);
    }

    /** The floor at its far (north) edge: the floor leant toward the haze. */
    public int farFloorRgb() {
        return mix(this.floorRgb, this.light.hazeRgb(), this.haze);
    }

    private static int mix(int from, int to, float share) {
        int r = Math.round((from >> 16 & 0xff) + ((to >> 16 & 0xff) - (from >> 16 & 0xff)) * share);
        int g = Math.round((from >> 8 & 0xff) + ((to >> 8 & 0xff) - (from >> 8 & 0xff)) * share);
        int b = Math.round((from & 0xff) + ((to & 0xff) - (from & 0xff)) * share);
        return r << 16 | g << 8 | b;
    }
}
