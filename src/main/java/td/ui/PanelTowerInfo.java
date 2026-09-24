package td.ui;

import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.util.GameWorld;
import td.util.ThreadConfined;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.Objects;

/**
 * The text pane under the tower toolbar, showing either the selected tower's live status and
 * a sell button, or - via {@link #setExternalText} - whatever the engine last pushed through
 * {@code GameHost.setInfoText} (a hovered tower's pre-purchase stats, a rejected placement).
 * The two are mutually exclusive: selecting a tower replaces external text and vice versa.
 * <p>
 * A selected tower's own upgrade buying happens on {@link PanelUpgradeTree}, not here - this
 * panel only shows gate progress (via {@code AbstractTower.getStatusString()}'s ✔/✘
 * lines, coloured by {@link #colorizeMarks}) and, while hovering a node button, that node's
 * full description (see {@link #showUpgradeHover}).
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelTowerInfo extends JPanel implements EconomyListener {

    /**
     * A single-property, narrow exception to every control otherwise sharing one look (see
     * {@link Hud}): selling is destructive and irreversible, so its text alone reads
     * differently. Border, fill, hover/press states and font all still come from {@link Hud}
     * untouched - {@code paintCentredText} already reads a button's own foreground colour,
     * so no change to {@code Hud}/{@code HudButton} is needed to support this.
     */
    private static final Color SELL_TEXT_COLOR = new Color(255, 120, 120);
    private static final Color GATE_MET_COLOR = new Color(140, 255, 140);
    private static final Color GATE_UNMET_COLOR = SELL_TEXT_COLOR;

    @Serial
    private static final long serialVersionUID = 1L;

    private GameWorld context;
    private Tower selectedTower;
    private String lastText;
    private boolean levelEnded = false;
    private boolean hovering = false;
    private Runnable onDeselected = () -> {
    };
    private HudButton jButton_sell;
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
        this.hovering = false;
        this.updateInterface();
    }

    public void setExternalText(String s) {
        this.jButton_sell.setVisible(false);
        this.setText(s);
    }

    public void onDeselected(Runnable listener) {
        this.onDeselected = listener;
    }

    /**
     * Whether the level is over. A finished level still shows a selected tower's stats - that
     * is the point of leaving the board visible behind the win/lose banner - but selling is a
     * move in a game that has already been decided, so the button is greyed out, matching how
     * {@code TowerDefense.keyTyped} refuses the keyboard equivalents. {@link Hud} paints the
     * disabled state, so it stays legible on this panel's dark face.
     */
    public void setLevelEnded(boolean ended) {
        this.levelEnded = ended;
        this.updateInterface();
    }

    private void updateInterface() {
        this.jButton_sell.setVisible(this.selectedTower != null);

        if (this.selectedTower != null) {
            this.jButton_sell.setEnabled(!this.levelEnded);
            this.jButton_sell.setText("Sell ( $" + this.selectedTower.getSellPrice() + " )");
            this.setText(this.selectedTower.getStatusString());
        }
    }

    /**
     * Shows a hovered upgrade node's full description, overriding the selected tower's own
     * status text until {@link #clearUpgradeHover} - called by {@code PanelUpgradeTree}'s own
     * hover callback (see {@code PanelGameConsole}'s wiring).
     */
    public void showUpgradeHover(String text) {
        this.hovering = true;
        this.lastText = null;
        this.jTextPane1.setText(text);
        this.colorizeMarks(text);
    }

    public void clearUpgradeHover() {
        this.hovering = false;
        this.updateInterface();
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
     * A no-op while a node's hover text owns the pane - see {@link #showUpgradeHover}.
     */
    private void setText(String s) {
        if (this.hovering || Objects.equals(s, this.lastText)) {
            return;
        }
        this.lastText = s;
        this.jTextPane1.setText(s);
        this.colorizeMarks(s);
    }

    /**
     * Colours every ✔ green and every ✘ red - the only styling this pane applies
     * beyond {@link Hud}'s own control/panel look, since {@code AbstractTower.getStatusString()}
     * marks a gate's own met/unmet state with those two characters rather than this panel
     * re-deriving it.
     */
    private void colorizeMarks(String text) {
        StyledDocument doc = this.jTextPane1.getStyledDocument();
        SimpleAttributeSet met = new SimpleAttributeSet();
        StyleConstants.setForeground(met, GATE_MET_COLOR);
        SimpleAttributeSet unmet = new SimpleAttributeSet();
        StyleConstants.setForeground(unmet, GATE_UNMET_COLOR);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '✔') {
                doc.setCharacterAttributes(i, 1, met, false);
            } else if (c == '✘') {
                doc.setCharacterAttributes(i, 1, unmet, false);
            }
        }
    }

    public void unselectTower() {
        if (this.selectedTower != null) {
            this.selectedTower.setSelected(false);
        }
        this.selectedTower = null;
        this.hovering = false;
    }

    public void setGameWorld(GameWorld context) {
        this.context = context;
        this.context.economy().addEconomyListener(this);
    }

    private void sellCurrentTower() {
        if (this.selectedTower != null) {
            this.context.towers().sell(this.selectedTower);
            this.unselectTower();
            this.updateInterface();
            this.onDeselected.run();
        }
    }

    /**
     * Also reachable from the game-loop thread - see GameWorld.apply()'s callers.
     */
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(this::updateInterface);
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        jScrollPane1 = new JScrollPane();
        jTextPane1 = new JTextPane();
        jButton_sell = new HudButton("Sell");

        setLayout(new GridBagLayout());

        setBackground(new Color(0, 0, 0));
        setBorder(Hud.panelBorder("Info"));
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

        jButton_sell.setText("Sell");
        jButton_sell.setForeground(SELL_TEXT_COLOR);
        jButton_sell.addActionListener(this::jButton_sellActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.01;
        add(jButton_sell, gridBagConstraints);

    }

    private void jButton_sellActionPerformed(ActionEvent evt) {
        this.sellCurrentTower();
    }

}
