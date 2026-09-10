package td.ui.render;

/** A filled disc covering a tower's whole range, shown while an area tower is actively firing. */
public record PulseDraw(Palette palette, float centerX, float centerY, float radius) implements TowerEffectDraw {
}
