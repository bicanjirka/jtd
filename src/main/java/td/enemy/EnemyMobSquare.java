package td.enemy;

import td.damage.Damage;
import td.util.GameWorld;

/**
 * Absorbs a fraction of every incoming hit, and absorbs more of it at higher wave levels.
 * The surviving fraction reaches zero at wave level 16 and goes negative past it, which
 * {@link Damage}'s zero-clamp turns into "immune" rather than into a healing hit.
 */
public final class EnemyMobSquare extends AbstractEnemyMobRotor {

    private float bodyScale;
    // Fraction of an incoming hit that actually lands, shrinking as wave level rises.
    private float K;

    public EnemyMobSquare(GameWorld gameWorld, int delay, int health, int price, int level) {
        super();
        this.doInit(gameWorld, delay, health, price, level);
    }

    protected void doInit(GameWorld gameWorld, int delay, int health, int price, int level) {
        super.doInit(gameWorld, delay, health, price, level);
        this.bodyScale = (float) this.gameWorld.getBoard().scale() / ((this.level < 6) ? (7 - level) : (2));
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
