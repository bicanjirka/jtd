package td.enemy;

public abstract class AbstractEnemyMobRotor extends AbstractEnemyMob {

    protected float rotPerTime = (float) Math.toRadians(5.0);
    private double facingRadians;
    private int lastTickTime;

    public void doTick(int gameTime) {
        super.doTick(gameTime);
        if (!this.inactive && !this.dead) {
            this.facingRadians += this.rotPerTime * (gameTime - this.lastTickTime);
        }
        this.lastTickTime = gameTime;
    }

    public double getFacingRadians() {
        return this.facingRadians;
    }
}
