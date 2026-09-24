package td.ui.render;

/** A small filled marker in a tower's corner for a state it is in, such as being disrupted. */
public record TowerStatusDraw(Palette palette, float x, float y, float scale) implements TowerEffectDraw {
}
