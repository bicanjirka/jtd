package td.board;

/**
 * The light a level's board stands in, fixed for the level, as plain {@code 0xRRGGBB}: the key light that rims what
 * stands, the colour its shaded sides fall toward, and the air the far (north) edge of the board hazes into.
 * {@link #house()} is the light of a level that names none: white key, black shade, no haze colour of its own.
 */
public record BoardLight(int keyRgb, int shadowRgb, int hazeRgb) {

    private static final BoardLight HOUSE = new BoardLight(0xFFFFFF, 0x000000, 0x000000);

    public static BoardLight house() {
        return HOUSE;
    }

    public static BoardLight of(int keyRgb, int shadowRgb, int hazeRgb) {
        return new BoardLight(keyRgb, shadowRgb, hazeRgb);
    }
}
