package td.ui;

import td.tower.Tower;
import td.util.GameWorld;
import td.util.ThreadConfined;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;

/**
 * The right-hand console: the tower-info pane over the wave preview, which the selected tower's
 * Upgrades panel replaces. The status and the speed buttons are the header's.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelGameConsole extends JPanel {

    /** The console's width; the header's speed buttons are as wide, so they stand above it. */
    public static final int WIDTH = 300;

    @Serial
    private static final long serialVersionUID = 1L;

    private final PanelTowerInfo panelTowerInfo = new PanelTowerInfo();
    private final PanelWaveInfo panelWaveInfo = new PanelWaveInfo();
    private final PanelUpgradeTree panelUpgradeTree = new PanelUpgradeTree();

    public PanelGameConsole() {
        initComponents();
        this.panelTowerInfo.onDeselected(this::unselectTower);
    }

    public PanelTowerInfo getTowerInfo() {
        return this.panelTowerInfo;
    }

    public PanelWaveInfo getWaveInfo() {
        return this.panelWaveInfo;
    }

    /** The layer the panels' tooltips open on. */
    public void setTooltips(TooltipLayer tooltips) {
        this.panelTowerInfo.setTooltips(tooltips);
        this.panelUpgradeTree.setTooltips(tooltips);
        this.panelWaveInfo.setTooltips(tooltips);
    }

    public void setGameWorld(GameWorld world) {
        this.panelTowerInfo.setGameWorld(world);
        this.panelWaveInfo.setGameWorld(world);
        this.panelUpgradeTree.setGameWorld(world);
    }

    /** Shows {@code t}'s status and swaps the wave preview for its upgrade panel. */
    public void selectTower(Tower t) {
        this.panelTowerInfo.setTower(t);
        this.panelUpgradeTree.setTower(t);
        this.panelWaveInfo.setVisible(false);
        this.panelUpgradeTree.setVisible(true);
    }

    /** Clears the selection and swaps the wave preview back. Also runs after a sale. */
    public void unselectTower() {
        this.panelTowerInfo.unselectTower();
        this.panelUpgradeTree.setTower(null);
        this.panelUpgradeTree.setVisible(false);
        this.panelWaveInfo.setVisible(true);
    }

    public void setLevelEnded(boolean ended) {
        this.panelTowerInfo.setLevelEnded(ended);
        this.panelUpgradeTree.setLevelEnded(ended);
    }

    /**
     * Refreshes the selected tower's panels each render pulse, since gate progress changes without
     * any economy event.
     */
    public void refreshSelected() {
        this.panelUpgradeTree.refresh();
    }

    private void initComponents() {
        this.setLayout(new GridBagLayout());
        this.setBackground(new Color(0, 0, 0));
        this.setFocusable(false);
        Dimension size = new Dimension(WIDTH, 402);
        this.setMaximumSize(new Dimension(WIDTH, 2147483647));
        this.setMinimumSize(new Dimension(WIDTH, 263));
        this.setPreferredSize(size);

        this.panelTowerInfo.setFocusable(false);
        GridBagConstraints info = new GridBagConstraints();
        info.gridx = 0;
        info.gridy = 0;
        info.fill = GridBagConstraints.BOTH;
        info.anchor = GridBagConstraints.PAGE_START;
        info.weightx = 0.01;
        info.weighty = 1.0;
        info.insets = new Insets(0, 2, 0, 0);
        this.add(this.panelTowerInfo, info);

        this.panelWaveInfo.setMinimumSize(null);
        this.panelWaveInfo.setName("Waving :)");
        this.panelWaveInfo.setPreferredSize(null);
        GridBagConstraints below = new GridBagConstraints();
        below.gridx = 0;
        below.gridy = 1;
        below.fill = GridBagConstraints.HORIZONTAL;
        below.anchor = GridBagConstraints.PAGE_END;
        below.insets = new Insets(0, 2, 0, 0);
        this.add(this.panelWaveInfo, below);

        // Shares panelWaveInfo's cell; the two toggle by visibility.
        this.panelUpgradeTree.setFocusable(false);
        this.panelUpgradeTree.setVisible(false);
        this.add(this.panelUpgradeTree, below);
    }
}
