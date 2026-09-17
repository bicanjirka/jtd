package td.tower.targeting;

import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyMobVisitor;

import java.util.Set;

/**
 * A minimal, immutable {@link EnemyMob} double - only the geometry/targeting-relevant state is real.
 */
final class FakeEnemyMob implements EnemyMob {

    private final int x;
    private final int y;
    private final int progression;
    private final Type mobType;
    private final boolean validTarget;

    private FakeEnemyMob(int x, int y, int progression, Type mobType, boolean validTarget) {
        this.x = x;
        this.y = y;
        this.progression = progression;
        this.mobType = mobType;
        this.validTarget = validTarget;
    }

    static FakeEnemyMob at(int x, int y) {
        return new FakeEnemyMob(x, y, 0, Type.NORMAL, true);
    }

    FakeEnemyMob withProgression(int progression) {
        return new FakeEnemyMob(this.x, this.y, progression, this.mobType, this.validTarget);
    }

    FakeEnemyMob withType(Type mobType) {
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
    public boolean validTarget(Type type) {
        return this.validTarget && this.mobType == type;
    }

    @Override
    public boolean validTarget(Type type0, Type type1) {
        return validTarget(type0) || validTarget(type1);
    }

    @Override
    public int getHealth() {
        return 1;
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public int getBounty() {
        return 0;
    }

    @Override
    public Damage doDamage(Damage damage) {
        return damage;
    }

    @Override
    public void applyEffect(Effect effect) {
    }

    @Override
    public Set<EffectKind> activeEffectKinds() {
        return Set.of();
    }

    @Override
    public float getSpeed() {
        return 0;
    }

    @Override
    public String getInfoString() {
        return "";
    }
}
