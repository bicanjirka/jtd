package td.enemy;

import td.util.Context;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

public final class EnemyMobGhost extends AbstractEnemyMob {

    private Shape bodyShape;
    private float bodyScale;

    public EnemyMobGhost() {
        super();
        this.color = this.colorTrans = Color.LIGHT_GRAY;
        this.type = EnemyMob.type.Invisible;
    }

    protected void doInit(Context context, int delay, int health, int price, int level) {
        super.doInit(context, delay, (health / 5), price, level);
        this.bodyScale = (float) this.context.scale / ((this.level < 6) ? (7 - level) : (2));
        this.bodyShape = new Ellipse2D.Float(-this.bodyScale, -this.bodyScale, this.bodyScale * 2, this.bodyScale * 2);
    }

    public String getInfoString() {
        return """
                Ghost mob
                
                Invisible to all towers. Area damage hurts them.""";
    }

    public void paint(Graphics2D g2, int gameTime) {
        g2.setColor(this.color);
        AffineTransform saveXform = g2.getTransform();
        g2.transform(this.atTranslate);
        if (this.dead) {
            int age = this.ticksSinceDeath(gameTime);
            if (!this.isFadeComplete(gameTime)) {
                g2.setColor(new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.fadeAlpha(age)));
                g2.draw(new Ellipse2D.Float(-(this.bodyScale + age), -(this.bodyScale + age), (this.bodyScale + age) * 2, (this.bodyScale + age) * 2));
            }
        } else if (!this.inactive) {
            g2.draw(this.bodyShape);
            g2.setColor(this.colorTrans);
            g2.fill(this.bodyShape);
        }
        g2.setTransform(saveXform);
    }
}