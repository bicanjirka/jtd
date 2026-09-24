package td.ui.render;

import java.util.List;
import java.util.Optional;

/**
 * Everything needed to draw one frame of the board, without AWT, plus the selected enemy's
 * inspector sheet, so the EDT never reads a live mob.
 */
public record RenderFrame(int scale, int maxX, int maxY,
                          List<CellDraw> cells,
                          List<EnemyDraw> enemies,
                          List<StatusMarkerDraw> statusMarkers,
                          List<CritSparkDraw> critSparks,
                          List<TowerSpriteDraw> towerSprites,
                          List<TurretHeadDraw> towerHeads,
                          List<TowerEffectDraw> towerEffects,
                          List<ProjectileDraw> projectiles,
                          List<PathMarkerDraw> pathMarkers,
                          List<EnemyOverlayDraw> enemyOverlays,
                          Optional<EnemySheet> enemyInspection) {
}
