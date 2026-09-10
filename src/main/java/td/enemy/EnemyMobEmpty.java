package td.enemy;

import td.util.GameWorld;

/**
 * Not a real enemy: a spacer that occupies a spawn slot so the enemies after it arrive later,
 * without ever appearing on the board. It never ticks, is never a valid target, and is not
 * counted by {@code WaveContent.enemyCount()} - clearing a wave does not require killing it.
 */
public final class EnemyMobEmpty extends AbstractEnemyMob {

    public EnemyMobEmpty(GameWorld gameWorld, int delay, int health, int price, int level) {
        super();
        this.doInit(gameWorld, delay, health, price, level);
    }

    public void doTick(int gameTime) {
    }

    public <R> R accept(EnemyMobVisitor<R> visitor) {
        return visitor.visitEmpty(this);
    }

    public String getInfoString() {
        return null;
    }

    public boolean validTarget() {
        return false;
    }

}
