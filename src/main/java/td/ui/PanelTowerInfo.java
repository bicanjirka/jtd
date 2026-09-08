package td.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.util.GameWorld;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.Serial;

public class PanelTowerInfo extends JPanel implements EconomyListener {

    private static final Logger LOG = LoggerFactory.getLogger(PanelTowerInfo.class);

    @Serial
    private static final long serialVersionUID = 1L;

    private GameWorld context;
    private Tower selectedTower;
    private JButton jButton_sell;
    private JPanel jPanel_buttons;
    private JScrollPane jScrollPane1;
    private JTextPane jTextPane1;

    public PanelTowerInfo() {
        initComponents();
    }

    public void setTower(Tower t) {
        if (this.selectedTower != null) {
            this.selectedTower.setSelected(false);
        }
        this.selectedTower = t;
        this.selectedTower.setSelected(true);
        this.updateInterface();
    }

    public void setExternalText(String s) {
        this.jPanel_buttons.setVisible(false);
        this.setText(s);
    }

    private void updateInterface() {
        this.jPanel_buttons.setVisible(false);
        this.jButton_sell.setVisible(true);

        if (this.selectedTower != null) {
            this.jPanel_buttons.setVisible(true);
            this.jButton_sell.setText("Sell ( $" + this.selectedTower.getSellPrice() + " )");
            this.setText(this.selectedTower.getStatusString());
        }
    }

    private void setText(String s) {
        try {
            this.jTextPane1.setText(s);
        } catch (NullPointerException e) {
            LOG.warn("Could not set tower info text", e);
        }
    }

    public void unselectTower() {
        if (this.selectedTower != null) {
            this.selectedTower.setSelected(false);
        }
        this.selectedTower = null;
    }

    public void setGameWorld(GameWorld context) {
        this.context = context;
        this.context.addEconomyListener(this);
    }

    private void sellCurrentTower() {
        if (this.selectedTower != null) {
            this.context.sellTower(this.selectedTower);
            this.unselectTower();
            this.updateInterface();
        }
    }

    /** Also reachable from the game-loop thread - see GameWorld.apply()'s callers. */
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(this::updateInterface);
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        jScrollPane1 = new JScrollPane();
        jTextPane1 = new JTextPane();
        jPanel_buttons = new JPanel();
        jButton_sell = new JButton();

        setLayout(new GridBagLayout());

        setBackground(new Color(0, 0, 0));
        setBorder(BorderFactory.createTitledBorder(null, "Info", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.PLAIN, 11), new Color(220, 255, 220)));
        setForeground(new Color(220, 255, 220));
        setMaximumSize(new Dimension(200, 2147483647));
        setMinimumSize(new Dimension(200, 150));
        setPreferredSize(new Dimension(200, 300));
        jScrollPane1.setBackground(new Color(0, 0, 0));
        jScrollPane1.setBorder(null);
        jScrollPane1.setForeground(new Color(220, 255, 220));
        jTextPane1.setBackground(new Color(0, 0, 0));
        jTextPane1.setBorder(null);
        jTextPane1.setForeground(new Color(220, 255, 220));
        jScrollPane1.setViewportView(jTextPane1);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 0.01;
        add(jScrollPane1, gridBagConstraints);

        jPanel_buttons.setLayout(new GridBagLayout());

        jPanel_buttons.setBackground(new Color(0, 0, 0));
        jPanel_buttons.setForeground(new Color(220, 255, 220));

        jButton_sell.setBackground(new Color(0, 0, 0));
        jButton_sell.setText("Sell");
        jButton_sell.setMargin(new Insets(2, 2, 2, 2));
        jButton_sell.addActionListener(this::jButton_sellActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.01;
        jPanel_buttons.add(jButton_sell, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(jPanel_buttons, gridBagConstraints);

    }

    private void jButton_sellActionPerformed(ActionEvent evt) {
        this.sellCurrentTower();
    }

}
