package td.enemy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import td.util.Context;

public class EnemyMobTriangle extends AbstractEnemyMobRotor {

	private int deadTime;
	private boolean gone;
	private Shape bodyShape;
	private float bodyScale;
	
	public EnemyMobTriangle() {
		super();
		this.color = this.colorTrans = Color.YELLOW;
		this.rotPerTime = (float)Math.toRadians(-5.0);
	}
	
	protected void doInit(Context context, int delay, int health, int price, int level) {
		super.doInit(context, delay, health, price, level);
		this.bodyScale = this.context.scale/((this.level<6) ? (7-level) : (2));
		this.bodyShape = this.createTriangle(this.bodyScale, true);
		this.speedMax = (int)(this.speedBase*(1.4+0.1*this.level));
	}
	
	private Shape createTriangle(float scale, boolean up) {
		int u = up ? 1 : -1;
		double point = (Math.sqrt(3)*scale)/2;
		GeneralPath p = new GeneralPath();
	    p.moveTo(0.0f, -scale*u);
	    p.lineTo(-point*u, scale/2*u);
	    p.lineTo(point*u, scale/2*u);
	    p.closePath();
	    return p;
	}
	
	public void doDamage(int damage) {
		super.doDamage(damage);
		this.speed = (this.speedBase + (int)((this.speedMax-this.speedBase)*(1-(this.health/(float)this.healthMax))));
	}
	
	@Override
	public void paint(Graphics2D g2, int gameTime) {
		g2.setColor(this.color);
    	AffineTransform saveXform = g2.getTransform();
    	super.paint(g2, gameTime);
    	g2.transform(this.atTranslate);
    	g2.transform(this.atRotate);
    	
    	if (this.dead) {
    		if (!this.gone) {
	    		if (this.deadTime != 0) {
	    			Color tempColor = g2.getColor();
	    			int i = gameTime - this.deadTime;
	    			if (i>(3*this.level+6)) this.gone = true;
	    			int alpha = (255-(i*(255/((3*this.level+6)+1))));
	    			g2.setColor(new Color(tempColor.getRed(), tempColor.getGreen(), tempColor.getBlue(), ((alpha < 0) ? 0 : alpha)));
	    			i = i*2;
	    			g2.draw(this.createTriangle(this.bodyScale+i, false));
	    			g2.draw(this.createTriangle(this.bodyScale+i, true));
	    		} else {
	    			this.deadTime = gameTime;
	    			this.paint(g2, gameTime);
	    		}
    		}
    	} else if (!this.inactive) {
    		g2.draw(this.bodyShape);
    		g2.setColor(this.colorTrans);
    		g2.fill(this.bodyShape);
    	}
    	g2.setTransform(saveXform);
	}

	@Override
	public String getInfoString() {
		String retString = "";
        retString += 	"Triangle mob\n\n" +
						"Increases speed as it takes damage.";
        return retString;
	}

}
