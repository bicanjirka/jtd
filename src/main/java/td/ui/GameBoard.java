package td.ui;

import td.TowerDefence;
import td.util.Context;

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class GameBoard extends JPanel {
    private static final long serialVersionUID = 1L;

    private final TowerDefence game;
    private final Context context;

    public GameBoard(TowerDefence game, Context context) {
        this.game = game;
        this.context = context;
        initComponents();
    }

    public void paint(Graphics g) {
        this.game.paintBoard((Graphics2D) g);
    }

    public void recalculateBoard(int width, int height) {
        int scale = this.context.scale;
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
