package td.tower.targeting;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.enemy.EnemyMobVisitor;
import td.util.GameWorld;

/** A minimal, immutable {@link EnemyMob} double - only the geometry/targeting-relevant state is real. */
final class FakeEnemyMob implements EnemyMob {

    private final int x;
    private final int y;
    private final int progression;
    private final type mobType;
    private final boolean validTarget;

    private FakeEnemyMob(int x, int y, int progression, type mobType, boolean validTarget) {
        this.x = x;
        this.y = y;
        this.progression = progression;
        this.mobType = mobType;
        this.validTarget = validTarget;
    }

    static FakeEnemyMob at(int x, int y) {
        return new FakeEnemyMob(x, y, 0, type.Normal, true);
    }

    FakeEnemyMob withProgression(int progression) {
        return new FakeEnemyMob(this.x, this.y, progression, this.mobType, this.validTarget);
    }

    FakeEnemyMob withType(type mobType) {
        return new FakeEnemyMob(this.x, this.y, this.progression, mobType, this.validTarget);
    }

    FakeEnemyMob invalid() {
        return new FakeEnemyMob(this.x, this.y, this.progression, this.mobType, false);
    }

    @Override
    public void doTick(int gameTime) {
    }

    @Override
    public <R> R accept(EnemyMobVisitor<R> visitor) {
        throw new UnsupportedOperationException("FakeEnemyMob is not a real enemy kind");
    }

    @Override
    public double getX() {
        return this.x;
    }

    @Override
    public double getY() {
        return this.y;
    }

    @Override
    public int getProgression() {
        return this.progression;
    }

    @Override
    public boolean validTarget() {
        return this.validTarget;
    }

    @Override
    public boolean validTarget(type type) {
        return this.validTarget && this.mobType == type;
    }

    @Override
    public boolean validTarget(type type0, type type1) {
        return validTarget(type0) || validTarget(type1);
    }

    @Override
    public long getHealth() {
        return 1;
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public void doDamage(Damage damage) {
    }

    @Override
    public float getSpeed() {
        return 0;
    }

    @Override
    public String getInfoString() {
        return "";
    }

    @Override
    public EnemyMob create(GameWorld context, int delay, int health, int price, int level) {
        throw new UnsupportedOperationException("FakeEnemyMob is not a prototype");
    }
}
