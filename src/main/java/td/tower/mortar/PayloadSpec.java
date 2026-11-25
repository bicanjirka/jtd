package td.tower.mortar;

/**
 * How a payload shell's blast and ground grow past their own.
 *
 * @param extraCells    cells its blast, and its ground with it, reach further
 * @param extraTicks    ticks its ground lasts longer
 * @param normalsCarry  the share of a pulse of its ground that the blast of a Normal right behind it puts on what it
 *                      reaches; 0 for none
 */
public record PayloadSpec(float extraCells, int extraTicks, float normalsCarry) {

    public static PayloadSpec none() {
        return new PayloadSpec(0f, 0, 0f);
    }

    public PayloadSpec wider(float cells) {
        return new PayloadSpec(this.extraCells + cells, this.extraTicks, this.normalsCarry);
    }

    public PayloadSpec longer(int ticks) {
        return new PayloadSpec(this.extraCells, this.extraTicks + ticks, this.normalsCarry);
    }

    /** Normal shells' blasts carrying {@code share} more of a pulse of the payload's ground. */
    public PayloadSpec carriedByNormals(float share) {
        return new PayloadSpec(this.extraCells, this.extraTicks, this.normalsCarry + share);
    }
}
