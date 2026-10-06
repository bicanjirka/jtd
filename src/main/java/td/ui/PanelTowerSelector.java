package td.ui;

import td.TowerDefense;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerInspection;
import td.ui.render.InfoSheet;
import td.util.GameHost;
import td.util.GameWorld;
import td.util.ThreadConfined;

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
 * greyed out while unaffordable. Icons come from {@link Java2DFrameRenderer#renderTowerIcon}, so
 * they match the board.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelTowerSelector extends JPanel implements EconomyListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final int ICON_SIZE = 32;

    private final HudToggleButton[] buttons;
    private final TowerInspection[] shopTowers;
    private final float[] towerRanges;
    private final List<TowerFactory.Type> towerTypes;
    private GameWorld context;
    private TowerDefense mainApp;
    private boolean placing = false;

    public PanelTowerSelector() {
        initComponents();
        this.towerTypes = new ArrayList<>();

        TowerFactory.Type[] types = TowerFactory.Type.values();
        for (TowerFactory.Type type : types) {
            this.towerTypes.add(TowerFactory.Type.valueOf(type.toString()));
        }

        this.buttons = new HudToggleButton[this.towerTypes.size()];
        this.shopTowers = new TowerInspection[this.towerTypes.size()];
        this.towerRanges = new float[this.towerTypes.size()];
    }

    /** An explicit disabled icon: Swing only greys out a plain {@link ImageIcon} automatically. */
    private static BufferedImage grayedOut(BufferedImage image) {
        Image filtered = GrayFilter.createDisabledImage(image);
        ImageIcon loader = new ImageIcon(filtered);
        BufferedImage result = new BufferedImage(loader.getIconWidth(), loader.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        loader.paintIcon(null, g2, 0, 0);
        g2.dispose();
        return result;
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

        GameWorld previewWorld = new GameWorld(GameHost.noOp());
        Java2DFrameRenderer iconRenderer = new Java2DFrameRenderer();

        for (int i = 0; i < this.towerTypes.size(); i++) {
            TowerFactory.Type type = this.towerTypes.get(i);
            Tower tower = TowerFactory.createTower(type, previewWorld, 0, 0);
            this.shopTowers[i] = tower.inspect();
            this.towerRanges[i] = tower.getRange();

            BufferedImage icon = iconRenderer.renderTowerIcon(TowerSpriteFrameBuilder.bodyPaletteFor(type), ICON_SIZE);
            this.buttons[i].setIcon(new SharpImageIcon(icon));
            this.buttons[i].setDisabledIcon(new SharpImageIcon(grayedOut(icon)));
        }

        this.context.economy().addEconomyListener(this);
    }

    public void startPlacing(TowerFactory.Type t, float r) {
        this.placing = true;
        this.mainApp.startPlacing(t, r);
    }

    public void stopPlacing() {
        this.untoggleAll();
        this.placing = false;
    }

    /** May run on the game-loop thread. */
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < this.buttons.length; i++) {
                this.buttons[i].setEnabled(state.canAfford(this.context.towers().priceOf(this.towerTypes.get(i))));
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
        this.mainApp.showInfoSheet(this.shopSheet(i));
        this.startPlacing(this.towerTypes.get(i), this.towerRanges[i]);
    }

    /** The toolbar description at the price the next copy costs now. */
    private InfoSheet shopSheet(int i) {
        return TowerSheetText.shop(this.shopTowers[i].withPrice(this.context.towers().priceOf(this.towerTypes.get(i))));
    }

    private void mouseOver(int i) {
        if (!this.placing) {
            this.mainApp.showInfoSheet(this.shopSheet(i));
        }
    }

    private void initComponents() {

        setLayout(new GridBagLayout());

        setBackground(Hud.BACKGROUND);
        setBorder(Hud.panelBorder("Towers"));
        setForeground(Hud.FOREGROUND);
    }


}
