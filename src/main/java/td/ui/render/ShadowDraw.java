package td.ui.render;

/**
 * The Sundial's shadow from its stone along {@code headingRadians}, {@code reach} long, in the band of sun-lit ground
 * round it, faded by {@code alpha}; {@code cellPixels} is a cell's size on the board. Inside the stone's field,
 * {@code fieldRadius} round it, an {@code inFieldUmbra} stone lights the band darker.
 */
public record ShadowDraw(float originX, float originY, float headingRadians, float reach, float cellPixels,
                         float alpha, float fieldRadius, boolean inFieldUmbra) implements TowerEffectDraw {
}
