package td.ui;

import td.TowerDefence;
import td.util.Context;

import javax.swing.*;
import java.awt.*;

/**
 * Herni plocha na ktere se odehrava veskere akcni deni hry
 *
 * @author Juras
 *
 */
@SuppressWarnings("serial")
public class GameBoard extends JPanel {

    private final TowerDefence game;
    private final Context context;

    /**
     * Konstruktor herniho planu<br>
     * Ulozi ukazatel na herni kontext a hlavni aplikaci
     *
     * @param game    - kontext
     * @param context - hlavni aplikace
     */
    public GameBoard(TowerDefence game, Context context) {
        this.game = game;
        this.context = context;
        initComponents();
    }

    public void paint(Graphics g) {
        //super.paint(g);
        this.game.paintBoard((Graphics2D) g);
    }

    /**
     * Zmena velikosti herniho planu
     *
     * @param width  - sirka
     * @param height - vyska
     */
    public void recalculateBoard(int width, int height) {
        int scale = this.context.scale;
        int realW = width * scale;
        int realH = height * scale;
        //210 je prirozena sirka praveho informacniho sloupce
        //93 je prirozena vyska panelu s tlacitky vezi
        game.setSize(realW + 210 + scale, realH + 93 + scale);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, realW, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, realH, Short.MAX_VALUE)
        );
    }

    private void initComponents() {
        this.setDoubleBuffered(true);
		/*javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );*/
    }

}
