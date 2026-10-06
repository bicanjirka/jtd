package td.ui;

import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.tower.upgrade.UpgradeDecision;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeState;
import td.ui.render.Palette;
import td.ui.render.SheetLine.Glyph;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * The selected tower's upgrade tree, under its XP bar (with a tick at the next node XP opens):
 * per {@link UpgradeSlot}, a header in the slot's colour with
 * what the slot holds, a lock row per exclusive choice made there, and a numbered button per
 * offered node saying its price or why it can't be bought yet. Buttons of one exclusive choice are
 * joined by a bracket, with "1 of N" beside the header. Replaces the wave preview while a tower is
 * selected.
 * <p>
 * No slot offers more than three nodes at once. Numbering runs across all slots in {@code offered}
 * order, so a button's number is the key that buys it.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelUpgradeTree extends JPanel implements EconomyListener {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int BUTTONS_PER_SLOT = 3;
    /** A chain root's rival and a level IV branch's in the head; two specials passed over. */
    private static final int DECISIONS_PER_SLOT = 2;
    private static final float DECISION_FONT_SIZE = 10f;
    private static final float XP_FONT_SIZE = 10f;
    private static final UpgradeSlot[] SLOTS = UpgradeSlot.values();
    private static final Color LOCKED_COLOR = new Color(150, 170, 150);
    private static final int PIP_BOX = 11;
    private static final float PIP_SIZE = 4.2f;

    private final JLabel xpLabel = new JLabel("");
    private final XpBar xpBar = new XpBar();
    private final JLabel[] slotHeaders = new JLabel[SLOTS.length];
    private final JLabel[] choiceLabels = new JLabel[SLOTS.length];
    private final JLabel[][] decisionRows = new JLabel[SLOTS.length][DECISIONS_PER_SLOT];
    private final ChoiceBracket[][] brackets = new ChoiceBracket[SLOTS.length][BUTTONS_PER_SLOT];
    private final HudButton[][] slotButtons = new HudButton[SLOTS.length][BUTTONS_PER_SLOT];
    private final UpgradeOffer[][] slotOffers = new UpgradeOffer[SLOTS.length][BUTTONS_PER_SLOT];
    private final Java2DFrameRenderer glyphRenderer = new Java2DFrameRenderer();

    private GameWorld context;
    private Tower tower;
    private boolean levelEnded = false;
    private Consumer<UpgradeOffer> onHover = offer -> {
    };
    private Runnable onHoverEnd = () -> {
    };
    private Runnable onBought = () -> {
    };

    public PanelUpgradeTree() {
        initComponents();
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

    void onHover(Consumer<UpgradeOffer> listener) {
        this.onHover = listener;
    }

    public void onHoverEnd(Runnable listener) {
        this.onHoverEnd = listener;
    }

    public void onBought(Runnable listener) {
        this.onBought = listener;
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
                this.slotOffers[slot.ordinal()][i] = null;
            }
        }
        if (this.tower == null) {
            return;
        }
        List<UpgradeNode> offered = this.tower.offeredUpgrades(this.context);
        int[] used = new int[SLOTS.length];
        for (int i = 0; i < offered.size(); i++) {
            UpgradeOffer offer = UpgradeOffer.of(offered, i, this.tower, this.context);
            int slotIndex = offer.node().slot().ordinal();
            int buttonIndex = used[slotIndex]++;
            if (buttonIndex >= BUTTONS_PER_SLOT) {
                continue;
            }
            this.slotOffers[slotIndex][buttonIndex] = offer;
            HudButton button = this.slotButtons[slotIndex][buttonIndex];
            button.setVisible(true);
            button.setText(UpgradeSheetText.buttonText(offer));
            button.setEnabled(!this.levelEnded && offer.buyable());
            if (button.getModel().isRollover()) {
                this.onHover.accept(offer);
            }
        }
        int xp = this.tower.experience().xp();
        Optional<UpgradeNode> nextXpGate = UpgradeSheetText.nextXpGate(offered, xp);
        this.xpLabel.setText(UpgradeSheetText.xpLabel(xp, nextXpGate));
        this.xpBar.show(xp, nextXpGate.map(node -> OptionalInt.of(node.xp())).orElse(OptionalInt.empty()));
        UpgradeState owned = this.tower.upgrades();
        List<UpgradeDecision> decisions = this.tower.upgradeTree().decisions(owned);
        for (UpgradeSlot slot : SLOTS) {
            boolean reachable = offered.stream().anyMatch(n -> n.slot() == slot);
            JLabel header = this.slotHeaders[slot.ordinal()];
            header.setText(UpgradeSheetText.slotHeader(slot, owned, reachable));
            header.setForeground(owned.countIn(slot) > 0 || reachable
                    ? Java2DFrameRenderer.colorFor(TowerSpriteFrameBuilder.slotPaletteFor(slot))
                    : LOCKED_COLOR);
            this.refreshChoiceMark(slot);
            this.refreshDecisionRows(slot, decisions);
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

    private void refreshDecisionRows(UpgradeSlot slot, List<UpgradeDecision> decisions) {
        List<String> rows = UpgradeSheetText.decisionRows(decisions.stream().filter(d -> d.chosen().slot() == slot).toList());
        for (int i = 0; i < DECISIONS_PER_SLOT; i++) {
            JLabel row = this.decisionRows[slot.ordinal()][i];
            row.setVisible(i < rows.size());
            if (i < rows.size()) {
                row.setText(rows.get(i));
            }
        }
    }

    private void buttonClicked(int slotIndex, int buttonIndex) {
        UpgradeOffer offer = this.slotOffers[slotIndex][buttonIndex];
        if (offer == null || this.tower == null) {
            return;
        }
        if (this.tower.buyUpgrade(offer.node())) {
            this.onBought.run();
        }
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

            for (int i = 0; i < DECISIONS_PER_SLOT; i++) {
                JLabel decision = new JLabel("");
                decision.setForeground(Hud.FOREGROUND);
                decision.setFont(Hud.LABEL_FONT.deriveFont(DECISION_FONT_SIZE));
                decision.setIcon(new PaintedIcon(PIP_BOX, PIP_BOX,
                        g2 -> this.glyphRenderer.paintRowGlyph(g2, Glyph.LOCK, Optional.empty(), PIP_SIZE)));
                decision.setVisible(false);
                this.decisionRows[slotIndex][i] = decision;
                GridBagConstraints decisionConstraints = new GridBagConstraints();
                decisionConstraints.gridx = 0;
                decisionConstraints.gridy = row++;
                decisionConstraints.gridwidth = 2;
                decisionConstraints.anchor = GridBagConstraints.WEST;
                decisionConstraints.fill = GridBagConstraints.HORIZONTAL;
                decisionConstraints.weightx = 0.01;
                decisionConstraints.insets = new Insets(0, 0, 2, 0);
                add(decision, decisionConstraints);
            }

            for (int i = 0; i < BUTTONS_PER_SLOT; i++) {
                HudButton button = new HudButton("");
                button.setVisible(false);
                int capturedSlot = slotIndex;
                int capturedButton = i;
                button.addActionListener(evt -> this.buttonClicked(capturedSlot, capturedButton));
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent evt) {
                        UpgradeOffer offer = PanelUpgradeTree.this.slotOffers[capturedSlot][capturedButton];
                        if (offer != null) {
                            PanelUpgradeTree.this.onHover.accept(offer);
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent evt) {
                        PanelUpgradeTree.this.onHoverEnd.run();
                    }
                });
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
