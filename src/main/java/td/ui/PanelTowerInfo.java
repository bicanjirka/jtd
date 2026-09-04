package td.ui;

import td.tower.Tower;
import td.util.Context;
import td.util.ContextListener;

import javax.swing.*;
import java.awt.*;

public class PanelTowerInfo extends JPanel implements ContextListener {
    private static final long serialVersionUID = 1L;

    private Context context;
    private Tower selectedTower;
    private javax.swing.JButton jButton_sell;
    private JPanel jPanel_buttons;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane jTextPane1;

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
            e.printStackTrace();
        }
    }

    public void unselectTower() {
        if (this.selectedTower != null) {
            this.selectedTower.setSelected(false);
        }
        this.selectedTower = null;
    }

    public void setContext(Context context) {
        this.context = context;
        this.context.addContextListener(this);
    }

    private void sellCurrentTower() {
        if (this.selectedTower != null) {
            this.context.sellTower(this.selectedTower);
            this.unselectTower();
            this.updateInterface();
        }
    }

    public void moneyChanged() {
        this.updateInterface();
    }

    public void livesChanged() {
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jPanel_buttons = new JPanel();
        jButton_sell = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        setBackground(new java.awt.Color(0, 0, 0));
        setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Info", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", Font.PLAIN, 11), new java.awt.Color(220, 255, 220)));
        setForeground(new java.awt.Color(220, 255, 220));
        setMaximumSize(new java.awt.Dimension(200, 2147483647));
        setMinimumSize(new java.awt.Dimension(200, 150));
        setPreferredSize(new java.awt.Dimension(200, 300));
        jScrollPane1.setBackground(new java.awt.Color(0, 0, 0));
        jScrollPane1.setBorder(null);
        jScrollPane1.setForeground(new java.awt.Color(220, 255, 220));
        jTextPane1.setBackground(new java.awt.Color(0, 0, 0));
        jTextPane1.setBorder(null);
        jTextPane1.setForeground(new java.awt.Color(220, 255, 220));
        jScrollPane1.setViewportView(jTextPane1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 0.01;
        add(jScrollPane1, gridBagConstraints);

        jPanel_buttons.setLayout(new java.awt.GridBagLayout());

        jPanel_buttons.setBackground(new java.awt.Color(0, 0, 0));
        jPanel_buttons.setForeground(new java.awt.Color(220, 255, 220));

        jButton_sell.setBackground(new java.awt.Color(0, 0, 0));
        jButton_sell.setText("Sell");
        jButton_sell.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jButton_sell.addActionListener(this::jButton_sellActionPerformed);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.01;
        jPanel_buttons.add(jButton_sell, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(jPanel_buttons, gridBagConstraints);

    }

    private void jButton_sellActionPerformed(java.awt.event.ActionEvent evt) {
        this.sellCurrentTower();
    }

}
