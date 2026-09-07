package td.enemy;

import td.util.Context;

public final class EnemyMobGhost extends AbstractEnemyMob {

    private float bodyScale;

    public EnemyMobGhost() {
        super();
        this.type = EnemyMob.type.Invisible;
    }

    protected void doInit(Context context, int delay, int health, int price, int level) {
        super.doInit(context, delay, (health / 5), price, level);
        this.bodyScale = (float) this.context.scale / ((this.level < 6) ? (7 - level) : (2));
    }

    public float getBodyScale() {
        return this.bodyScale;
    }

    public <R> R accept(EnemyMobVisitor<R> visitor) {
        return visitor.visitGhost(this);
    }

    public String getInfoString() {
        return """
                Ghost mob

                Invisible to all towers. Area damage hurts them.""";
    }
}
