package td.enemy;

import td.damage.Damage;
import td.util.GameWorld;

public final class EnemyMobSquare extends AbstractEnemyMobRotor {

    private float bodyScale;
    private float K;

    public EnemyMobSquare() {
        super();
    }

    protected void doInit(GameWorld context, int delay, int health, int price, int level) {
        super.doInit(context, delay, health, price, level);
        this.bodyScale = (float) this.context.getBoard().scale() / ((this.level < 6) ? (7 - level) : (2));
        K = 0.8f - this.level * 0.05f;
    }

    public float getBodyScale() {
        return this.bodyScale;
    }

    @Override
    protected Damage absorb(Damage incoming) {
        return incoming.scaledBy(this.K);
    }

    public <R> R accept(EnemyMobVisitor<R> visitor) {
        return visitor.visitSquare(this);
    }

    public String getInfoString() {
        return """
                Square mob

                Takes less damage.""";
    }
}
