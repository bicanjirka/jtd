package td.ui;

import td.util.ThreadConfined;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.io.Serial;

/**
 * The "Game Over!"/"Congratulations!" banners shown over the board on a loss or a win.
 * Both exist the whole time (added to the board's layout once) and are shown/hidden via
 * {@link #showLost}/{@link #showWon}/{@link #reset}, matching how the board itself is a
 * permanent component whose content changes rather than being rebuilt per level. Each also
 * carries a "Back to menu" button, wired through {@link #onBackToMenu} the same way
 * {@link PanelGameConsole}'s speed buttons are - a no-op default {@link Runnable}, an
 * {@code addActionListener}, and a public setter TowerDefense wires up.
 * <p>
 * A banner is deliberately small and anchored to the top of the board, not a full-board
 * cover. The simulation is stopped by then, but the final position of every enemy and tower
 * is exactly what a player wants to look at after a loss - including selecting a tower to
 * read its stats, which still works because the board keeps rendering and the banner covers
 * almost none of it.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class BoardOverlays {

    private static final Color BANNER_FILL = new Color(0, 0, 0, 210);
    private static final Color BANNER_EDGE = new Color(120, 200, 120);
    private static final Color BANNER_TEXT = new Color(220, 255, 220);
    private static final int CORNER_RADIUS = 14;

    private final JPanel lostPanel;
    private final JPanel wonPanel;

    private Runnable onBackToMenu = () -> {
    };

    public BoardOverlays() {
        this.lostPanel = buildOverlay("Game Over!");
        this.wonPanel = buildOverlay("Congratulations!");
    }

    /**
     * A transparent layer covering the board, holding the plate near the top. The layer keeps
     * the board's own grid constraints - it must stay the same size as the board, or the cell
     * they share collapses to the plate's size and takes the board down with it - and shows
     * the board through everywhere the plate itself does not cover.
     */
    private JPanel buildOverlay(String text) {
        JPanel layer = new JPanel(new GridBagLayout());
        layer.setOpaque(false);

        JPanel plate = new Plate();

        JLabel label = new JLabel();
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        label.setForeground(BANNER_TEXT);
        label.setText(text);
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridy = 0;
        labelConstraints.insets = new Insets(12, 24, 0, 24);
        plate.add(label, labelConstraints);

        HudButton backToMenu = new HudButton("Back to menu");
        backToMenu.addActionListener(this::backToMenuActionPerformed);
        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridy = 1;
        buttonConstraints.insets = new Insets(8, 24, 12, 24);
        plate.add(backToMenu, buttonConstraints);

        GridBagConstraints plateConstraints = new GridBagConstraints();
        plateConstraints.weighty = 1;
        plateConstraints.anchor = GridBagConstraints.PAGE_START;
        plateConstraints.insets = new Insets(24, 0, 0, 0);
        layer.add(plate, plateConstraints);

        return layer;
    }

    /**
     * Adds both overlay layers to {@code board}, at the same grid cell as the board itself.
     */
    public void addTo(JPanel board, GridBagConstraints cellConstraints) {
        board.add(this.lostPanel, cellConstraints);
        board.add(this.wonPanel, cellConstraints);
    }

    public void onBackToMenu(Runnable r) {
        this.onBackToMenu = r;
    }

    public void showLost() {
        this.lostPanel.setVisible(true);
    }

    public void showWon() {
        this.wonPanel.setVisible(true);
    }

    public void reset() {
        this.lostPanel.setVisible(false);
        this.wonPanel.setVisible(false);
    }

    private void backToMenuActionPerformed(ActionEvent evt) {
        this.onBackToMenu.run();
    }

    /**
     * The rounded, translucent plate the message actually sits on. It paints itself rather
     * than using a background colour with an alpha channel: an opaque Swing component is
     * contracted to fill every pixel of its bounds, so Swing skips painting what is underneath
     * and the alpha has nothing to blend against - which is why the old overlay, whose only
     * translucency was a background colour, came out solid black over the board.
     */
    private static final class Plate extends JPanel {
        @Serial
        private static final long serialVersionUID = 1L;

        Plate() {
            super(new GridBagLayout());
            this.setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BANNER_FILL);
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), CORNER_RADIUS, CORNER_RADIUS);
            g2.setColor(BANNER_EDGE);
            g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
