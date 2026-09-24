package td.ui.render;

/** A filled disc over a tower's range while an area tower fires. */
public record PulseDraw(Palette palette, float centerX, float centerY, float radius) implements TowerEffectDraw {
}
