package td.tower;

import td.tower.Tower;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Iterator;
import java.util.Vector;

import td.util.Context;
import td.util.TowerListener;

public class TowerUpgrade extends AbstractTower implements TowerListener {

	public static int price = 20;
    public static int damage = 0;
    public static float range = 1.5f;
    public static float power = 0.2f;
    
    private Vector<Tower> clients;
	
    public TowerUpgrade(Context context, int x, int y) {
    	super(TowerFactory.type.upgrade, price, damage, range);
    	this.name = "upg";
    	this.lineColor = Color.WHITE;
    	this.passive = true;
    	this.clients = new Vector<Tower>();
    	this.doInit(context, x, y);
    	
    	this.context.addTowerListener(this);
    	this.scanTowers();
    }
    
    private void scanTowers() {
    	Tower t;
    	int dx,dy;
    	for (Iterator<Tower> i=this.context.towers.iterator(); i.hasNext();) {
    		t = i.next();
    		if (!this.clients.contains(t)) {
    			switch (t.getType()) {
    				case upgrade:
    					break;
    				default:
    					dx = this.centerX - t.getX();
    					dy = this.centerY - t.getY();
    					if ((dx*dx+dy*dy) < this.rangeReal2) {
    						t.registerTower(this);
    					}
    					break;
    			}
    		}
    	}
    }
    
    protected void calcDamageRange() {
    	this.scanTowers();
        super.calcDamageRange();
    }
    
    public void doTick(int gameTime) {
    }
    
    public void towerBuild(Tower t) {
    	if (t != this && !this.clients.contains(t)) {
    		switch (t.getType()) {
    			case upgrade:
    				break;
    			default:
    				int dx,dy;
    				dx = this.centerX - t.getX();
					dy = this.centerY - t.getY();
					if ((dx*dx+dy*dy) < this.rangeReal2) {
						t.registerTower(this);
					}
					break;
    		}
    	}
    }
    
    public void towerRemoved(Tower t) {
    	if (this.clients.contains(t)) {
            t.unregisterTower(this);
        }
    }
    
    public void addClient(Tower t) {
        if (!this.clients.contains(t)) {
        	//System.out.println("TowerUpgrade::addClient: adding");
            this.clients.add(t);
        }
    }
    public void removeClient(Tower t) {
        this.clients.remove(t);
    }
    
    public void doCleanup() {
        super.doCleanup();
        for (int i=this.clients.size()-1; i>=0; i--) {
            Tower t = this.clients.get(i);
            t.unregisterTower(this);
        }
        this.context.removeTowerListener(this);
    }
    
    public void paintEffect(Graphics2D g2, int gameTime) {
    }
    
    public String getInfoString() {
		String s = "";
		s +=	"Power tower\n\n" +
				super.getInfoString() +
				"Increases damage and range of nearby towers by " + (TowerUpgrade.power*100) + "%";
				
		return s;
	}
    
    public String getStatusString() {
		String s = "";
		s +=	"Power tower\n\n" +
				super.getStatusString() +
				"Increases damage and range of nearby towers by " + (TowerUpgrade.power*100) + "%\n\n" +
				"Affects towers: " + this.clients.size();
				
		return s;
	}
}
