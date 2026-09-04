package td.enemy;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import td.util.Context;
import td.wave.Path;

/**
 * Abstraktni trida nepratel
 * @author Jirka
 *
 */
public abstract class AbstractEnemyMob implements EnemyMob, Cloneable {
	
	protected type type;
	protected boolean inactive = true;
    protected boolean validTarget = false;
    protected boolean dead = false;
//    protected boolean visible = true;
    protected int price;
    protected int level;
    protected Color colorTrans = Color.WHITE;
    protected Color color = Color.WHITE;
    private int delay;
    protected int x,y;
    protected AffineTransform atTranslate;
    
    private Path path;
    private int segment = 0;
    private int segmentProgression = 0;//TODO float 0..1, automaticke pocitani vzdalenosti, aby se dalo chodit i sikmo - napadl me problem ze by se pak nedalo po takovym segmentProgression moc speedovat
    private int[] segmentStartPoint;
    private int[] segmentEndPoint;
    
    //protected int acceleration = 1;
    protected int speed = 40;
    protected int speedMax = 40;
    protected int speedBase = 40;
    //protected int speedRepairTime = 0;
    
    protected int health;
    protected int healthMax;
    protected int alpha = 255;
    
    protected Context context;
    
    
    public AbstractEnemyMob() {
    	this.type = EnemyMob.type.Normal;
    }    
    /**
     * Inicializuje nepritele
     * @param context - herni kontext
     * @param delay - zpozdeni
     * @param health - zivoty
     * @param price - cena
     */
    protected void doInit(Context context, int delay, int health, int price, int level) {
        this.context = context;
        this.price = price;
        this.level = level;
        this.health = health*100;
        this.healthMax = health*100;
        this.path = this.context.getPath();
        this.x = 0;
        this.y = 0;
        this.delay = Math.round(700f * delay / this.speed);	//TODO 700f nahradit ukazatelem context.delay ktery by se menil pri zadani zmeny pred zadavani mobu, treba 700 w
        if (delay == 0) {
            this.inactive = false;
            this.validTarget = true;
        }
        this.atTranslate = new AffineTransform();
        this.resetPosition();
    }

    public long getHealth() {
        return this.health;
    }
    
    public void doDamage(int damage) {
    	//System.out.println("EnemyMobBase::doDamage: "+this.getInfoString()+" getting "+damage/100f+" damage...");
    	//this.context.dmg += damage;		//TEST damage
    	if (this.validTarget()) {
            this.health -= damage;
        }
        if (this.health <= 0) {
            this.validTarget = false;
            this.dead = true;
            this.alpha = 0;
            this.colorTrans = new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), alpha);
            this.context.addScore(this.price);
            this.context.doReceive(this.price);
            this.context.removeEnemy();
        } else {
        	this.alpha = (int) (((float)this.health / this.healthMax)*255);
            this.colorTrans = new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), alpha);
        }
    }
    
    public int getX() {
        return this.x;
    }
    
    public int getY() {
        return this.y;
    }
    
    public int getSpeed() {
        return this.speed;
    }
    
    public int getProgression() {
    	int i = 1000*this.segment + this.segmentProgression;
    	return i;
    }
    
    public boolean validTarget() {
        return ( (!this.inactive) && this.validTarget && (!this.dead));
    }
    
    public boolean validTarget(type type) {
    	return ( this.validTarget() && (type.equals(this.type)) );
    }
    
    public boolean validTarget(type type1, type type2) {
    	return ( this.validTarget(type1) || this.validTarget(type2));
    }
    /**
     * Resetne pozici nepritele na pocatek cesty
     */
    private void resetPosition() {
        this.segmentStartPoint = this.path.getStep(this.segment);
        this.segmentEndPoint = this.path.getStep(this.segment+1);
        this.x = this.segmentStartPoint[0] + (this.segmentEndPoint[0]-this.segmentStartPoint[0])*this.segmentProgression/1000;
        this.y = this.segmentStartPoint[1] + (this.segmentEndPoint[1]-this.segmentStartPoint[1])*this.segmentProgression/1000;
    }
    
    public void doTick(int gameTime) {
    	//if (this.path.equals(new PathEmpty())) return;//kvuli nepohybujicim se mobum
        if (this.inactive) {
            // not started yet.
            if (this.delay > 0) {
                this.delay--;
                if (this.delay == 0) {
                    this.inactive = false;
                    this.validTarget = true;
                    this.doTick(gameTime);
                }
            }
        } else if (this.dead) {
        } else {
            this.segmentProgression += this.speed;
            if (this.segmentProgression >= 1000) {
                this.segmentProgression -= 1000;
                this.segment++;
                if (this.segment >= this.path.length()) {
                    this.segment = 0;
                    if(this.price == 0) this.context.deductScore(10);	//TODO odecitani score pokud ma vlna value 0
                    	else this.context.deductScore(this.price);
                    this.context.removeLife();
                }
                this.segmentStartPoint = this.path.getStep(this.segment);
                this.segmentEndPoint = this.path.getStep(this.segment+1);
            }
            this.x = this.segmentStartPoint[0] + (this.segmentEndPoint[0]-this.segmentStartPoint[0])*this.segmentProgression/1000;
            this.y = this.segmentStartPoint[1] + (this.segmentEndPoint[1]-this.segmentStartPoint[1])*this.segmentProgression/1000;
            atTranslate.setToIdentity();
            atTranslate.translate(this.x,this.y);
            //System.out.println("EnemyMobBase::doTick: "+this.x+"."+this.y);
            if (this.x < 0 || this.x > this.context.maxX || this.y < 0 || this.y > this.context.maxY) {
                this.validTarget = false;
            } else {
                this.validTarget = true;
            }
        }
    }

    public EnemyMob create(Context context, int delay, int health, int price, int level) {
        AbstractEnemyMob newEnemy = null;
        try {
            newEnemy = (AbstractEnemyMob)this.clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
        }
        newEnemy.doInit(context, delay, health, price, level);
        
        return newEnemy;
    }
}
