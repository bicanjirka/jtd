package td.enemy;

import td.util.Context;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public final class EnemyMobSquare extends AbstractEnemyMobRotor {

    private Shape bodyShape;
    private float bodyScale;
    private float K;

    public EnemyMobSquare() {
        super();
        this.color = this.colorTrans = Color.PINK;
    }

    protected void doInit(Context context, int delay, int health, int price, int level) {
        super.doInit(context, delay, health, price, level);
        this.bodyScale = (float) this.context.scale / ((this.level < 6) ? (7 - level) : (2));
        this.bodyShape = new Rectangle2D.Float(-this.bodyScale, -this.bodyScale, this.bodyScale * 2, this.bodyScale * 2);
        K = 0.8f - this.level * 0.05f;
    }

    public void doDamage(int damage) {
        super.doDamage((int) (damage * K));
    }

    public String getInfoString() {
        return """
                Square mob
                
                Takes less damage.""";
    }

    public void paint(Graphics2D g2, int gameTime) {
        g2.setColor(this.color);
        AffineTransform saveXform = g2.getTransform();
        g2.transform(this.atTranslate);
        g2.transform(this.atRotate);

        if (this.dead) {
            int age = this.ticksSinceDeath(gameTime);
            if (!this.isFadeComplete(gameTime)) {
                g2.setColor(new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.fadeAlpha(age)));
                int growth = age * 2;
                g2.draw(new Rectangle2D.Float(-(this.bodyScale + growth), -this.bodyScale, (this.bodyScale + growth) * 2, this.bodyScale * 2));
                g2.draw(new Rectangle2D.Float(-this.bodyScale, -(this.bodyScale + growth), this.bodyScale * 2, (this.bodyScale + growth) * 2));
            }
        } else if (!this.inactive) {
            g2.draw(this.bodyShape);
            g2.setColor(this.colorTrans);
            g2.fill(this.bodyShape);
        }
        g2.setTransform(saveXform);
    }
}
