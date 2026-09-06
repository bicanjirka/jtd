package td.ui;

import td.enemy.EnemyFactory;
import td.util.Context;
import td.wave.Wave;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class PanelWaveInfo extends JPanel {
    private static final long serialVersionUID = 1L;
    private JLabel jLabel_cur_health;
    private JLabel jLabel_cur_level;
    private JLabel jLabel_cur_round;
    private JLabel jLabel_cur_reward;
    private JLabel jLabel_next_health;
    private JLabel jLabel_next_level;
    private JLabel jLabel_next_round;
    private JLabel jLabel_next_reward;
    private PanelEnemy panelEnemy_cur;
    private PanelEnemy panelEnemy_next;

    public PanelWaveInfo() {
        initComponents();
    }

    public void setContext(Context c) {
        this.panelEnemy_cur.setContext(c);
        this.panelEnemy_next.setContext(c);
    }

    public void clearWaves() {
        this.panelEnemy_cur.clearEnemies();
        this.panelEnemy_next.clearEnemies();
        this.jLabel_cur_round.setText("-");
        this.jLabel_cur_health.setText("-hp");
        this.jLabel_cur_reward.setText("-$");
        this.jLabel_cur_level.setText("lvl-");
        this.jLabel_next_round.setText("-");
        this.jLabel_next_health.setText("-hp");
        this.jLabel_next_reward.setText("-$");
        this.jLabel_next_level.setText("lvl-");
    }

    public void setWaveCur(int round, Wave w) {
        this.panelEnemy_cur.clearEnemies();
        this.jLabel_cur_round.setText("" + round);
        this.jLabel_cur_health.setText(w.getBaseHealth() + "hp");
        this.jLabel_cur_reward.setText(w.getBasePrice() + "$");
        this.jLabel_cur_level.setText("lvl " + w.getLevel());
        for (EnemyFactory.Enemy e : w.enemySet()) {
            this.panelEnemy_cur.addEnemy(e, w.enemyCount(e), w.getLevel());
        }
        this.panelEnemy_cur.recalculateSize();
        this.invalidate();
        this.panelEnemy_cur.invalidate();
        this.validate();
    }

    public void setWaveNext(int round, Wave w) {
        this.panelEnemy_next.clearEnemies();
        this.jLabel_next_round.setText("" + round);
        this.jLabel_next_health.setText(w.getBaseHealth() + "hp");
        this.jLabel_next_reward.setText(w.getBasePrice() + "$");
        this.jLabel_next_level.setText("lvl " + w.getLevel());
        for (EnemyFactory.Enemy e : w.enemySet()) {
            this.panelEnemy_next.addEnemy(e, w.enemyCount(e), w.getLevel());
        }
        this.panelEnemy_next.recalculateSize();
        this.invalidate();
        this.panelEnemy_next.invalidate();
        this.validate();
    }

    public void doTick(int gameTime) {
        this.panelEnemy_cur.doTick(gameTime);
        this.panelEnemy_next.doTick(gameTime);
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        jLabel_cur_health = new JLabel();
        jLabel_cur_reward = new JLabel();
        jLabel_next_health = new JLabel();
        jLabel_next_reward = new JLabel();
        jLabel_next_level = new JLabel();
        jLabel_cur_level = new JLabel();
        jLabel_next_round = new JLabel();
        jLabel_cur_round = new JLabel();
        panelEnemy_cur = new PanelEnemy();
        panelEnemy_next = new PanelEnemy();

        setLayout(new GridBagLayout());

        setBackground(new Color(0, 0, 0));
        setBorder(BorderFactory.createTitledBorder(null, "Current & Next Wave", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.PLAIN, 11), new Color(220, 255, 220)));
        setForeground(new Color(220, 255, 220));
        setMaximumSize(new Dimension(200, 32767));
        setMinimumSize(new Dimension(200, 60));
        setPreferredSize(new Dimension(200, 60));
        jLabel_cur_health.setForeground(new Color(220, 255, 220));
        jLabel_cur_health.setText("0hp");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 5);
        add(jLabel_cur_health, gridBagConstraints);

        jLabel_cur_reward.setForeground(new Color(220, 255, 220));
        jLabel_cur_reward.setText("$0");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        add(jLabel_cur_reward, gridBagConstraints);

        panelEnemy_cur.setMinimumSize(new Dimension(30, 30));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new Insets(2, 0, 2, 0);
        add(panelEnemy_cur, gridBagConstraints);

        jLabel_next_health.setForeground(new Color(220, 255, 220));
        jLabel_next_health.setText("0hp");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 5);
        add(jLabel_next_health, gridBagConstraints);

        jLabel_next_reward.setForeground(new Color(220, 255, 220));
        jLabel_next_reward.setText("$0");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        add(jLabel_next_reward, gridBagConstraints);

        panelEnemy_next.setMinimumSize(new Dimension(30, 30));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new Insets(2, 0, 2, 0);
        add(panelEnemy_next, gridBagConstraints);

        jLabel_next_round.setBackground(new Color(0, 0, 0));
        jLabel_next_round.setForeground(new Color(220, 255, 220));
        jLabel_next_round.setText("0");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(0, 0, 0, 5);
        add(jLabel_next_round, gridBagConstraints);

        jLabel_cur_round.setBackground(new Color(0, 0, 0));
        jLabel_cur_round.setForeground(new Color(220, 255, 220));
        jLabel_cur_round.setText("0");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(0, 0, 0, 5);
        add(jLabel_cur_round, gridBagConstraints);

        jLabel_next_level.setBackground(new Color(0, 0, 0));
        jLabel_next_level.setForeground(new Color(220, 255, 220));
        jLabel_next_level.setText("0");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(0, 0, 0, 5);
        add(jLabel_next_level, gridBagConstraints);

        jLabel_cur_level.setBackground(new Color(0, 0, 0));
        jLabel_cur_level.setForeground(new Color(220, 255, 220));
        jLabel_cur_level.setText("0");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(0, 0, 0, 5);
        add(jLabel_cur_level, gridBagConstraints);

    }

}
