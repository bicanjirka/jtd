package td.enemy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import td.util.Context;

/**
 * Silny nepritel znazornen kostickou, dostava polovicni zraneni
 * @author Jirka
 *
 */
public class EnemyMobSquare extends AbstractEnemyMobRotor{
	
	private int deadTime;
	private boolean gone;
	private Shape bodyShape;
	private float bodyScale;
	private float K;

	public EnemyMobSquare() {
		super();
		this.color = this.colorTrans = Color.PINK;
	}
	
	protected void doInit(Context context, int delay, int health, int price, int level) {
		super.doInit(context, delay, health, price, level);
		this.bodyScale = this.context.scale/((this.level<6) ? (7-level) : (2));
		this.bodyShape = new Rectangle2D.Float(-this.bodyScale,-this.bodyScale,this.bodyScale*2,this.bodyScale*2);
		K = 0.8f - this.level * 0.05f;
		//System.out.println(""+K);
	}
	
	public void doDamage(int damage) {
		super.doDamage((int)(damage*K));
	}
	
    public String getInfoString() {
        return """
                Square mob

                Takes less damage.""";
    }
    
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
	    			g2.draw(new Rectangle2D.Float(-(this.bodyScale+i),-this.bodyScale,(this.bodyScale+i)*2,this.bodyScale*2));
	    			g2.draw(new Rectangle2D.Float(-this.bodyScale,-(this.bodyScale+i),this.bodyScale*2,(this.bodyScale+i)*2));
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
}
