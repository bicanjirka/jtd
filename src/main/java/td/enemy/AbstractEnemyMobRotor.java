package td.enemy;

import td.util.Context;

import java.awt.*;
import java.awt.geom.AffineTransform;

public abstract class AbstractEnemyMobRotor extends AbstractEnemyMob {

    protected float rotPerTime = (float) Math.toRadians(5.0);
    protected AffineTransform atRotate;
    private int prevPaintTime;

    protected void doInit(Context context, int path, int delay, int health, int price) {
        this.atRotate = new AffineTransform();
        super.doInit(context, path, delay, health, price);
    }

    @Override
    public void paint(Graphics2D g2, int gameTime) {
        if (!this.inactive && !this.dead) {
            this.atRotate.rotate(this.rotPerTime * (gameTime - this.prevPaintTime));
        }
        this.prevPaintTime = gameTime;
    }
}
