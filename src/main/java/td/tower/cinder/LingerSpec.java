package td.tower.cinder;

/** Lingering Flames: each wave leaves a patch of burning ground where its target stands. */
public record LingerSpec(float radiusCells, int ticks) {

    public static LingerSpec none() {
        return new LingerSpec(0f, 0);
    }

    public static LingerSpec of(float radiusCells, int ticks) {
        return new LingerSpec(radiusCells, ticks);
    }

    public boolean isActive() {
        return this.ticks > 0;
    }
}
