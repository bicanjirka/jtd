package td.enemy;

import td.util.Context;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

/**
 * Zakladni nepritel, znazornen koleckem
 *
 * @author Jirka
 *
 */
public class EnemyMobCircle extends AbstractEnemyMob {

    private int deadTime;
    private boolean gone;
    private Shape bodyShape;
    private float bodyScale;

    public EnemyMobCircle() {
        super();
        this.color = this.colorTrans = Color.CYAN;
    }

    protected void doInit(Context context, int delay, int health, int price, int level) {
        super.doInit(context, delay, health, price, level);
        this.bodyScale = this.context.scale / 6f;
        this.bodyShape = new Ellipse2D.Float(-this.bodyScale, -this.bodyScale, this.bodyScale * 2, this.bodyScale * 2);
    }

    public String getInfoString() {
        return """
                Simple mob
                
                No special abilities.""";
    }

    public void paint(Graphics2D g2, int gameTime) {
        g2.setColor(this.color);
        AffineTransform saveXform = g2.getTransform();
        g2.transform(this.atTranslate);
        if (this.dead) {
            if (!this.gone) {
                if (this.deadTime != 0) {
                    Color tempColor = g2.getColor();
                    int i = gameTime - this.deadTime;
                    if (i > (3 * this.level + 6)) this.gone = true;
                    int alpha = (255 - (i * (255 / ((3 * this.level + 6) + 1))));
                    g2.setColor(new Color(tempColor.getRed(), tempColor.getGreen(), tempColor.getBlue(), ((alpha < 0) ? 0 : alpha)));
                    g2.draw(new Ellipse2D.Float(-(this.bodyScale + i), -(this.bodyScale + i), (this.bodyScale + i) * 2, (this.bodyScale + i) * 2));
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
