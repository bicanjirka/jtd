package td.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.util.GameWorld;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.Objects;

/**
 * The text pane under the tower toolbar, showing either the selected tower's live status and
 * a sell button, or - via {@link #setExternalText} - whatever the engine last pushed through
 * {@code GameHost.setInfoText} (a hovered tower's pre-purchase stats, a rejected placement).
 * The two are mutually exclusive: selecting a tower replaces external text and vice versa.
 */
public class PanelTowerInfo extends JPanel implements EconomyListener {

    private static final Logger LOG = LoggerFactory.getLogger(PanelTowerInfo.class);

    private static final String SELL_TEXT_ENABLED = "#DCFFDC";
    private static final String SELL_TEXT_DISABLED = "#6E7A6E";

    @Serial
    private static final long serialVersionUID = 1L;

    private GameWorld context;
    private Tower selectedTower;
    private String lastText;
    private boolean levelEnded = false;
    private JButton jButton_sell;
    private JPanel jPanel_buttons;
    private JScrollPane jScrollPane1;
    private JTextPane jTextPane1;

    public PanelTowerInfo() {
        initComponents();
    }

    public void setTower(Tower t) {
        if (this.selectedTower != null) {
            this.selectedTower.setSelected(false);
        }
        this.selectedTower = t;
        this.selectedTower.setSelected(true);
        this.updateInterface();
    }

    public void setExternalText(String s) {
        this.jPanel_buttons.setVisible(false);
        this.setText(s);
    }

    /**
     * Whether the level is over. A finished level still shows a selected tower's stats - that
     * is the point of leaving the board visible behind the win/lose banner - but selling is a
     * move in a game that has already been decided, so the button is greyed out, matching how
     * {@code TowerDefense.keyTyped} refuses the keyboard equivalents.
     */
    public void setLevelEnded(boolean ended) {
        this.levelEnded = ended;
        this.updateInterface();
    }

    private void updateInterface() {
        this.jPanel_buttons.setVisible(this.selectedTower != null);
        this.jButton_sell.setVisible(this.selectedTower != null);

        if (this.selectedTower != null) {
            this.jButton_sell.setEnabled(!this.levelEnded);
            this.jButton_sell.setText(sellLabel(this.selectedTower.getSellPrice(), this.levelEnded));
            this.setText(this.selectedTower.getStatusString());
        }
    }

    /**
     * The sell label as HTML, carrying its own colour. A button's disabled text colour is
     * normally the look-and-feel's to pick, and against this panel's black button face it
     * comes out invisible - the button read as an empty box rather than as a greyed-out one.
     * Swing paints HTML button text through its own view, colours included, regardless of the
     * button's enabled state, which is the one way to set that colour per-component rather
     * than by mutating a shared {@code UIManager} key.
     */
    private static String sellLabel(int price, boolean disabled) {
        String color = disabled ? SELL_TEXT_DISABLED : SELL_TEXT_ENABLED;
        return "<html><font color='" + color + "'>Sell ( $" + price + " )</font></html>";
    }

    /**
     * Rebuilds the selected tower's status text, if there is one. Damage dealt and kill count
     * change on any tick the tower fires, and nothing reports that - {@link #economyChanged} only
     * fires on a kill or a purchase, so between kills the panel used to sit stale until the
     * tower was clicked again. Called from the render pulse rather than from tick code: it runs
     * on the EDT at a flat ~60fps, so the text tracks the simulation without being rewritten
     * once per tick while fast-forwarding.
     */
    public void refreshSelected() {
        if (this.selectedTower != null) {
            this.setText(this.selectedTower.getStatusString());
        }
    }

    /**
     * Skips identical text, which matters because this is now called every frame: handing a
     * {@code JTextPane} the same string again still resets its caret and scroll position.
     */
    private void setText(String s) {
        if (Objects.equals(s, this.lastText)) {
            return;
        }
        this.lastText = s;
        try {
            this.jTextPane1.setText(s);
        } catch (NullPointerException e) {
            LOG.warn("Could not set tower info text", e);
        }
    }

    public void unselectTower() {
        if (this.selectedTower != null) {
            this.selectedTower.setSelected(false);
        }
        this.selectedTower = null;
    }

    public void setGameWorld(GameWorld context) {
        this.context = context;
        this.context.addEconomyListener(this);
    }

    private void sellCurrentTower() {
        if (this.selectedTower != null) {
            this.context.sellTower(this.selectedTower);
            this.unselectTower();
            this.updateInterface();
        }
    }

    /** Also reachable from the game-loop thread - see GameWorld.apply()'s callers. */
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(this::updateInterface);
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        jScrollPane1 = new JScrollPane();
        jTextPane1 = new JTextPane();
        jPanel_buttons = new JPanel();
        jButton_sell = new JButton();

        setLayout(new GridBagLayout());

        setBackground(new Color(0, 0, 0));
        setBorder(BorderFactory.createTitledBorder(null, "Info", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.PLAIN, 11), new Color(220, 255, 220)));
        setForeground(new Color(220, 255, 220));
        setMaximumSize(new Dimension(200, 2147483647));
        setMinimumSize(new Dimension(200, 150));
        setPreferredSize(new Dimension(200, 300));
        jScrollPane1.setBackground(new Color(0, 0, 0));
        jScrollPane1.setBorder(null);
        jScrollPane1.setForeground(new Color(220, 255, 220));
        jTextPane1.setBackground(new Color(0, 0, 0));
        jTextPane1.setBorder(null);
        jTextPane1.setForeground(new Color(220, 255, 220));
        jScrollPane1.setViewportView(jTextPane1);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 0.01;
        add(jScrollPane1, gridBagConstraints);

        jPanel_buttons.setLayout(new GridBagLayout());

        jPanel_buttons.setBackground(new Color(0, 0, 0));
        jPanel_buttons.setForeground(new Color(220, 255, 220));

        jButton_sell.setBackground(new Color(0, 0, 0));
        jButton_sell.setText("Sell");
        jButton_sell.setMargin(new Insets(2, 2, 2, 2));
        jButton_sell.addActionListener(this::jButton_sellActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.01;
        jPanel_buttons.add(jButton_sell, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(jPanel_buttons, gridBagConstraints);

    }

    private void jButton_sellActionPerformed(ActionEvent evt) {
        this.sellCurrentTower();
    }

}
