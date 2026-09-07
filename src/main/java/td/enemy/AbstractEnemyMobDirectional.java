package td.enemy;

public abstract class AbstractEnemyMobDirectional extends AbstractEnemyMob {

    private double facingRadians;

    public void doTick(int gameTime) {
        int oldx = this.getX();
        int oldy = this.getY();
        super.doTick(gameTime);
        this.facingRadians = Math.atan2(this.getY() - oldy, this.getX() - oldx);
    }

    public double getFacingRadians() {
        return this.facingRadians;
    }
}
