package td.ui.render;

/** An expanding, fading ring - the upgrade tower's passive "buff aura", sized to its real buff range. */
public record AuraDraw(Palette palette, float centerX, float centerY, float radius, float alpha) implements TowerEffectDraw {
}
