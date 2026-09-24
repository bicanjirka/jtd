package td.ui;

import td.TowerDefense;
import td.util.GameWorld;

import javax.swing.GroupLayout;
import javax.swing.JPanel;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.Serial;

/**
 * The board component. {@link #paint} skips {@code super.paint()} and hands the graphics to the
 * frame painter, whose background fill clears the previous frame.
 */
public class GameBoard extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;

    private final TowerDefense game;
    private final GameWorld context;

    public GameBoard(TowerDefense game, GameWorld context) {
        this.game = game;
        this.context = context;
        initComponents();
    }

    public void paint(Graphics g) {
        this.game.paintBoard((Graphics2D) g);
    }

    /** Resizes the board and window for a level of {@code width} x {@code height} cells. */
    public void recalculateBoard(int width, int height) {
        int scale = this.context.getBoard().scale();
        int realW = width * scale;
        int realH = height * scale;
        // 210 = the info column's natural width, 93 = the tower-buttons panel's natural height
        game.setSize(realW + 210 + scale, realH + 93 + scale);
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, realW, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, realH, Short.MAX_VALUE)
        );
    }

    private void initComponents() {
        this.setDoubleBuffered(true);
    }

}
