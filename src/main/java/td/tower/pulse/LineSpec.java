package td.tower.pulse;

/**
 * The Ley Line a field casts.
 *
 * @param reachCells how far, centre to centre in cells, the field it links to may stand; {@code 0} for no line
 * @param bandCells  how near the beam an enemy must be to stand on it
 */
public record LineSpec(float reachCells, float bandCells) {

    /** No line. */
    public static LineSpec none() {
        return new LineSpec(0f, 0f);
    }

    public boolean casts() {
        return this.reachCells > 0f;
    }
}
