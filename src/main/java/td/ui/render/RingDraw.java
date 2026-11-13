package td.ui.render;

/** An expanding, fading ring: an aura's breath out to its buff range. */
public record RingDraw(Palette palette, float centerX, float centerY, float radius,
                       float alpha) implements TowerEffectDraw {
}
