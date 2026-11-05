package td.ui;

import td.TowerDefense;
import td.board.BoardGeometry;
import td.util.GameWorld;

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.Serial;

/**
 * The board component, drawn at the zoom its size allows. {@link #paint} skips
 * {@code super.paint()} and hands the graphics to the frame painter, whose background fill clears
 * the previous frame.
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
        // The zoomed frame can stop a pixel short of the component's edge.
        g.setColor(Hud.BACKGROUND);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            double factor = this.zoom().factor();
            g2.scale(factor, factor);
            this.game.paintBoard(g2);
        } finally {
            g2.dispose();
        }
    }

    /** The zoom the board is drawn at, from the size the layout gave it. */
    public BoardZoom zoom() {
        BoardGeometry board = this.context.getBoard();
        return BoardZoom.fit(new Dimension(board.pixelWidth(), board.pixelHeight()), this.getSize());
    }

    /** The board pixel under {@code evt}, whichever component received it. */
    public Point boardPixelAt(MouseEvent evt) {
        Point here = SwingUtilities.convertPoint(evt.getComponent(), evt.getPoint(), this);
        BoardZoom zoom = this.zoom();
        return new Point(zoom.toBoard(here.x), zoom.toBoard(here.y));
    }

    /** Sizes the board, at its natural zoom, for a level of {@code width} x {@code height} cells. */
    public void recalculateBoard(int width, int height) {
        int scale = this.context.getBoard().scale();
        int realW = width * scale;
        int realH = height * scale;
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
