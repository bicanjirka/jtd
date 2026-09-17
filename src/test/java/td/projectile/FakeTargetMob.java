package td.projectile;

import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyMobVisitor;

import java.util.Set;

/** A minimal, mutable {@link EnemyMob} double - lets a test move a target mid-flight or invalidate it. */
final class FakeTargetMob implements EnemyMob {

    private double x;
    private double y;
    private boolean valid = true;

    FakeTargetMob(double x, double y) {
        this.x = x;
        this.y = y;
    }

    void moveTo(double x, double y) {
        this.x = x;
        this.y = y;
    }

    void invalidate() {
        this.valid = false;
    }

    @Override
    public void doTick(int gameTime) {
    }

    @Override
    public <R> R accept(EnemyMobVisitor<R> visitor) {
        throw new UnsupportedOperationException("FakeTargetMob is not a real enemy kind");
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
        return 0;
    }

    @Override
    public boolean validTarget() {
        return this.valid;
    }

    @Override
    public boolean validTarget(Type type) {
        return this.valid;
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
        return !this.valid;
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
