package td.enemy;

import java.awt.geom.AffineTransform;

import td.util.Context;

public abstract class AbstractEnemyMobDirectional extends AbstractEnemyMob {
	
	protected AffineTransform atRotate;
	
	protected void doInit(Context context, int path, int delay, int health, int price) {
        this.atRotate = new AffineTransform();
        super.doInit(context, path, delay, health, price);
    }
	
	public void doTick(int gameTime) {
		int oldx = this.getX();
        int oldy = this.getY();
        super.doTick(gameTime);
        double a = Math.atan2(this.getY()-oldy, this.getX()-oldx);
        this.atRotate.setToIdentity();
        this.atRotate.rotate(a);
	}
}
