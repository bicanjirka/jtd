package td.ui.render;

/** A tower's body sprite, plus its selection range ring when {@code selected}. */
public record TowerSpriteDraw(Palette palette, int boardX, int boardY, boolean selected,
                               float centerX, float centerY, float rangeReal) {
}
