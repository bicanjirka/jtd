package td.ui;

import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.ui.render.InfoSheet;
import td.util.GameWorld;
import td.util.ThreadConfined;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serial;
import java.util.List;
import java.util.Objects;

/**
 * The text pane under the tower toolbar: the selected tower's status and a sell button, an enemy's
 * or a shop tower's sheet, or plain text. Each replaces the other, and shows from its top. Buying
 * upgrades happens on {@link PanelUpgradeTree}; this pane shows a hovered node's description.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelTowerInfo extends JPanel implements EconomyListener {

    /**
     * The one colour exception to the shared control look: selling is irreversible, so its text
     * reads differently.
     */
    private static final Color SELL_TEXT_COLOR = new Color(255, 120, 120);
    private static final Color GATE_MET_COLOR = new Color(140, 255, 140);
    private static final Color GATE_UNMET_COLOR = SELL_TEXT_COLOR;
    private static final InfoSheet NO_SHEET = new InfoSheet(List.of());
    /** Before the pane is laid out, the width the side panel gives it. */
    private static final int FALLBACK_TEXT_WIDTH = 170;

    @Serial
    private static final long serialVersionUID = 1L;

    private final Java2DFrameRenderer glyphRenderer = new Java2DFrameRenderer();

    private GameWorld context;
    private Tower selectedTower;
    private String lastText;
    private InfoSheet lastSheet = NO_SHEET;
    private int sheetWidth = 0;
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

    public boolean hasSelectedTower() {
        return this.selectedTower != null;
    }

    public void setExternalText(String s) {
        this.jButton_sell.setVisible(false);
        this.setText(s);
    }

    /** An enemy's or a shop tower's sheet, in place of any tower status. */
    public void showSheet(InfoSheet sheet) {
        this.jButton_sell.setVisible(false);
        if (!this.hovering) {
            this.replaceSheet(sheet);
        }
    }

    /** Skips an identical sheet, as {@link #setText} does. */
    private void replaceSheet(InfoSheet sheet) {
        if (sheet.equals(this.lastSheet)) {
            return;
        }
        this.lastSheet = sheet;
        this.lastText = null;
        this.layOutSheet();
    }

    /**
     * Values align to the pane's current width, so a scrollbar coming or going, which changes that
     * width, lays the sheet out again.
     */
    private void layOutSheet() {
        this.sheetWidth = this.textWidth();
        this.jTextPane1.setStyledDocument(InfoSheetDocument.of(this.lastSheet, this.glyphRenderer, this.sheetWidth,
                this.jTextPane1.getFontMetrics(this.jTextPane1.getFont())));
        this.jTextPane1.setCaretPosition(0);
    }

    private void textPaneResized() {
        if (!this.lastSheet.equals(NO_SHEET) && this.textWidth() != this.sheetWidth) {
            this.layOutSheet();
        }
    }

    private int textWidth() {
        int width = this.jTextPane1.getWidth() - this.jTextPane1.getInsets().left - this.jTextPane1.getInsets().right;
        return width > 0 ? width : FALLBACK_TEXT_WIDTH;
    }

    public void onDeselected(Runnable listener) {
        this.onDeselected = listener;
    }

    /** After a level ends, a selected tower's stats stay visible but selling is disabled. */
    public void setLevelEnded(boolean ended) {
        this.levelEnded = ended;
        this.updateInterface();
    }

    private void updateInterface() {
        this.jButton_sell.setVisible(this.selectedTower != null);

        if (this.selectedTower != null) {
            this.jButton_sell.setEnabled(!this.levelEnded);
            this.refreshSelected();
        }
    }

    /**
     * Shows a hovered upgrade node's description in place of the tower status until
     * {@link #clearUpgradeHover}.
     */
    public void showUpgradeHover(String text) {
        this.hovering = true;
        this.lastText = null;
        this.lastSheet = NO_SHEET;
        this.replaceText(text);
    }

    public void clearUpgradeHover() {
        this.hovering = false;
        this.updateInterface();
    }

    /**
     * Rebuilds the selected tower's status. Called from the render pulse, since damage and kills
     * change every tick without any economy event.
     */
    public void refreshSelected() {
        if (this.selectedTower != null && !this.hovering) {
            this.replaceSheet(TowerSheetText.status(this.selectedTower.inspect()));
        }
    }

    /**
     * Skips identical text, since resetting a {@code JTextPane} moves its caret and scroll, and
     * does nothing while hover text owns the pane.
     */
    private void setText(String s) {
        if (this.hovering || Objects.equals(s, this.lastText)) {
            return;
        }
        this.lastText = s;
        this.lastSheet = NO_SHEET;
        this.replaceText(s);
    }

    /**
     * A fresh document, so no style of a previous sheet carries over, read from the top: resetting
     * the text otherwise leaves the caret, and the scroll, at the end.
     */
    private void replaceText(String text) {
        this.jTextPane1.setStyledDocument(new DefaultStyledDocument());
        this.jTextPane1.setText(text);
        this.colorizeMarks(text);
        this.jTextPane1.setCaretPosition(0);
    }

    /** Colours every ✔ green and every ✘ red. */
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

    /** May run on the game-loop thread. */
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
        jTextPane1.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                textPaneResized();
            }
        });

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
