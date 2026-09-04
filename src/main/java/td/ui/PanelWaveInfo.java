package td.ui;

import javax.swing.JPanel;

import td.enemy.EnemyFactory;
import td.util.Context;
import td.wave.Wave;

/**
 * Panel obsahujici informace o soucasne a nasledujici vlne
 * @author Juras
 *
 */
@SuppressWarnings("serial")
public class PanelWaveInfo extends JPanel {
	/**
	 * Konstruktor, vyvola inicializaci komponent
	 */
	public PanelWaveInfo() {
		initComponents();
	}
	
	public void setContext(Context c) {
        this.panelEnemy_cur.setContext(c);
        this.panelEnemy_next.setContext(c);
    }
	/**
	 * Vynuluje informace o vlnach
	 */
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
	/**
	 * Nastavi informace o soucasne vlne
	 * @param w - soucasna vlna
	 */
	public void setWaveCur(int round, Wave w) {
		this.panelEnemy_cur.clearEnemies();
		this.jLabel_cur_round.setText(""+round);
        this.jLabel_cur_health.setText(""+w.getBaseHealth()+"hp");
        this.jLabel_cur_reward.setText(""+w.getBasePrice()+"$");
        this.jLabel_cur_level.setText("lvl "+w.getLevel());
        for (EnemyFactory.Enemy e : w.enemySet()) {
            this.panelEnemy_cur.addEnemy(e, w.enemyCount(e), w.getLevel());
        }
        this.panelEnemy_cur.recalculateSize();
        this.invalidate();
        this.panelEnemy_cur.invalidate();
        this.validate();
    }
	/**
	 * Nastavi informace o pristi vlne
	 * @param w - pristi vlna
	 */
    public void setWaveNext(int round, Wave w) {
    	this.panelEnemy_next.clearEnemies();
        this.jLabel_next_round.setText(""+round);
        this.jLabel_next_health.setText(""+w.getBaseHealth()+"hp");
        this.jLabel_next_reward.setText(""+w.getBasePrice()+"$");
        this.jLabel_next_level.setText("lvl "+w.getLevel());
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
    /**
     * Inicializuje veskere komponenty interface<br>
     * Volano z konstruktoru
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel_cur_health = new javax.swing.JLabel();
        jLabel_cur_reward = new javax.swing.JLabel();
        jLabel_next_health = new javax.swing.JLabel();
        jLabel_next_reward = new javax.swing.JLabel();
        jLabel_next_level = new javax.swing.JLabel();
        jLabel_cur_level = new javax.swing.JLabel();
        jLabel_next_round = new javax.swing.JLabel();
        jLabel_cur_round = new javax.swing.JLabel();
        panelEnemy_cur = new PanelEnemy();
        panelEnemy_next = new PanelEnemy();

        setLayout(new java.awt.GridBagLayout());

        setBackground(new java.awt.Color(0, 0, 0));
        setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Current & Next Wave", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 11), new java.awt.Color(220, 255, 220)));
        setForeground(new java.awt.Color(220, 255, 220));
        setMaximumSize(new java.awt.Dimension(200, 32767));
        setMinimumSize(new java.awt.Dimension(200, 60));
        setPreferredSize(new java.awt.Dimension(200, 60));
        jLabel_cur_health.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_cur_health.setText("0hp");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        add(jLabel_cur_health, gridBagConstraints);

        jLabel_cur_reward.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_cur_reward.setText("$0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        add(jLabel_cur_reward, gridBagConstraints);
        
        panelEnemy_cur.setMinimumSize(new java.awt.Dimension(30, 30));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(panelEnemy_cur, gridBagConstraints);
        
        jLabel_next_health.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_next_health.setText("0hp");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        add(jLabel_next_health, gridBagConstraints);

        jLabel_next_reward.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_next_reward.setText("$0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        add(jLabel_next_reward, gridBagConstraints);
        
        panelEnemy_next.setMinimumSize(new java.awt.Dimension(30, 30));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(panelEnemy_next, gridBagConstraints);
        
        jLabel_next_round.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_next_round.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_next_round.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        add(jLabel_next_round, gridBagConstraints);

        jLabel_cur_round.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_cur_round.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_cur_round.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        add(jLabel_cur_round, gridBagConstraints);
        
        jLabel_next_level.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_next_level.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_next_level.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        add(jLabel_next_level, gridBagConstraints);

        jLabel_cur_level.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_cur_level.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_cur_level.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        add(jLabel_cur_level, gridBagConstraints);

    }
    
    
    private javax.swing.JLabel jLabel_cur_health;
    private javax.swing.JLabel jLabel_cur_level;
    private javax.swing.JLabel jLabel_cur_round;
    private javax.swing.JLabel jLabel_cur_reward;
    private javax.swing.JLabel jLabel_next_health;
    private javax.swing.JLabel jLabel_next_level;
    private javax.swing.JLabel jLabel_next_round;
    private javax.swing.JLabel jLabel_next_reward;
    private PanelEnemy panelEnemy_cur;
    private PanelEnemy panelEnemy_next;
    
}
