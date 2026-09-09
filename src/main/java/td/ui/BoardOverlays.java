package td.ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

/**
 * The "Game Over!"/"Congratulations!" panels painted over the board on a loss or a win.
 * Both exist the whole time (added to the board's layout once) and are shown/hidden via
 * {@link #showLost}/{@link #showWon}/{@link #reset}, matching how the board itself is a
 * permanent component whose content changes rather than being rebuilt per level. Each also
 * carries a "Back to menu" button, wired through {@link #onBackToMenu} the same way
 * {@link PanelGameConsole}'s speed buttons are - a no-op default {@link Runnable}, an
 * {@code addActionListener}, and a public setter TowerDefense wires up.
 */
public class BoardOverlays {

    private final JPanel lostPanel;
    private final JPanel wonPanel;

    private Runnable onBackToMenu = () -> {
    };

    public BoardOverlays() {
        this.lostPanel = buildOverlay("Game Over!");
        this.wonPanel = buildOverlay("Congratulations!");
    }

    private JPanel buildOverlay(String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(0, 0, 0, 80));

        JLabel label = new JLabel();
        label.setBackground(new Color(0, 0, 0));
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        label.setForeground(new Color(220, 255, 220));
        label.setText(text);
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridy = 0;
        panel.add(label, labelConstraints);

        JButton backToMenu = new JButton("Back to menu");
        backToMenu.setFocusable(false);
        backToMenu.addActionListener(this::backToMenuActionPerformed);
        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridy = 1;
        buttonConstraints.insets = new Insets(8, 0, 0, 0);
        panel.add(backToMenu, buttonConstraints);

        return panel;
    }

    /** Adds both overlay panels to {@code board}, stacked at the given GridBagLayout cell. */
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
}
