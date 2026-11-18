package td.ui.render;

/**
 * A spent cartridge thrown from a rifle's breech, {@code progress} 0..1 through its flight, for the rifle standing at
 * ({@code centerX}, {@code centerY}) aimed along {@code headingRadians}, its breech {@code breechCells} high;
 * {@code cellPixels} is a cell's size on the board.
 */
public record CasingDraw(float centerX, float centerY, float cellPixels, float headingRadians, float breechCells,
                         float progress) implements TowerEffectDraw {
}
