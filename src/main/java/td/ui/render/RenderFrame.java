package td.ui.render;

import td.board.BoardGround;

import java.util.List;
import java.util.Optional;

/**
 * Everything needed to draw one frame of the board, without AWT, plus the selected enemy's
 * inspector sheet, so the EDT never reads a live mob. {@code cellGrid} is present only while the
 * dev panel's grid is on. Over the ground, the path marks, the cell highlight and the zone patches, {@code scene} holds
 * everything else already in paint order. {@code boardFlash} is present while a nuke's flash whitens the board, over
 * everything else.
 */
public record RenderFrame(int scale, int maxX, int maxY,
                          List<CellDraw> cells,
                          List<ZonePatchDraw> zones,
                          List<PathMarkerDraw> pathMarkers,
                          List<BoardDraw> scene,
                          Optional<InfoSheet> enemyInspection,
                          Optional<TowerSheet> towerSheet,
                          Optional<CellGridDraw> cellGrid,
                          BoardGround ground,
                          Optional<BoardFlashDraw> boardFlash) {

    /** The towers in the scene, back to front. */
    public List<TowerSpriteDraw> towerSprites() {
        return this.scene.stream().filter(TowerSpriteDraw.class::isInstance).map(TowerSpriteDraw.class::cast).toList();
    }

    /** The enemy bodies, corpses and emissions in the scene, back to front. */
    public List<EnemyDraw> enemies() {
        return this.scene.stream().filter(EnemyDraw.class::isInstance).map(EnemyDraw.class::cast).toList();
    }
}
