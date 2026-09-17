package td.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.List;
import java.util.Objects;

/**
 * The text pane under the tower toolbar, showing either the selected tower's live status and
 * a sell button, or - via {@link #setExternalText} - whatever the engine last pushed through
 * {@code GameHost.setInfoText} (a hovered tower's pre-purchase stats, a rejected placement).
 * The two are mutually exclusive: selecting a tower replaces external text and vice versa.
 */
public class PanelTowerInfo extends JPanel implements EconomyListener {

    private static final Logger LOG = LoggerFactory.getLogger(PanelTowerInfo.class);

    /**
     * A single-property, narrow exception to every control otherwise sharing one look (see
     * {@link Hud}): selling is destructive and irreversible, so its text alone reads
     * differently. Border, fill, hover/press states and font all still come from {@link Hud}
     * untouched - {@code paintCentredText} already reads a button's own foreground colour,
     * so no change to {@code Hud}/{@code HudButton} is needed to support this.
     */
    private static final Color SELL_TEXT_COLOR = new Color(255, 120, 120);

    @Serial
    private static final long serialVersionUID = 1L;

    private GameWorld context;
    private Tower selectedTower;
    private String lastText;
    private boolean levelEnded = false;
    private HudButton jButton_sell;
    private HudButton jButton_path1;
    private HudButton jButton_path2;
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
     * {@code TowerDefense.keyTyped} refuses the keyboard equivalents. {@link Hud} paints the
     * disabled state, so it stays legible on this panel's dark face.
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
            this.jButton_sell.setText("Sell ( $" + this.selectedTower.getSellPrice() + " )");
            this.setText(this.selectedTower.getStatusString());
            this.updatePathButtons();
        }
    }

    /**
     * Shows up to two upgrade-path buttons for the selected tower, one per
     * {@code availablePaths()} entry - or none, once a path has already been chosen (its name
     * then reads from {@code getStatusString()}'s own "Specialized: ..." line instead).
     * Called from both {@link #updateInterface} and the render-pulse {@link #refreshSelected},
     * the same way sell's affordability and the live kill/damage figures already are - a
     * path's enablement (cluster size, damage dealt, kill count) can change without any
     * economy event, so it needs the same per-frame re-derivation.
     */
    private void updatePathButtons() {
        List<UpgradePath> paths = this.selectedTower.getChosenPath().isPresent()
                ? List.of()
                : this.selectedTower.availablePaths();
        this.updatePathButton(this.jButton_path1, paths, 0);
        this.updatePathButton(this.jButton_path2, paths, 1);
    }

    private void updatePathButton(HudButton button, List<UpgradePath> paths, int index) {
        if (index >= paths.size()) {
            button.setVisible(false);
            return;
        }
        UpgradePath path = paths.get(index);
        button.setVisible(true);
        button.setText(path.displayName() + " ( $" + path.price() + " )");
        boolean available = !this.levelEnded
                && path.condition().isSatisfied(this.selectedTower, this.context)
                && this.context.economy().canPay(path.price());
        button.setEnabled(available);
    }

    private void choosePathAt(int index) {
        if (this.selectedTower == null) {
            return;
        }
        List<UpgradePath> paths = this.selectedTower.availablePaths();
        if (index < paths.size() && this.selectedTower.chooseUpgradePath(paths.get(index))) {
            this.updateInterface();
        }
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
            this.updatePathButtons();
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
        this.jTextPane1.setText(s);
    }

    public void unselectTower() {
        if (this.selectedTower != null) {
            this.selectedTower.setSelected(false);
        }
        this.selectedTower = null;
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
        jButton_sell = new HudButton("Sell");
        jButton_path1 = new HudButton("");
        jButton_path2 = new HudButton("");

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

        jPanel_buttons.setLayout(new GridBagLayout());

        jPanel_buttons.setBackground(new Color(0, 0, 0));
        jPanel_buttons.setForeground(new Color(220, 255, 220));

        jButton_sell.setText("Sell");
        jButton_sell.setForeground(SELL_TEXT_COLOR);
        jButton_sell.addActionListener(this::jButton_sellActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.01;
        jPanel_buttons.add(jButton_sell, gridBagConstraints);

        jButton_path1.addActionListener(evt -> this.choosePathAt(0));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.01;
        jPanel_buttons.add(jButton_path1, gridBagConstraints);

        jButton_path2.addActionListener(evt -> this.choosePathAt(1));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.01;
        jPanel_buttons.add(jButton_path2, gridBagConstraints);

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
