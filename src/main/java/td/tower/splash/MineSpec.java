package td.tower.splash;

/**
 * Mines: every {@code every}th blast leaves a mine on the path where it landed, which the next enemy to
 * step within {@code radiusCells} of sets off as one blast. A Splash keeps at most {@code max} of them.
 */
public record MineSpec(int every, int max, int lifetimeTicks, float radiusCells) {

    public static MineSpec none() {
        return new MineSpec(0, 0, 0, 0f);
    }

    public static MineSpec of(int every, int max, int lifetimeTicks, float radiusCells) {
        return new MineSpec(every, max, lifetimeTicks, radiusCells);
    }

    public boolean isActive() {
        return this.every > 0;
    }
}
