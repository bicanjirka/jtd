package td.ui;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import td.TowerDefence;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.Cache;
import td.util.Context;
import td.util.ContextListener;
/**
 * Panel s tlacitky na stavbu vsech vezi
 * @author Juras
 *
 */
@SuppressWarnings("serial")
public class PanelTowerSelector extends JPanel implements ContextListener {

	private BufferedImage[] images;
    private JToggleButton[] buttons;
    private String[] infoText;
    private float[] towerRanges;
    private Context context;
    private TowerDefence mainApp;
    private Vector<TowerFactory.type> towerTypes;
    private boolean placing = false;
    
    /**Vytvori panel s vyberem vezi, ktere zinicializuje
     * a vytvori schranku pro vnejsi ulozeni jejich potrebnych
     * informaci
     */
    public PanelTowerSelector() {
        initComponents();
        this.towerTypes = new Vector<TowerFactory.type>();
        
        TowerFactory.type[] types = TowerFactory.type.values();
        for (int i = 0; i < types.length; i++) {
        	this.towerTypes.add(TowerFactory.type.valueOf(types[i].toString()));
        }
        /*this.towerTypes.add(TowerFactory.type.first);
        this.towerTypes.add(TowerFactory.type.second);
        this.towerTypes.add(TowerFactory.type.third);
        this.towerTypes.add(TowerFactory.type.fourth);
        this.towerTypes.add(TowerFactory.type.upgrade);*/
        
        this.images         = new BufferedImage[this.towerTypes.size()];//TowerFactory.type.values().length;
        this.buttons        = new JToggleButton[this.towerTypes.size()];
        this.infoText       = new String[this.towerTypes.size()];
        this.towerRanges    = new float[this.towerTypes.size()];
    }
    /**
     * Vytvori tlacitka a prida jim listenery
     */
    private void makeButtons() {
        GridBagConstraints gridBagConstraints;
        JToggleButton tempToggle;
        for (int i=0; i<this.towerTypes.size(); i++) {
            tempToggle = new JToggleButton();
            this.buttons[i] = tempToggle;
            tempToggle.setMargin(new Insets(1, 1, 1, 1));
            final int n = i;
            tempToggle.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    doPlace(n);
                }
            });
            tempToggle.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    mouseOver(n);
                }
            });
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = n;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.insets = new Insets(-9,0,-2,3);
            add(tempToggle, gridBagConstraints);
        }
    }
    /**
     * Vytvori vsechny veze a vytahne z nich veskere potrebne informace<b>
     * Ulozi ukazatel na hlavni aplikaci a herni kontext, do ktereho
     * se prida jako listener
     * @param c - kontext
     * @param mainApp - hlavni aplikace
     */
    public void doInit(Context c, TowerDefence mainApp) {
        this.context = c;
        this.mainApp = mainApp;
        
        this.makeButtons();
        
        Context tempContext = new Context(null);
        Cache cache = this.context.getCache();
        
        for (int i=0; i<this.towerTypes.size(); i++) {
        	Tower tempTower = TowerFactory.createTower(this.towerTypes.get(i), tempContext, 0, 0);
            if(cache.hasBufImg(tempTower.getName()+"_ico")) {
            	this.images[i] = cache.getBufImg(tempTower.getName()+"_ico");
            }
            this.infoText[i] = tempTower.getInfoString();
            if(this.images[i] != null)
            	this.buttons[i].setIcon(new ImageIcon(this.images[i]));
            this.towerRanges[i] = tempTower.getRange();
        }
        
        this.context.addContextListener(this);
    }
    /**
     * Zaznamena ze pokladame vez a preposle pozadavek na hlavni aplikaci
     * @param t - vez
     * @param r - dostrel
     */
    public void startPlacing(TowerFactory.type t, float r) {
        this.placing = true;
        this.mainApp.startPlacing(t, r);
    }
    /**
     * Prestavame pokladat vez
     */
    public void stopPlacing() {
        this.untoggleAll();
        this.placing = false;
    }
    
    public void moneyChanged() {
        for (int i=0; i<this.buttons.length; i++) {
            this.buttons[i].setEnabled(this.context.canPay(this.towerTypes.get(i).price));
        }
    }
    
    public void livesChanged() {
    }
    /**
     * Vsem tlacitkum s vezemi nastavi, ze nejsou zmacknuta
     */
    private void untoggleAll() {
        for (int i=0; i<this.buttons.length; i++) {
            this.buttons[i].setSelected(false);
        }
    }
    /**
     * Zacne pokladat vez urcenou cislem<br>
     * Zvyrazni tlacitko s vezi a nastavi info panelu text veze
     * @param i - cislo veze
     */
    public void doPlace(int i) {
        //this.mouseOver(i);
        this.untoggleAll();
        this.buttons[i].setSelected(true);
        this.mainApp.setInfoText(this.infoText[i]);
        this.startPlacing(this.towerTypes.get(i), this.towerRanges[i]);
    }
    /**
     * Nastavi info panelu text veze urcene cislem
     * @param i - cislo veze
     */
    private void mouseOver(int i) {
    	if (!this.placing) {
            this.mainApp.setInfoText(this.infoText[i]);
        }
    }
    
    /**
     * Inicializace vsech komponent interface<br>
     * Volano z konstruktoru
     */
    private void initComponents() {
        
    	setLayout(new java.awt.GridBagLayout());

        setBackground(new java.awt.Color(0, 0, 0));
        setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Towers", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 11), new java.awt.Color(220, 255, 220)));
        setForeground(new java.awt.Color(220, 255, 220));
    }
    
    
}