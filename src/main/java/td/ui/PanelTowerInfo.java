package td.ui;

import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.economy.Refund;
import td.economy.SellRefund;
import td.term.TermText;
import td.tower.PulseTower;
import td.tower.Tower;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;
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
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The text pane under the tower toolbar: the selected tower's status and a sell button, the selected
 * enemy's sheet, the sheet of the tower being placed, or plain text. Each replaces the other, and
 * shows from its top. Only a click changes it; a hover opens a tooltip instead. Buying upgrades
 * happens on {@link PanelUpgradeTree}.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelTowerInfo extends JPanel implements EconomyListener {

    /**
     * The one colour exception to the shared control look: selling is irreversible, so its text
     * reads differently.
     */
    static final Color SELL_TEXT_COLOR = new Color(255, 120, 120);
    private static final InfoSheet NO_SHEET = new InfoSheet(List.of());
    private static final InfoSheet PIN_TOOLTIP = new InfoSheet(List.of(
            new SheetLine.Title(SheetLine.Glyph.CHEVRON, Palette.TOWER_PULSE_BODY, "Pin line", ""),
            new SheetLine.Prose(TermText.of("Pick the Obelisk this one's Ley Line links to; pick it again to unpin."))));
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
    private Runnable onDeselected = () -> {
    };
    private Consumer<Boolean> onPinToggled = pinning -> {
    };
    private TooltipLayer tooltips;
    private HudButton sellButton;
    private HudToggleButton pinToggle;
    private JScrollPane jScrollPane1;
    private SheetPane sheetPane;

    public PanelTowerInfo() {
        initComponents();
    }

    /** The layer whose tooltips this pane's mentions and icons open. */
    public void setTooltips(TooltipLayer layer) {
        this.tooltips = layer;
        layer.watch(this.sheetPane);
        layer.watchButton(this.pinToggle, () -> Optional.of(new SheetTooltip(TooltipKey.of("tower", "pin"),
                PIN_TOOLTIP)));
        layer.watchButton(this.sellButton, () -> Optional.ofNullable(this.selectedTower)
                .map(tower -> new SheetTooltip(TooltipKey.of("tower", "sell"), sellTooltip(tower.sellRefund().isFull()))));
    }

    /** "Sell ( $120 )", or "Sell ( $120 · full )" for a tower no wave has run with, which sells for all it cost. */
    private static String sellText(Refund refund) {
        return "Sell ( $" + refund.total() + (refund.isFull() ? " · full" : "") + " )";
    }

    private static InfoSheet sellTooltip(boolean inFull) {
        String why = inFull ? "No wave has run with this tower yet, so selling it gives back all it cost."
                : "Selling gives back " + Math.round(SellRefund.SHARE * 100)
                        + "% of what a wave has used, and all of what was bought since the last wave ran.";
        return new InfoSheet(List.of(
                new SheetLine.Title(SheetLine.Glyph.CASH, Palette.TERM_STATUS_CASH, "Sell", ""),
                new SheetLine.Prose(TermText.of(why))));
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
        this.sellButton.setVisible(false);
        this.pinToggle.setVisible(false);
        this.setText(s);
    }

    /** An enemy's or a shop tower's sheet, in place of any tower status. */
    public void showSheet(InfoSheet sheet) {
        this.sellButton.setVisible(false);
        this.pinToggle.setVisible(false);
        this.replaceSheet(sheet);
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
        this.pinToggle.setSelected(pinning);
    }

    /** After a level ends, a selected tower's stats stay visible but selling is disabled. */
    public void setLevelEnded(boolean ended) {
        this.levelEnded = ended;
        this.updateInterface();
    }

    private void updateInterface() {
        this.sellButton.setVisible(this.selectedTower != null);
        boolean pins = this.selectedTower != null && !this.levelEnded && PulseTower.castsLeyLine(this.selectedTower);
        this.pinToggle.setVisible(pins);
        if (!pins) {
            this.pinToggle.setSelected(false);
        }

        if (this.selectedTower != null) {
            this.sellButton.setEnabled(!this.levelEnded);
            this.sellButton.setText(sellText(this.selectedTower.sellRefund()));
            this.replaceSheet(TowerSheetText.status(this.selectedTower.inspect()));
        }
    }

    /**
     * The selected tower's sheet, built with the frame; shown only while that tower is still selected.
     */
    public void showTowerSheet(TowerSheet sheet) {
        if (this.selectedTower != null && sheet.x() == this.selectedTower.getX()
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

    /** Sells the selected tower as a click on Sell does, while Sell shows and is enabled. */
    public void sellSelected() {
        if (this.sellButton.isVisible() && this.sellButton.isEnabled()) {
            this.sellCurrentTower();
        }
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
        sellButton = new HudButton("Sell");
        pinToggle = new HudToggleButton();

        setLayout(new GridBagLayout());

        setBackground(Hud.BACKGROUND);
        setBorder(Hud.panelBorder("Info"));
        setForeground(Hud.FOREGROUND);
        setMaximumSize(new Dimension(200, 2147483647));
        setMinimumSize(new Dimension(200, 150));
        setPreferredSize(new Dimension(200, 300));
        jScrollPane1.setBackground(Hud.BACKGROUND);
        jScrollPane1.setBorder(null);
        jScrollPane1.setForeground(Hud.FOREGROUND);
        sheetPane.setBackground(Hud.BACKGROUND);
        sheetPane.setBorder(null);
        sheetPane.setForeground(Hud.FOREGROUND);
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

        pinToggle.setText("Pin line");
        pinToggle.setVisible(false);
        pinToggle.addActionListener(evt -> this.onPinToggled.accept(this.pinToggle.isSelected()));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.01;
        add(pinToggle, gridBagConstraints);

        sellButton.setText("Sell");
        sellButton.setForeground(SELL_TEXT_COLOR);
        sellButton.setFace(ControlFace.button().withChip(GameKey.SELL.chip()));
        sellButton.addActionListener(this::onSellClicked);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.01;
        add(sellButton, gridBagConstraints);

    }

    private void onSellClicked(ActionEvent evt) {
        this.sellCurrentTower();
    }

}
