package td.ui.render;

/** Toll round an enemy: a ring of segments, {@code filled} of them lit, bolder once {@code full}. */
public record TollMarkDraw(float x, float y, float radius, int filled, boolean full, float alpha)
        implements EnemyOverlayDraw {
}
