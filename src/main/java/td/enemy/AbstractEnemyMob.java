package td.enemy;

import td.util.Context;
import td.wave.Path;
import td.wave.Point;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Objects;

public abstract class AbstractEnemyMob implements EnemyMob, Cloneable {

    protected type type;
    protected boolean inactive = true;
    protected boolean validTarget = false;
    protected boolean dead = false;
    protected int price;
    protected int level;
    protected Color colorTrans = Color.WHITE;
    protected Color color = Color.WHITE;
    protected int x, y;
    protected AffineTransform atTranslate;
    protected int speed = 40;
    protected int speedMax = 40;
    protected final int speedBase = 40;
    protected int health;
    protected int healthMax;
    protected int alpha = 255;
    protected Context context;
    private int delay;
    private Path path;
    private int segment = 0;
    private int segmentProgression = 0;
    private Point segmentStartPoint;
    private Point segmentEndPoint;


    public AbstractEnemyMob() {
        this.type = EnemyMob.type.Normal;
    }

    protected void doInit(Context context, int delay, int health, int price, int level) {
        this.context = context;
        this.price = price;
        this.level = level;
        this.health = health * 100;
        this.healthMax = health * 100;
        this.path = this.context.getPath();
        this.x = 0;
        this.y = 0;
        this.delay = Math.round(700f * delay / this.speed);
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
            this.alpha = (int) (((float) this.health / this.healthMax) * 255);
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
        return 1000 * this.segment + this.segmentProgression;
    }

    public boolean validTarget() {
        return ((!this.inactive) && this.validTarget && (!this.dead));
    }

    public boolean validTarget(type type) {
        return (this.validTarget() && (type.equals(this.type)));
    }

    public boolean validTarget(type type1, type type2) {
        return (this.validTarget(type1) || this.validTarget(type2));
    }

    private void resetPosition() {
        this.segmentStartPoint = this.path.getStep(this.segment);
        this.segmentEndPoint = this.path.getStep(this.segment + 1);
        this.x = this.segmentStartPoint.x() + (this.segmentEndPoint.x() - this.segmentStartPoint.x()) * this.segmentProgression / 1000;
        this.y = this.segmentStartPoint.y() + (this.segmentEndPoint.y() - this.segmentStartPoint.y()) * this.segmentProgression / 1000;
    }

    public void doTick(int gameTime) {
        if (this.inactive) {
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
                    if (this.price == 0) this.context.deductScore(10);
                    else this.context.deductScore(this.price);
                    this.context.removeLife();
                }
                this.segmentStartPoint = this.path.getStep(this.segment);
                this.segmentEndPoint = this.path.getStep(this.segment + 1);
            }
            this.x = this.segmentStartPoint.x() + (this.segmentEndPoint.x() - this.segmentStartPoint.x()) * this.segmentProgression / 1000;
            this.y = this.segmentStartPoint.y() + (this.segmentEndPoint.y() - this.segmentStartPoint.y()) * this.segmentProgression / 1000;
            atTranslate.setToIdentity();
            atTranslate.translate(this.x, this.y);
            this.validTarget = this.x >= 0 && this.x <= this.context.maxX && this.y >= 0 && this.y <= this.context.maxY;
        }
    }

    public EnemyMob create(Context context, int delay, int health, int price, int level) {
        AbstractEnemyMob newEnemy = null;
        try {
            newEnemy = (AbstractEnemyMob) this.clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
        }
        Objects.requireNonNull(newEnemy).doInit(context, delay, health, price, level);

        return newEnemy;
    }
}
