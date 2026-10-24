package td.ui;

import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.PulseTower;
import td.tower.Tower;
import td.ui.render.InfoSheet;
import td.ui.render.TowerSheet;
import td.util.GameWorld;
import td.util.ThreadConfined;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultStyledDocument;

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
import java.util.function.Consumer;

/**
 * The text pane under the tower toolbar: the selected tower's status and a sell button, an enemy's
 * or a shop tower's sheet, or plain text. Each replaces the other, and shows from its top. A hovered
 * sheet only lends the pane: the selected tower's status comes back once the hover ends. Buying
 * upgrades happens on {@link PanelUpgradeTree}.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelTowerInfo extends JPanel implements EconomyListener {

    /**
     * The one colour exception to the shared control look: selling is irreversible, so its text
     * reads differently.
     */
    private static final Color SELL_TEXT_COLOR = new Color(255, 120, 120);
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
    private Consumer<Boolean> onPinToggled = pinning -> {
    };
    private TooltipLayer tooltips;
    private HudButton jButton_sell;
    private HudToggleButton jToggle_pin;
    private JScrollPane jScrollPane1;
    private SheetPane sheetPane;

    public PanelTowerInfo() {
        initComponents();
    }

    /** The layer whose tooltips this pane's mentions and icons open. */
    public void setTooltips(TooltipLayer layer) {
        this.tooltips = layer;
        layer.watch(this.sheetPane);
    }

    public void setTower(Tower t) {
        if (this.selectedTower != null) {
            this.selectedTower.setSelected(false);
        }
        this.selectedTower = t;
        this.selectedTower.setSelected(true);
        this.updateInterface();
    }

    public boolean hasSelectedTower() {
        return this.selectedTower != null;
    }

    public void setExternalText(String s) {
        this.closeTooltips();
        this.jButton_sell.setVisible(false);
        this.jToggle_pin.setVisible(false);
        this.setText(s);
    }

    /** An enemy's or a shop tower's sheet, in place of any tower status. */
    public void showSheet(InfoSheet sheet) {
        this.hovering = false;
        this.jButton_sell.setVisible(false);
        this.jToggle_pin.setVisible(false);
        this.replaceSheet(sheet);
    }

    /**
     * The sheet of what the pointer rests on, shown over the selection without clearing it. Sell and pin hide meanwhile,
     * as they would read as acting on the hovered tower.
     */
    public void showHoveredSheet(InfoSheet sheet) {
        this.hovering = true;
        this.jButton_sell.setVisible(false);
        this.jToggle_pin.setVisible(false);
        this.replaceSheet(sheet);
    }

    /** Gives the pane back to the selected tower; with none, the hovered sheet stays so its terms can still be read. */
    public void hoverEnded() {
        if (this.hovering) {
            this.hovering = false;
            this.updateInterface();
        }
    }

    public boolean isHovering() {
        return this.hovering;
    }

    InfoSheet shownSheet() {
        return this.lastSheet;
    }

    /** Skips an identical sheet, as {@link #setText} does. */
    private void replaceSheet(InfoSheet sheet) {
        if (sheet.equals(this.lastSheet)) {
            return;
        }
        boolean another = this.lastSheet.lines().isEmpty() || sheet.lines().isEmpty()
                || !this.lastSheet.lines().getFirst().equals(sheet.lines().getFirst());
        this.lastSheet = sheet;
        this.lastText = null;
        if (another) {
            this.closeTooltips();
        }
        this.layOutSheet();
        if (!another && this.tooltips != null) {
            this.tooltips.sheetChanged(this.sheetPane, sheet);
        }
    }

    /**
     * Values align to the pane's current width, so a scrollbar coming or going, which changes that
     * width, lays the sheet out again.
     */
    private void layOutSheet() {
        this.sheetWidth = this.textWidth();
        this.sheetPane.show(this.lastSheet, this.glyphRenderer, this.sheetWidth);
    }

    private void closeTooltips() {
        if (this.tooltips != null) {
            this.tooltips.closeAll();
        }
    }

    private void textPaneResized() {
        if (!this.lastSheet.equals(NO_SHEET) && this.textWidth() != this.sheetWidth) {
            this.layOutSheet();
        }
    }

    private int textWidth() {
        int width = this.sheetPane.getWidth() - this.sheetPane.getInsets().left - this.sheetPane.getInsets().right;
        return width > 0 ? width : FALLBACK_TEXT_WIDTH;
    }

    public void onDeselected(Runnable listener) {
        this.onDeselected = listener;
    }

    /** Told {@code true} when the player asks to pick a Ley Line's host, {@code false} when they take it back. */
    public void onPinToggled(Consumer<Boolean> listener) {
        this.onPinToggled = listener;
    }

    /** Shows whether the next board click picks the selected Obelisk's Ley Line host. */
    public void showPinning(boolean pinning) {
        this.jToggle_pin.setSelected(pinning);
    }

    /** After a level ends, a selected tower's stats stay visible but selling is disabled. */
    public void setLevelEnded(boolean ended) {
        this.levelEnded = ended;
        this.updateInterface();
    }

    private void updateInterface() {
        if (this.hovering) {
            return;
        }
        this.jButton_sell.setVisible(this.selectedTower != null);
        boolean pins = this.selectedTower != null && !this.levelEnded && PulseTower.castsLeyLine(this.selectedTower);
        this.jToggle_pin.setVisible(pins);
        if (!pins) {
            this.jToggle_pin.setSelected(false);
        }

        if (this.selectedTower != null) {
            this.jButton_sell.setEnabled(!this.levelEnded);
            this.jButton_sell.setText("Sell ( $" + this.selectedTower.getSellPrice() + " )");
            this.replaceSheet(TowerSheetText.status(this.selectedTower.inspect()));
        }
    }

    /**
     * The selected tower's sheet, built with the frame; shown only while that tower is still selected and nothing
     * else owns the pane.
     */
    public void showTowerSheet(TowerSheet sheet) {
        if (!this.hovering && this.selectedTower != null && sheet.x() == this.selectedTower.getX()
                && sheet.y() == this.selectedTower.getY()) {
            this.replaceSheet(sheet.sheet());
        }
    }

    /**
     * Skips identical text, since resetting a {@code JTextPane} moves its caret and scroll.
     */
    private void setText(String s) {
        if (Objects.equals(s, this.lastText)) {
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
        this.sheetPane.setStyledDocument(new DefaultStyledDocument());
        this.sheetPane.setText(text);
        this.sheetPane.setCaretPosition(0);
    }

    public void unselectTower() {
        if (this.selectedTower != null) {
            this.selectedTower.setSelected(false);
        }
        this.selectedTower = null;
        this.closeTooltips();
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
        sheetPane = new SheetPane();
        jButton_sell = new HudButton("Sell");
        jToggle_pin = new HudToggleButton();

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
        sheetPane.setBackground(new Color(0, 0, 0));
        sheetPane.setBorder(null);
        sheetPane.setForeground(new Color(220, 255, 220));
        jScrollPane1.setViewportView(sheetPane);
        sheetPane.addComponentListener(new ComponentAdapter() {
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

        jToggle_pin.setText("Pin line");
        jToggle_pin.setToolTipText("Pick the Obelisk this one's Ley Line links to; pick it again to unpin");
        jToggle_pin.setVisible(false);
        jToggle_pin.addActionListener(evt -> this.onPinToggled.accept(this.jToggle_pin.isSelected()));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.01;
        add(jToggle_pin, gridBagConstraints);

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
