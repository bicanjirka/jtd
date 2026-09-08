package td.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.Serial;

/**
 * The "Game Over!"/"Congratulations!" panels painted over the board on a loss or a win.
 * Both exist the whole time (added to the board's layout once) and are shown/hidden via
 * {@link #showLost}/{@link #showWon}/{@link #reset}, matching how the board itself is a
 * permanent component whose content changes rather than being rebuilt per level.
 */
public class BoardOverlays {

    private final JPanel lostPanel = new JPanel();
    private final JPanel wonPanel = new JPanel();

    public BoardOverlays() {
        this.lostPanel.setLayout(new GridBagLayout());
        this.lostPanel.setBackground(new Color(0, 0, 0, 80));
        JLabel lostText = new JLabel();
        lostText.setBackground(new Color(0, 0, 0));
        lostText.setFont(new Font("SansSerif", Font.BOLD, 18));
        lostText.setForeground(new Color(220, 255, 220));
        lostText.setText("Game Over!");
        this.lostPanel.add(lostText, new GridBagConstraints());

        this.wonPanel.setLayout(new GridBagLayout());
        this.wonPanel.setBackground(new Color(0, 0, 0, 80));
        JLabel wonText = new JLabel();
        wonText.setBackground(new Color(0, 0, 0));
        wonText.setFont(new Font("SansSerif", Font.BOLD, 18));
        wonText.setForeground(new Color(220, 255, 220));
        wonText.setText("Congratulations!");
        this.wonPanel.add(wonText, new GridBagConstraints());
    }

    /** Adds both overlay panels to {@code board}, stacked at the given GridBagLayout cell. */
    public void addTo(JPanel board, GridBagConstraints cellConstraints) {
        board.add(this.lostPanel, cellConstraints);
        board.add(this.wonPanel, cellConstraints);
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
}
