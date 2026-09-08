package td.enemy;

import td.util.Context;

public final class EnemyMobCircle extends AbstractEnemyMob {

    private float bodyScale;

    public EnemyMobCircle() {
        super();
    }

    protected void doInit(Context context, int delay, int health, int price, int level) {
        super.doInit(context, delay, health, price, level);
        this.bodyScale = this.context.getBoard().scale() / 6f;
    }

    public float getBodyScale() {
        return this.bodyScale;
    }

    public <R> R accept(EnemyMobVisitor<R> visitor) {
        return visitor.visitCircle(this);
    }

    public String getInfoString() {
        return """
                Simple mob

                No special abilities.""";
    }
}
