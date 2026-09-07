package td.ui;

import td.GameEngine;
import td.cell.Cell;
import td.enemy.EnemyMob;
import td.tower.Tower;
import td.util.Context;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Draws one frame of the game board: background, cell highlights, enemies,
 * and towers. The one place that owns the per-object painter dispatch, so
 * TowerDefense/GameBoard no longer need to know how any domain type is
 * drawn.
 */
public final class BoardRenderer {

    private final GameEngine engine;
    private final Context context;

    public BoardRenderer(GameEngine engine, Context context) {
        this.engine = engine;
        this.context = context;
    }

    public void paint(Graphics2D g2, BufferedImage background, int gameTime) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        g2.drawImage(background, 0, 0, null);

        Cell[][] cellGrid = this.engine.getCellGrid();
        if (cellGrid != null) {
            CellRenderer cellRenderer = new CellRenderer(g2, this.context);
            for (Cell[] cells : cellGrid) {
                for (int j = 0; j < cellGrid[0].length; j++) {
                    cellRenderer.paint(cells[j]);
                }
            }
        }

        EnemyPainter enemyPainter = new EnemyPainter(g2, gameTime);
        for (EnemyMob enemy : this.context.getEnemies()) {
            enemy.accept(enemyPainter);
        }

        TowerSpritePainter spritePainter = new TowerSpritePainter(g2);
        for (Tower tower : this.engine.getTowers()) {
            tower.accept(spritePainter);
        }

        TowerEffectPainter effectPainter = new TowerEffectPainter(g2);
        for (Tower tower : this.engine.getTowers()) {
            tower.accept(effectPainter);
        }
    }
}
