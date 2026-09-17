package td.ui;

import td.TowerDefense;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameHost;
import td.util.GameWorld;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * The tower toolbar: one {@link HudToggleButton} per {@link TowerFactory.Type}, in enum order,
 * so the buttons follow whatever that enum declares rather than a hand-maintained list -
 * {@code TowerDefense.TOWER_PLACEMENT_KEYS} likewise assigns one keyboard shortcut per ordinal.
 * Buttons the player cannot currently afford are greyed out, which is why this panel is an
 * {@link EconomyListener}.
 * <p>
 * Icons come from {@link Java2DFrameRenderer#renderTowerIcon}, i.e. the same shapes the board
 * uses, so a tower's icon cannot drift from how it actually looks once placed. They are a
 * toggle rather than a push control because picking a tower is a mode, but they look exactly
 * like every other control - see {@link Hud}.
 */
public class PanelTowerSelector extends JPanel implements EconomyListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final int ICON_SIZE = 32;

    private final HudToggleButton[] buttons;
    private final String[] infoText;
    private final float[] towerRanges;
    private GameWorld context;
    private TowerDefense mainApp;
    private final List<TowerFactory.Type> towerTypes;
    private boolean placing = false;

    public PanelTowerSelector() {
        initComponents();
        this.towerTypes = new ArrayList<>();

        TowerFactory.Type[] types = TowerFactory.Type.values();
        for (TowerFactory.Type type : types) {
            this.towerTypes.add(TowerFactory.Type.valueOf(type.toString()));
        }

        this.buttons = new HudToggleButton[this.towerTypes.size()];
        this.infoText = new String[this.towerTypes.size()];
        this.towerRanges = new float[this.towerTypes.size()];
    }

    private void makeButtons() {
        GridBagConstraints gridBagConstraints;
        HudToggleButton tempToggle;
        for (int i = 0; i < this.towerTypes.size(); i++) {
            tempToggle = new HudToggleButton();
            this.buttons[i] = tempToggle;
            final int n = i;
            tempToggle.addActionListener(evt -> doPlace(n));
            tempToggle.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent evt) {
                    mouseOver(n);
                }
            });
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = n;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.insets = new Insets(0, 0, 0, 4);
            add(tempToggle, gridBagConstraints);
        }
    }

    public void doInit(GameWorld c, TowerDefense mainApp) {
        this.context = c;
        this.mainApp = mainApp;

        this.makeButtons();

        GameWorld tempContext = new GameWorld(GameHost.noOp());
        Java2DFrameRenderer iconRenderer = new Java2DFrameRenderer();

        for (int i = 0; i < this.towerTypes.size(); i++) {
            TowerFactory.Type type = this.towerTypes.get(i);
            Tower tempTower = TowerFactory.createTower(type, tempContext, 0, 0);
            this.infoText[i] = tempTower.getInfoString();
            this.towerRanges[i] = tempTower.getRange();

            BufferedImage icon = iconRenderer.renderTowerIcon(TowerSpriteFrameBuilder.bodyPaletteFor(type), ICON_SIZE);
            this.buttons[i].setIcon(new SharpImageIcon(icon));
            this.buttons[i].setDisabledIcon(new SharpImageIcon(grayedOut(icon)));
        }

        this.context.addEconomyListener(this);
    }

    public void startPlacing(TowerFactory.Type t, float r) {
        this.placing = true;
        this.mainApp.startPlacing(t, r);
    }

    public void stopPlacing() {
        this.untoggleAll();
        this.placing = false;
    }

    /** Also reachable from the game-loop thread - see GameWorld.apply()'s callers. */
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < this.buttons.length; i++) {
                this.buttons[i].setEnabled(state.canAfford(this.towerTypes.get(i).price));
            }
        });
    }

    private void untoggleAll() {
        for (HudToggleButton button : this.buttons) {
            button.setSelected(false);
        }
    }

    public void doPlace(int i) {
        this.untoggleAll();
        this.buttons[i].setSelected(true);
        this.mainApp.setInfoText(this.infoText[i]);
        this.startPlacing(this.towerTypes.get(i), this.towerRanges[i]);
    }

    /**
     * Swing's built-in "gray out the icon when disabled" behaviour only fires for a plain
     * {@link ImageIcon}; {@link SharpImageIcon} implements {@code Icon} directly (for its
     * bilinear repaint), so an unaffordable button was correctly disabled but still painted
     * as if enabled without this explicit disabled icon.
     */
    private static BufferedImage grayedOut(BufferedImage image) {
        Image filtered = GrayFilter.createDisabledImage(image);
        ImageIcon loader = new ImageIcon(filtered);
        BufferedImage result = new BufferedImage(loader.getIconWidth(), loader.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        loader.paintIcon(null, g2, 0, 0);
        g2.dispose();
        return result;
    }

    private void mouseOver(int i) {
        if (!this.placing) {
            this.mainApp.setInfoText(this.infoText[i]);
        }
    }

    private void initComponents() {

        setLayout(new GridBagLayout());

        setBackground(Hud.BACKGROUND);
        setBorder(Hud.panelBorder("Towers"));
        setForeground(Hud.FOREGROUND);
    }


}