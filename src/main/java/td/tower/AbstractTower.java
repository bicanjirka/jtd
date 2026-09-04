
package td.tower;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import td.util.Cache;
import td.util.Context;

/**
 * Abstraktni trida pro vsechny veze
 * @author Jirka
 *
 */
public abstract class AbstractTower implements Tower {	//TODO rotovaci obrazky??? :)
	
	protected Context context;	
	private TowerFactory.type type;
	protected String name;
	protected List<TowerUpgrade> upgTowers;
	
	protected int boardX;
    protected int boardY;
    protected int centerX;
    protected int centerY;
	
	protected float	rangeBase = 0;
	private float rangeCurrent = 0;
	protected int damageBase = 0;
	protected int damageCurrent = 0;
	protected int coolDownMax;
	
	protected float rangeReal = 0;
	protected float rangeReal2 = 0;
	
	protected Color lineColor;
	protected Shape rangeCircle;
	protected BufferedImage img;
	
	protected boolean passive = false;
	protected boolean selected = false;
	private int price = 0;
	
	
	public AbstractTower(TowerFactory.type t, int price, int damage, float range) {
		//System.out.println("Creating "+this.name);
		this.price = price;
        this.type = t;
        this.damageBase = this.damageCurrent = damage;
        this.rangeBase = this.rangeCurrent = range;
        this.upgTowers = new ArrayList<TowerUpgrade>();
	}
	/**
	 * Inicializuje vez
	 * @param context - herni kontext
	 * @param x - souradnice X
	 * @param y - souradnice Y
	 */
	protected void doInit(Context context, int x, int y) {
		this.context = context;
		Cache cache = this.context.getCache();
		if (cache.hasBufImg(this.name))
			this.img = cache.getBufImg(this.name);
		int scale = this.context.scale;
		this.boardX = x*scale;
		this.boardY = y*scale;
		this.centerX = this.boardX + scale/2;
		this.centerY = this.boardY + scale/2;
		this.rangeReal = this.rangeBase * scale;
		this.rangeReal2 = rangeReal * rangeReal;
		this.rangeCircle = new Ellipse2D.Float(this.centerX - this.rangeReal, this.centerY - this.rangeReal, rangeReal*2, rangeReal*2);
	}
	
	public float getRange() {
		return this.rangeBase;
	}
	
	public float getRangeReal() {
		return this.rangeReal;
	}
	
	public int getSellPrice() {
		return (int) Math.round(0.75 * this.price);
	}
	
	protected void calcDamageRange() {
		//System.out.println("AbstractTower::calcDmgRng: calling");
		float upg = (1f + TowerUpgrade.power * this.upgTowers.size());
		this.damageCurrent = (int) (this.damageBase * upg);
		this.rangeCurrent = this.rangeBase * upg;
		
		this.rangeReal = this.rangeCurrent * this.context.scale;
		this.rangeReal2 = rangeReal * rangeReal;
		this.rangeCircle = new Ellipse2D.Float(this.centerX - this.rangeReal, this.centerY - this.rangeReal, rangeReal*2, rangeReal*2);
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public void paint(Graphics2D g2, int gameTime) {
		if (this.selected) {
			g2.setColor(Color.PINK);
			g2.draw(this.rangeCircle);
		}
		g2.drawImage(img, null, boardX, boardY);
	}
	
	public int getX() {
		//return boardX;
		return centerX;
	}
	
	public int getY() {
		//return boardY;
		return centerY;
	}
	
	public TowerFactory.type getType() {
		return this.type;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getInfoString() {
		String s = "";
		s += 	"Price: " + this.price + "\n" +
				"Range: " + this.rangeBase + "\n";
				if (this.passive) {
					s += 	"\n";
				} else {
					s += 	"Damage: " + this.damageBase/100f + "\n" +
							"Fire rate: " + 20f/(this.coolDownMax+1) + "/s\n\n";
				}
				
		return s;
	}
	
	public String getStatusString() {
		String s = "";
		s += 	"Range: " + this.rangeCurrent + "\n";
				if (this.passive) {
					s += 	"\n";
				} else {
					s += 	"Damage: " + this.damageCurrent/100f + "\n" +
							"Fire rate: " + 20f/(this.coolDownMax+1) + "/s\n\n";
				}
				
		return s;
	}
	
	public void registerTower(Tower t) {
		if (t != this) {
			switch (t.getType()) {
				case upgrade -> {
					if (this.type != TowerFactory.type.upgrade && !this.upgTowers.contains(t)) {
						TowerUpgrade tupg = (TowerUpgrade) t;
						this.upgTowers.add(tupg);
						tupg.addClient(this);
						this.calcDamageRange();
					}
				}
				default -> {}
			}
		}
	}

	public void unregisterTower(Tower t) {
		switch (t.getType()) {
			case upgrade -> {
				TowerUpgrade tupg = (TowerUpgrade) t;
				this.upgTowers.remove(t);
				tupg.removeClient(this);
				this.calcDamageRange();
			}
			default -> {}
		}
	}
	
	public void doCleanup() {
		for (int i=this.upgTowers.size()-1; i>=0; i--) {
            TowerUpgrade tupg = this.upgTowers.remove(i);
            tupg.removeClient(this);
        }
	}
}
