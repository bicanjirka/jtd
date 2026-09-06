package td.ui;

import td.TowerDefence;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.Cache;
import td.util.Context;
import td.util.ContextListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PanelTowerSelector extends JPanel implements ContextListener {
    private static final long serialVersionUID = 1L;

    private final BufferedImage[] images;
    private final JToggleButton[] buttons;
    private final String[] infoText;
    private final float[] towerRanges;
    private Context context;
    private TowerDefence mainApp;
    private final List<TowerFactory.type> towerTypes;
    private boolean placing = false;

    public PanelTowerSelector() {
        initComponents();
        this.towerTypes = new ArrayList<>();

        TowerFactory.type[] types = TowerFactory.type.values();
        for (TowerFactory.type type : types) {
            this.towerTypes.add(TowerFactory.type.valueOf(type.toString()));
        }

        this.images = new BufferedImage[this.towerTypes.size()];
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

    public void doInit(Context c, TowerDefence mainApp) {
        this.context = c;
        this.mainApp = mainApp;

        this.makeButtons();

        Context tempContext = new Context(null);
        Cache cache = this.context.getCache();

        for (int i = 0; i < this.towerTypes.size(); i++) {
            Tower tempTower = TowerFactory.createTower(this.towerTypes.get(i), tempContext, 0, 0);
            if (cache.hasBufImg(tempTower.getName() + "_ico")) {
                this.images[i] = cache.getBufImg(tempTower.getName() + "_ico");
            }
            this.infoText[i] = tempTower.getInfoString();
            if (this.images[i] != null)
                this.buttons[i].setIcon(new ImageIcon(this.images[i]));
            this.towerRanges[i] = tempTower.getRange();
        }

        this.context.addContextListener(this);
    }

    public void startPlacing(TowerFactory.type t, float r) {
        this.placing = true;
        this.mainApp.startPlacing(t, r);
    }

    public void stopPlacing() {
        this.untoggleAll();
        this.placing = false;
    }

    public void moneyChanged() {
        for (int i = 0; i < this.buttons.length; i++) {
            this.buttons[i].setEnabled(this.context.canPay(this.towerTypes.get(i).price));
        }
    }

    public void livesChanged() {
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