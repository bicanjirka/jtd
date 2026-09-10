package td.enemy;

import td.damage.Damage;
import td.util.GameWorld;

/**
 * Accelerates as it is damaged, from {@code speedBase} at full health up to a level-scaled
 * {@code speedMax} as it nears death - so chipping at one without finishing it is worse than
 * leaving it alone. Also the one mob that spins the opposite way to {@code EnemyMobSquare}.
 */
public final class EnemyMobTriangle extends AbstractEnemyMobRotor {

    private float bodyScale;

    public EnemyMobTriangle(GameWorld context, int delay, int health, int price, int level) {
        super();
        this.rotPerTime = (float) Math.toRadians(-5.0);
        this.doInit(context, delay, health, price, level);
    }

    protected void doInit(GameWorld context, int delay, int health, int price, int level) {
        super.doInit(context, delay, health, price, level);
        this.bodyScale = (float) this.context.getBoard().scale() / ((this.level < 6) ? (7 - level) : (2));
        this.speedMax = (float) (this.speedBase * (1.4 + 0.1 * this.level));
    }

    public float getBodyScale() {
        return this.bodyScale;
    }

    public Damage doDamage(Damage damage) {
        Damage landed = super.doDamage(damage);
        this.speed = this.speedBase + (this.speedMax - this.speedBase) * (1 - (this.health / (float) this.healthMax));
        return landed;
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
