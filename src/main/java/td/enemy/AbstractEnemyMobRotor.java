package td.enemy;

import td.util.Context;

import java.awt.geom.AffineTransform;

public abstract class AbstractEnemyMobRotor extends AbstractEnemyMob {

    protected float rotPerTime = (float) Math.toRadians(5.0);
    protected AffineTransform atRotate;
    private int lastTickTime;

    protected void doInit(Context context, int delay, int health, int price, int level) {
        this.atRotate = new AffineTransform();
        super.doInit(context, delay, health, price, level);
    }

    public void doTick(int gameTime) {
        super.doTick(gameTime);
        if (!this.inactive && !this.dead) {
            this.atRotate.rotate(this.rotPerTime * (gameTime - this.lastTickTime));
        }
        this.lastTickTime = gameTime;
    }
}
