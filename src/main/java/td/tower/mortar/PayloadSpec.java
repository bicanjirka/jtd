package td.tower.mortar;

/**
 * How a payload shell's blast and ground grow past their own.
 *
 * @param extraCells    cells its blast, and its ground with it, reach further
 * @param extraTicks    ticks its ground lasts longer
 */
public record PayloadSpec(float extraCells, int extraTicks) {

    public static PayloadSpec none() {
        return new PayloadSpec(0f, 0);
    }

    public PayloadSpec wider(float cells) {
        return new PayloadSpec(this.extraCells + cells, this.extraTicks);
    }

    public PayloadSpec longer(int ticks) {
        return new PayloadSpec(this.extraCells, this.extraTicks + ticks);
    }
}
