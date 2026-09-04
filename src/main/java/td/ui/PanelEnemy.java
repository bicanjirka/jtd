package td.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JPanel;

import td.enemy.EnemyFactory;
import td.enemy.EnemyFactory.Enemy;
import td.enemy.EnemyMob;
import td.util.Context;
import td.wave.Path;
import td.wave.PathEmpty;

@SuppressWarnings("serial")
public class PanelEnemy extends JPanel {
	
	private Vector<EnemyMob> enemies = new Vector<EnemyMob>();
	private int[] enemiesCount;
	private Font font;
	private Context contextLocal, contextFull;
	private int pHeight = 0;
    private int pWidth = 0;
    private int scale = 32;
    private int gameTime = 0;
    
    public PanelEnemy() {
        initComponents();
        this.contextLocal = new Context(null);
        this.contextLocal.setPath(new PathEmpty());
    }
    
    public void setContext(Context c) {
        this.contextFull = c;
    }
    
    public void clearEnemies() {
        this.enemies.clear();
        this.enemiesCount = new int[Enemy.values().length];
        this.contextLocal.setPath(new PathEmpty());
    }
    
    public void addEnemy(Enemy e, int count, int level) {
    	if (e.equals(Enemy.Empty)) return;
    	int nr = this.enemies.size();
        Path path = this.contextLocal.getPath();
        path.addStep(this.scale/2 + this.scale*nr, this.pHeight/2);
        EnemyMob enemy = e.getCopy(this.contextLocal, 0, 0, 0, level);
        enemy.doTick(0);
        this.enemies.add(enemy);
        this.enemiesCount[nr] = count;
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(195,40);
    }
    
    public void recalculateSize() {
        this.pWidth = this.getWidth();
        //this.contextLocal.getCache().clearCache();
        if (this.enemies.size() > 0) {
            this.pHeight = Math.min(this.pWidth / this.enemies.size(), this.getHeight());
            this.scale = this.pHeight;
            this.contextLocal.scale = this.scale;
//            Path path = this.contextLocal.getPath();
//            path.addStep(this.scale/2, this.pHeight/2);
        } else {
        	this.pHeight = this.getHeight();
        	this.scale = this.pHeight;
            this.contextLocal.scale = this.scale;
        }
        this.font = new Font(Font.DIALOG, Font.PLAIN, (int)(0.30*this.scale));
    }
    
    public void doTick(int gameTime) {
        this.gameTime = gameTime;
        /*for (Iterator<EnemyMob> i = this.enemies.iterator(); i.hasNext();) {
            EnemyMob e = i.next();
            e.doTick(gameTime);
        }*/
        this.repaint();
    }
    
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        g2.fillRect(0,0,this.pWidth, this.pHeight);
        int nr = 0;
        
        for (Iterator<EnemyMob> i = this.enemies.iterator(); i.hasNext();) {
            EnemyMob e = i.next();
            e.paint(g2, this.gameTime);
            g2.setColor(Color.PINK);
            g2.setFont(this.font);
            g2.drawString(""+this.enemiesCount[nr], this.scale*nr, this.pHeight);
            nr++;
        }
        
    }
    
    private void mouseOver(int x) {
        int nr = (int)(x/this.scale);
        //System.out.println("PanelEnemy:: mouse: "+nr);
        if (nr < this.enemies.size()) {
            EnemyMob e = this.enemies.get(nr);
            this.contextFull.setInfoText(e.getInfoString());
        }
    }
    
    private void initComponents() {

        setLayout(null);

        setBackground(new Color(0, 0, 0));
        setForeground(new Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                formMouseMoved(evt);
            }
        });

    }
    
    private void formMouseMoved(java.awt.event.MouseEvent evt) {
        this.mouseOver(evt.getX());
    }
    
    private void formComponentResized(java.awt.event.ComponentEvent evt) {
        this.recalculateSize();
    }
    
}
