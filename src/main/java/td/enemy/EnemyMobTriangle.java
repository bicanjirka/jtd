package td.enemy;

import td.damage.Damage;
import td.util.GameWorld;

public final class EnemyMobTriangle extends AbstractEnemyMobRotor {

    private float bodyScale;

    public EnemyMobTriangle() {
        super();
        this.rotPerTime = (float) Math.toRadians(-5.0);
    }

    protected void doInit(GameWorld context, int delay, int health, int price, int level) {
        super.doInit(context, delay, health, price, level);
        this.bodyScale = (float) this.context.getBoard().scale() / ((this.level < 6) ? (7 - level) : (2));
        this.speedMax = (float) (this.speedBase * (1.4 + 0.1 * this.level));
    }

    public float getBodyScale() {
        return this.bodyScale;
    }

    public void doDamage(Damage damage) {
        super.doDamage(damage);
        this.speed = this.speedBase + (this.speedMax - this.speedBase) * (1 - (this.health / (float) this.healthMax));
    }

    public <R> R accept(EnemyMobVisitor<R> visitor) {
        return visitor.visitTriangle(this);
    }

    @Override
    public String getInfoString() {
        return """
                Triangle mob

                Increases speed as it takes damage.""";
    }

}
