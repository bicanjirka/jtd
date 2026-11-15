package td.ui.render;

/**
 * The Sundial's veil from its stone: a wedge along {@code headingRadians}, {@code rootHalfWidth} wide at the stone and
 * {@code tipHalfWidth} at {@code reach}, with a lighter line {@code edgeWidth} wide along its leading edge. A darker
 * veil {@code inFieldUmbra} draws over it inside the stone's field, {@code fieldRadius} round it.
 */
public record ShadowDraw(float originX, float originY, float headingRadians, float reach, float rootHalfWidth,
                         float tipHalfWidth, float edgeWidth, float alpha, float fieldRadius, boolean inFieldUmbra)
        implements TowerEffectDraw {
}
