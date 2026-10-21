package td.ui;

import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeState;
import td.ui.render.Palette;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;
import td.util.GameWorld;
import td.util.ThreadConfined;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The selected tower's upgrade tree, under its XP bar (with a tick at the next node XP opens):
 * per {@link UpgradeSlot}, a header in the slot's colour with
 * what the slot holds, and a numbered button per
 * offered node saying its price or why it can't be bought yet. Buttons of one exclusive choice are
 * joined by a bracket, with "1 of N" beside the header. Replaces the wave preview while a tower is
 * selected.
 * <p>
 * No slot offers more than three nodes at once. A button's number is the key that buys it, the
 * same for every tower ({@link td.tower.upgrade.UpgradeTree#keyOf(td.tower.upgrade.UpgradeNode)}), and a
 * slot lists its buttons in key order.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelUpgradeTree extends JPanel implements EconomyListener {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int BUTTONS_PER_SLOT = 3;
    private static final int BUTTON_ICON = 14;
    private static final float XP_FONT_SIZE = 10f;
    private static final UpgradeSlot[] SLOTS = UpgradeSlot.values();
    private static final Color LOCKED_COLOR = new Color(150, 170, 150);
    private static final int PIP_BOX = 11;
    private static final float PIP_SIZE = 4.2f;

    private final JLabel xpLabel = new JLabel("");
    private final XpBar xpBar = new XpBar();
    private final JLabel[] slotHeaders = new JLabel[SLOTS.length];
    private final JLabel[] choiceLabels = new JLabel[SLOTS.length];
    private final ChoiceBracket[][] brackets = new ChoiceBracket[SLOTS.length][BUTTONS_PER_SLOT];
    private final HudButton[][] slotButtons = new HudButton[SLOTS.length][BUTTONS_PER_SLOT];
    private final UpgradeOffer[][] slotOffers = new UpgradeOffer[SLOTS.length][BUTTONS_PER_SLOT];
    private final Java2DFrameRenderer glyphRenderer = new Java2DFrameRenderer();

    private GameWorld context;
    private Tower tower;
    private boolean levelEnded = false;
    private TooltipLayer tooltips = null;

    public PanelUpgradeTree() {
        initComponents();
    }

    /** Each button's hover is a tooltip of what it offers now, keyed by the button's place so a purchase swaps it. */
    public void setTooltips(TooltipLayer tooltips) {
        this.tooltips = tooltips;
        for (UpgradeSlot slot : SLOTS) {
            for (int i = 0; i < BUTTONS_PER_SLOT; i++) {
                String place = slot + ":" + i;
                UpgradeOffer[] offers = this.slotOffers[slot.ordinal()];
                int index = i;
                tooltips.watchButton(this.slotButtons[slot.ordinal()][i], () -> Optional.ofNullable(offers[index])
                        .map(offer -> new SheetTooltip(TooltipKey.of("button", place), UpgradeSheetText.hover(offer))));
            }
        }
    }

    public void setGameWorld(GameWorld context) {
        this.context = context;
        this.context.economy().addEconomyListener(this);
    }

    public void setTower(Tower tower) {
        this.tower = tower;
        this.refresh();
    }

    public void setLevelEnded(boolean ended) {
        this.levelEnded = ended;
        this.refresh();
    }

    /** May run on the game-loop thread. */
    @Override
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(this::refresh);
    }

    /**
     * Rebuilds headers and buttons from the tower's offered upgrades. Also called from the render
     * pulse, since gate progress changes without economy events; a hovered button's sheet follows.
     */
    public void refresh() {
        for (UpgradeSlot slot : SLOTS) {
            for (int i = 0; i < BUTTONS_PER_SLOT; i++) {
                this.slotButtons[slot.ordinal()][i].setVisible(false);
                this.slotButtons[slot.ordinal()][i].setIcon(null);
                this.slotOffers[slot.ordinal()][i] = null;
            }
        }
        if (this.tower == null) {
            return;
        }
        List<UpgradeNode> offered = this.tower.offeredUpgrades(this.context).stream()
                .sorted(Comparator.comparingInt(this.tower.upgradeTree()::keyOf)).toList();
        int[] used = new int[SLOTS.length];
        for (UpgradeNode node : offered) {
            UpgradeOffer offer = UpgradeOffer.of(node, offered, this.tower, this.context);
            int slotIndex = offer.node().slot().ordinal();
            int buttonIndex = used[slotIndex]++;
            if (buttonIndex >= BUTTONS_PER_SLOT) {
                continue;
            }
            this.slotOffers[slotIndex][buttonIndex] = offer;
            HudButton button = this.slotButtons[slotIndex][buttonIndex];
            button.setVisible(true);
            button.setText(UpgradeSheetText.buttonText(offer));
            button.setIcon(new PaintedIcon(BUTTON_ICON, BUTTON_ICON, g2 -> TermIconPainter.paint(g2, offer.face(), BUTTON_ICON)));
            button.setEnabled(!this.levelEnded && offer.buyable());
        }
        int xp = this.tower.experience().xp();
        Optional<UpgradeNode> nextXpGate = UpgradeSheetText.nextXpGate(offered, xp);
        this.xpLabel.setText(UpgradeSheetText.xpLabel(xp, nextXpGate));
        this.xpBar.show(xp, nextXpGate.map(node -> OptionalInt.of(node.xp())).orElse(OptionalInt.empty()));
        UpgradeState owned = this.tower.upgrades();
        for (UpgradeSlot slot : SLOTS) {
            boolean reachable = offered.stream().anyMatch(n -> n.slot() == slot);
            JLabel header = this.slotHeaders[slot.ordinal()];
            header.setText(UpgradeSheetText.slotHeader(slot, owned, reachable));
            header.setForeground(owned.countIn(slot) > 0 || reachable
                    ? Java2DFrameRenderer.colorFor(TowerSpriteFrameBuilder.slotPaletteFor(slot))
                    : LOCKED_COLOR);
            this.refreshChoiceMark(slot);
        }
        if (this.tooltips != null) {
            this.tooltips.refreshButtons();
        }
    }

    /** The bracket joining the slot's buttons that belong to one choice, and its "1 of N". */
    private void refreshChoiceMark(UpgradeSlot slot) {
        UpgradeOffer[] offers = this.slotOffers[slot.ordinal()];
        int first = -1;
        int last = -1;
        for (int i = 0; i < BUTTONS_PER_SLOT; i++) {
            if (offers[i] != null && offers[i].inChoice()) {
                first = first < 0 ? i : first;
                last = i;
            }
        }
        for (int i = 0; i < BUTTONS_PER_SLOT; i++) {
            boolean member = offers[i] != null && offers[i].inChoice();
            ChoiceBracket.Segment segment;
            if (first < 0 || i < first || i > last) {
                segment = ChoiceBracket.Segment.NONE;
            } else if (!member) {
                segment = ChoiceBracket.Segment.PASS;
            } else if (i == first) {
                segment = ChoiceBracket.Segment.FIRST;
            } else {
                segment = i == last ? ChoiceBracket.Segment.LAST : ChoiceBracket.Segment.MIDDLE;
            }
            this.brackets[slot.ordinal()][i].setSegment(segment);
        }
        this.choiceLabels[slot.ordinal()].setText(
                first < 0 ? "" : UpgradeSheetText.choiceLabel(offers[first].rivals().size() + 1));
    }

    private void buttonClicked(int slotIndex, int buttonIndex) {
        UpgradeOffer offer = this.slotOffers[slotIndex][buttonIndex];
        if (offer == null || this.tower == null) {
            return;
        }
        this.tower.buyUpgrade(offer.node());
        this.refresh();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBackground(new Color(0, 0, 0));
        setBorder(Hud.panelBorder("Upgrades"));
        setForeground(new Color(220, 255, 220));

        int row = 0;
        this.xpLabel.setForeground(Hud.FOREGROUND);
        this.xpLabel.setFont(Hud.LABEL_FONT.deriveFont(XP_FONT_SIZE));
        GridBagConstraints xpLabelConstraints = new GridBagConstraints();
        xpLabelConstraints.gridx = 0;
        xpLabelConstraints.gridy = row++;
        xpLabelConstraints.gridwidth = 2;
        xpLabelConstraints.fill = GridBagConstraints.HORIZONTAL;
        xpLabelConstraints.weightx = 0.01;
        add(this.xpLabel, xpLabelConstraints);
        GridBagConstraints xpBarConstraints = new GridBagConstraints();
        xpBarConstraints.gridx = 0;
        xpBarConstraints.gridy = row++;
        xpBarConstraints.gridwidth = 2;
        xpBarConstraints.fill = GridBagConstraints.HORIZONTAL;
        xpBarConstraints.weightx = 0.01;
        xpBarConstraints.insets = new Insets(1, 0, 6, 0);
        add(this.xpBar, xpBarConstraints);
        for (UpgradeSlot slot : SLOTS) {
            int slotIndex = slot.ordinal();
            JLabel header = new JLabel(SheetNumbers.titleCase(slot));
            header.setForeground(Hud.FOREGROUND);
            header.setFont(Hud.LABEL_FONT);
            Palette slotPalette = TowerSpriteFrameBuilder.slotPaletteFor(slot);
            header.setIcon(new PaintedIcon(PIP_BOX, PIP_BOX,
                    g2 -> this.glyphRenderer.paintRowGlyph(g2, Glyph.PIP, Optional.of(slotPalette), PIP_SIZE)));
            this.slotHeaders[slotIndex] = header;
            JLabel choiceLabel = new JLabel("");
            choiceLabel.setForeground(Hud.FOREGROUND);
            choiceLabel.setFont(Hud.LABEL_FONT);
            this.choiceLabels[slotIndex] = choiceLabel;
            JPanel headerRow = new JPanel(new BorderLayout());
            headerRow.setOpaque(false);
            headerRow.add(header, BorderLayout.CENTER);
            headerRow.add(choiceLabel, BorderLayout.EAST);
            GridBagConstraints headerConstraints = new GridBagConstraints();
            headerConstraints.gridx = 0;
            headerConstraints.gridy = row++;
            headerConstraints.gridwidth = 2;
            headerConstraints.anchor = GridBagConstraints.WEST;
            headerConstraints.fill = GridBagConstraints.HORIZONTAL;
            headerConstraints.weightx = 0.01;
            headerConstraints.insets = new Insets(slotIndex == 0 ? 0 : 6, 0, 2, 0);
            add(headerRow, headerConstraints);

            for (int i = 0; i < BUTTONS_PER_SLOT; i++) {
                HudButton button = new HudButton("");
                button.setVisible(false);
                int capturedButton = i;
                button.addActionListener(evt -> this.buttonClicked(slotIndex, capturedButton));
                this.slotButtons[slotIndex][i] = button;
                GridBagConstraints buttonConstraints = new GridBagConstraints();
                buttonConstraints.gridx = 0;
                buttonConstraints.gridy = row++;
                buttonConstraints.fill = GridBagConstraints.HORIZONTAL;
                buttonConstraints.anchor = GridBagConstraints.WEST;
                buttonConstraints.weightx = 0.01;
                add(button, buttonConstraints);
                ChoiceBracket bracket = new ChoiceBracket();
                this.brackets[slotIndex][i] = bracket;
                GridBagConstraints bracketConstraints = new GridBagConstraints();
                bracketConstraints.gridx = 1;
                bracketConstraints.gridy = buttonConstraints.gridy;
                bracketConstraints.fill = GridBagConstraints.VERTICAL;
                add(bracket, bracketConstraints);
            }
        }
    }
}
