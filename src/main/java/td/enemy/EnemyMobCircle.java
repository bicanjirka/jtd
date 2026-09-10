package td.enemy;

import td.util.GameWorld;

/**
 * The baseline enemy: no resistance, no speed curve, no invisibility. Its body is a fixed
 * fraction of a cell regardless of wave level, which is what makes it visibly the smallest
 * mob in the later waves.
 */
public final class EnemyMobCircle extends AbstractEnemyMob {

    private float bodyScale;

    public EnemyMobCircle(GameWorld gameWorld, int delay, int health, int price, int level) {
        super();
        this.doInit(gameWorld, delay, health, price, level);
    }

    protected void doInit(GameWorld gameWorld, int delay, int health, int price, int level) {
        super.doInit(gameWorld, delay, health, price, level);
        this.bodyScale = this.gameWorld.getBoard().scale() / 6f;
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
