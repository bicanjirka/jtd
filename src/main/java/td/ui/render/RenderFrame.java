package td.ui.render;

import java.util.List;

/**
 * Everything needed to draw one frame of the game board, described without any
 * {@code java.awt} dependency. {@link td.ui.BoardRenderer} builds one of these each
 * frame; a backend (e.g. {@link td.ui.Java2DFrameRenderer}) turns it into pixels.
 */
public record RenderFrame(int scale, int maxX, int maxY,
                           List<CellDraw> cells,
                           List<EnemyDraw> enemies,
                           List<TowerSpriteDraw> towerSprites,
                           List<TowerEffectDraw> towerEffects,
                           List<PathMarkerDraw> pathMarkers) {
}
