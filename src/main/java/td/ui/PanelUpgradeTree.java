package td.ui;

import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.util.GameWorld;
import td.util.ThreadConfined;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.List;
import java.util.function.Consumer;

/**
 * The selected tower's upgrade tree: per {@link UpgradeSlot}, a header with what the slot holds and
 * a numbered button per offered node. Replaces the wave preview while a tower is selected.
 * <p>
 * No slot offers more than three nodes at once. Numbering runs across all slots in {@code offered}
 * order, so a button's number is the key that buys it.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelUpgradeTree extends JPanel implements EconomyListener {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int BUTTONS_PER_SLOT = 3;
    private static final UpgradeSlot[] SLOTS = UpgradeSlot.values();

    private final JLabel[] slotHeaders = new JLabel[SLOTS.length];
    private final HudButton[][] slotButtons = new HudButton[SLOTS.length][BUTTONS_PER_SLOT];
    private final UpgradeNode[][] slotNodes = new UpgradeNode[SLOTS.length][BUTTONS_PER_SLOT];

    private GameWorld context;
    private Tower tower;
    private boolean levelEnded = false;
    private Consumer<UpgradeNode> onHover = node -> {
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

    public void onHover(Consumer<UpgradeNode> listener) {
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
     * pulse, since gate progress changes without economy events.
     */
    public void refresh() {
        for (UpgradeSlot slot : SLOTS) {
            for (int i = 0; i < BUTTONS_PER_SLOT; i++) {
                this.slotButtons[slot.ordinal()][i].setVisible(false);
                this.slotNodes[slot.ordinal()][i] = null;
            }
            this.slotHeaders[slot.ordinal()].setText(slot.toString());
        }
        if (this.tower == null) {
            return;
        }
        List<UpgradeNode> offered = this.tower.offeredUpgrades(this.context);
        int[] used = new int[SLOTS.length];
        for (int i = 0; i < offered.size(); i++) {
            UpgradeNode node = offered.get(i);
            int slotIndex = node.slot().ordinal();
            int buttonIndex = used[slotIndex]++;
            if (buttonIndex >= BUTTONS_PER_SLOT) {
                continue;
            }
            this.slotNodes[slotIndex][buttonIndex] = node;
            HudButton button = this.slotButtons[slotIndex][buttonIndex];
            button.setVisible(true);
            button.setText((i + 1) + " " + node.displayName() + " ( $" + node.price() + " )");
            boolean available = !this.levelEnded
                    && node.gate().isSatisfied(this.tower, this.context)
                    && this.context.economy().canPay(node.price());
            button.setEnabled(available);
        }
        for (UpgradeSlot slot : SLOTS) {
            this.slotHeaders[slot.ordinal()].setText(this.headerFor(slot, offered));
        }
    }

    private String headerFor(UpgradeSlot slot, List<UpgradeNode> offered) {
        return this.tower.upgrades().tip(slot)
                .map(tip -> slot + "  " + tip.displayName() + " ✔")
                .orElseGet(() -> {
                    boolean reachable = offered.stream().anyMatch(n -> n.slot() == slot);
                    return reachable ? slot + "  —" : slot + "  (locked)";
                });
    }

    private void buttonClicked(int slotIndex, int buttonIndex) {
        UpgradeNode node = this.slotNodes[slotIndex][buttonIndex];
        if (node == null || this.tower == null) {
            return;
        }
        if (this.tower.buyUpgrade(node)) {
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
        for (UpgradeSlot slot : SLOTS) {
            int slotIndex = slot.ordinal();
            JLabel header = new JLabel(slot.toString());
            header.setForeground(new Color(220, 255, 220));
            this.slotHeaders[slotIndex] = header;
            GridBagConstraints headerConstraints = new GridBagConstraints();
            headerConstraints.gridx = 0;
            headerConstraints.gridy = row++;
            headerConstraints.anchor = GridBagConstraints.WEST;
            headerConstraints.fill = GridBagConstraints.HORIZONTAL;
            headerConstraints.weightx = 0.01;
            headerConstraints.insets = new Insets(slotIndex == 0 ? 0 : 6, 0, 2, 0);
            add(header, headerConstraints);

            for (int i = 0; i < BUTTONS_PER_SLOT; i++) {
                HudButton button = new HudButton("");
                button.setVisible(false);
                int capturedSlot = slotIndex;
                int capturedButton = i;
                button.addActionListener(evt -> this.buttonClicked(capturedSlot, capturedButton));
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent evt) {
                        UpgradeNode node = PanelUpgradeTree.this.slotNodes[capturedSlot][capturedButton];
                        if (node != null) {
                            PanelUpgradeTree.this.onHover.accept(node);
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
            }
        }
    }
}
