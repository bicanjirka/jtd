package td.enemy;

// Placeholder enemy used to create timing gaps between real enemies within a wave.
public final class EnemyMobEmpty extends AbstractEnemyMob {

    public EnemyMobEmpty() {
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
