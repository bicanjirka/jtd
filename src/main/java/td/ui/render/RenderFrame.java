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
                           List<StatusMarkerDraw> statusMarkers,
                           List<TowerSpriteDraw> towerSprites,
                           List<TurretHeadDraw> towerHeads,
                           List<TowerEffectDraw> towerEffects,
                           List<ProjectileDraw> projectiles,
                           List<PathMarkerDraw> pathMarkers) {
}
