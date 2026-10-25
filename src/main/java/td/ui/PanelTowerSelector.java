package td.ui;

import td.TowerDefense;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerInspection;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;
import td.util.GameHost;
import td.util.GameWorld;
import td.util.ThreadConfined;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The tower toolbar: one tile per {@link TowerFactory.Type}, in enum order, bordered in the tower's colour with its
 * price under it. A tower the player can't afford keeps its colours: its border dims and its price turns red. Icons
 * come from {@link Java2DFrameRenderer#renderTowerIcon}, so they match the board.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelTowerSelector extends JPanel implements EconomyListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final int ICON_SIZE = 32;

    private final HudToggleButton[] buttons;
    private final JLabel[] prices;
    private final TowerInspection[] shopTowers;
    private final float[] towerRanges;
    private final List<TowerFactory.Type> towerTypes;
    private GameWorld context;
    private TowerDefense mainApp;
    private TooltipLayer tooltips;

    public PanelTowerSelector() {
        initComponents();
        this.towerTypes = new ArrayList<>();

        TowerFactory.Type[] types = TowerFactory.Type.values();
        for (TowerFactory.Type type : types) {
            this.towerTypes.add(TowerFactory.Type.valueOf(type.toString()));
        }

        this.buttons = new HudToggleButton[this.towerTypes.size()];
        this.prices = new JLabel[this.towerTypes.size()];
        this.shopTowers = new TowerInspection[this.towerTypes.size()];
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
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = n;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.insets = new Insets(0, 0, 0, 4);
            add(tempToggle, gridBagConstraints);
            JLabel price = new JLabel(" ", SwingConstants.CENTER);
            price.setFont(Hud.PRICE_FONT);
            price.setForeground(Hud.FOREGROUND);
            this.prices[i] = price;
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = n;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.insets = new Insets(1, 0, 0, 4);
            add(price, gridBagConstraints);
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
            this.buttons[i].setFace(ControlFace.tile(
                    Java2DFrameRenderer.colorFor(TowerSpriteFrameBuilder.bodyPaletteFor(type)))
                    .withChip(String.valueOf(Character.toUpperCase(type.placementKey))));
        }

        this.context.economy().addEconomyListener(this);
        this.showPrices(this.context.economy().state());
    }

    /** Once {@link #doInit} made the buttons: each one's hover is a tooltip of the tower it builds, at today's price. */
    public void setTooltips(TooltipLayer layer) {
        this.tooltips = layer;
        for (int i = 0; i < this.buttons.length; i++) {
            int index = i;
            layer.watchButton(this.buttons[i], () -> Optional.of(new SheetTooltip(
                    TooltipKey.of("shop", this.towerTypes.get(index).name()),
                    TowerSheetText.shopTooltip(this.priced(index)))));
        }
    }

    public void startPlacing(TowerFactory.Type t, float r) {
        this.mainApp.startPlacing(t, r);
    }

    public void stopPlacing() {
        this.untoggleAll();
    }

    /** May run on the game-loop thread. */
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(() -> {
            this.showPrices(state);
            if (this.tooltips != null) {
                this.tooltips.refreshButtons();
            }
        });
    }

    /** Each tower's price now, red and its tile dimmed while it costs more than the cash. */
    private void showPrices(EconomyState state) {
        for (int i = 0; i < this.buttons.length; i++) {
            int price = this.context.towers().priceOf(this.towerTypes.get(i));
            boolean affordable = state.canAfford(price);
            this.buttons[i].setFace(this.buttons[i].face().withDimmed(!affordable));
            this.prices[i].setText("$" + price);
            this.prices[i].setForeground(affordable ? Hud.FOREGROUND : Hud.SHORT);
        }
    }

    private void untoggleAll() {
        for (HudToggleButton button : this.buttons) {
            button.setSelected(false);
        }
    }

    public void doPlace(int i) {
        this.untoggleAll();
        this.buttons[i].setSelected(true);
        this.mainApp.showInfoSheet(TowerSheetText.shop(this.priced(i)));
        this.startPlacing(this.towerTypes.get(i), this.towerRanges[i]);
    }

    /** The tower at the price the next copy costs now. */
    private TowerInspection priced(int i) {
        return this.shopTowers[i].withPrice(this.context.towers().priceOf(this.towerTypes.get(i)));
    }

    private void initComponents() {

        setLayout(new GridBagLayout());

        setBackground(Hud.BACKGROUND);
        setBorder(Hud.panelBorder("Towers"));
        setForeground(Hud.FOREGROUND);
    }


}
