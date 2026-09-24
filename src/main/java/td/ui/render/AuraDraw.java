package td.ui.render;

/** An expanding, fading ring at an aura's buff range. */
public record AuraDraw(Palette palette, float centerX, float centerY, float radius,
                       float alpha) implements TowerEffectDraw {
}
