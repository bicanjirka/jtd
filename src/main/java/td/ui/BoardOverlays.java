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
 * The win and lose banners over the board, each with a button that plays the last wave again and a "Back to menu"
 * button. Both are added once and toggled with {@link #showLost}, {@link #showWon} and {@link #reset}.
 * <p>
 * A banner is small and anchored to the top so the final board stays visible and its towers stay
 * selectable.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class BoardOverlays {

    private static final Color BANNER_FILL = new Color(0, 0, 0, 210);
    private static final Color BANNER_EDGE = new Color(189, 189, 193);
    private static final Color BANNER_TEXT = Hud.FOREGROUND;
    private static final int CORNER_RADIUS = 14;

    private final HudButton retry = new HudButton("Retry wave");
    private final HudButton replay = new HudButton("Replay wave");
    private final JPanel lostPanel;
    private final JPanel wonPanel;

    private Runnable onBackToMenu = () -> {
    };
    private Runnable onPlayAgain = () -> {
    };

    public BoardOverlays() {
        this.lostPanel = buildOverlay("Game Over!", this.retry);
        this.wonPanel = buildOverlay("Congratulations!", this.replay);
    }

    /**
     * A transparent layer over the board holding the plate. It keeps the board's grid constraints:
     * at the plate's size, the shared cell would collapse and take the board with it.
     */
    private JPanel buildOverlay(String text, HudButton playAgain) {
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

        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        playAgain.setFace(ControlFace.button().withChip(GameKey.RESTART.chip()));
        playAgain.addActionListener(evt -> this.onPlayAgain.run());
        GridBagConstraints each = new GridBagConstraints();
        each.insets = new Insets(0, 4, 0, 4);
        buttons.add(playAgain, each);
        HudButton backToMenu = new HudButton("Back to menu");
        backToMenu.setFace(ControlFace.button().withChip(GameKey.MENU.chip()));
        backToMenu.addActionListener(this::backToMenuActionPerformed);
        buttons.add(backToMenu, each);
        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridy = 1;
        buttonConstraints.insets = new Insets(8, 20, 12, 20);
        plate.add(buttons, buttonConstraints);

        GridBagConstraints plateConstraints = new GridBagConstraints();
        plateConstraints.weighty = 1;
        plateConstraints.anchor = GridBagConstraints.PAGE_START;
        plateConstraints.insets = new Insets(24, 0, 0, 0);
        layer.add(plate, plateConstraints);

        return layer;
    }

    /** Adds both overlays to {@code board} in the board's own grid cell. */
    public void addTo(JPanel board, GridBagConstraints cellConstraints) {
        board.add(this.lostPanel, cellConstraints);
        board.add(this.wonPanel, cellConstraints);
    }

    public void onBackToMenu(Runnable r) {
        this.onBackToMenu = r;
    }

    /** What the retry and replay buttons run: the last wave sent, played again. */
    public void onPlayAgain(Runnable r) {
        this.onPlayAgain = r;
    }

    /** The lose banner, offering to retry {@code wave}. */
    public void showLost(int wave) {
        this.retry.setText("Retry wave " + wave);
        this.lostPanel.setVisible(true);
    }

    /** The win banner, offering to replay {@code wave}. */
    public void showWon(int wave) {
        this.replay.setText("Replay wave " + wave);
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
     * The translucent plate the message sits on. It paints itself, because an opaque component with
     * an alpha background has nothing beneath it to blend with and comes out solid.
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
