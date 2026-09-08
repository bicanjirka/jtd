package td.ui;

import td.TowerDefense;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.Context;
import td.util.GameHost;

import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Font;
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

public class PanelTowerSelector extends JPanel implements EconomyListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final int ICON_SIZE = 32;

    private final JToggleButton[] buttons;
    private final String[] infoText;
    private final float[] towerRanges;
    private Context context;
    private TowerDefense mainApp;
    private final List<TowerFactory.type> towerTypes;
    private boolean placing = false;

    public PanelTowerSelector() {
        initComponents();
        this.towerTypes = new ArrayList<>();

        TowerFactory.type[] types = TowerFactory.type.values();
        for (TowerFactory.type type : types) {
            this.towerTypes.add(TowerFactory.type.valueOf(type.toString()));
        }

        this.buttons = new JToggleButton[this.towerTypes.size()];
        this.infoText = new String[this.towerTypes.size()];
        this.towerRanges = new float[this.towerTypes.size()];
    }

    private void makeButtons() {
        GridBagConstraints gridBagConstraints;
        JToggleButton tempToggle;
        for (int i = 0; i < this.towerTypes.size(); i++) {
            tempToggle = new JToggleButton();
            this.buttons[i] = tempToggle;
            tempToggle.setMargin(new Insets(1, 1, 1, 1));
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
            gridBagConstraints.insets = new Insets(-9, 0, -2, 3);
            add(tempToggle, gridBagConstraints);
        }
    }

    public void doInit(Context c, TowerDefense mainApp) {
        this.context = c;
        this.mainApp = mainApp;

        this.makeButtons();

        Context tempContext = new Context(GameHost.noOp());
        Java2DFrameRenderer iconRenderer = new Java2DFrameRenderer();

        for (int i = 0; i < this.towerTypes.size(); i++) {
            TowerFactory.type type = this.towerTypes.get(i);
            Tower tempTower = TowerFactory.createTower(type, tempContext, 0, 0);
            this.infoText[i] = tempTower.getInfoString();
            this.towerRanges[i] = tempTower.getRange();

            BufferedImage icon = iconRenderer.renderTowerIcon(TowerSpriteFrameBuilder.bodyPaletteFor(type), ICON_SIZE);
            this.buttons[i].setIcon(new SharpImageIcon(icon));
            this.buttons[i].setDisabledIcon(new SharpImageIcon(grayedOut(icon)));
        }

        this.context.addEconomyListener(this);
    }

    public void startPlacing(TowerFactory.type t, float r) {
        this.placing = true;
        this.mainApp.startPlacing(t, r);
    }

    public void stopPlacing() {
        this.untoggleAll();
        this.placing = false;
    }

    /** Also reachable from the game-loop thread - see Context.apply()'s callers. */
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < this.buttons.length; i++) {
                this.buttons[i].setEnabled(state.canAfford(this.towerTypes.get(i).price));
            }
        });
    }

    private void untoggleAll() {
        for (JToggleButton button : this.buttons) {
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

        setBackground(new Color(0, 0, 0));
        setBorder(BorderFactory.createTitledBorder(null, "Towers", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.PLAIN, 11), new Color(220, 255, 220)));
        setForeground(new Color(220, 255, 220));
    }


}