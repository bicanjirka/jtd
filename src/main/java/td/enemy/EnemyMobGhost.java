package td.enemy;

import td.util.GameWorld;

/**
 * Invisible to every single-target tower ({@link EnemyMob.type#Invisible}), so only area
 * damage reaches it. Paid for with a fifth of the wave's health - note that divisor is flat
 * rather than level-scaled the way its body size is; see {@code TODO.md}.
 */
public final class EnemyMobGhost extends AbstractEnemyMob {

    private float bodyScale;

    public EnemyMobGhost(GameWorld gameWorld, int delay, int health, int price, int level) {
        super();
        this.type = EnemyMob.type.Invisible;
        this.doInit(gameWorld, delay, health, price, level);
    }

    protected void doInit(GameWorld gameWorld, int delay, int health, int price, int level) {
        super.doInit(gameWorld, delay, (health / 5), price, level);
        this.bodyScale = (float) this.gameWorld.getBoard().scale() / ((this.level < 6) ? (7 - level) : (2));
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
