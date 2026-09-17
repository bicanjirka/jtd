package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyMobVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An {@link EnemyMob} double that sits at a fixed point and records every hit and effect
 * aimed at it. A real mob's position comes from how far it has walked the level's path, which
 * is no way to place one at a chosen distance from a blast centre.
 * <p>
 * It never dies and absorbs nothing, so a recorded hit is exactly what the tower decided to
 * deal - which is the thing under test here, separate from what a real mob would do with it.
 */
final class RecordingEnemyMob implements EnemyMob {

    private final double x;
    private final double y;
    private final type mobType;
    private final List<Damage> hits = new ArrayList<>();
    private final List<Effect> appliedEffects = new ArrayList<>();

    private RecordingEnemyMob(double x, double y, type mobType) {
        this.x = x;
        this.y = y;
        this.mobType = mobType;
    }

    static RecordingEnemyMob normalAt(double x, double y) {
        return new RecordingEnemyMob(x, y, type.Normal);
    }

    /** Invisible mobs are excluded from a tower's primary-target scan but not from its splash. */
    static RecordingEnemyMob ghostAt(double x, double y) {
        return new RecordingEnemyMob(x, y, type.Invisible);
    }

    List<Damage> hits() {
        return this.hits;
    }

    int onlyHitAmount() {
        if (this.hits.size() != 1) {
            throw new IllegalStateException("expected exactly one hit, got " + this.hits.size());
        }
        return this.hits.get(0).amount();
    }

    @Override
    public Damage doDamage(Damage damage) {
        this.hits.add(damage);
        return damage;
    }

    @Override
    public void applyEffect(Effect effect) {
        this.appliedEffects.add(effect);
    }

    List<Effect> appliedEffects() {
        return this.appliedEffects;
    }

    @Override
    public Set<EffectKind> activeEffectKinds() {
        return Set.of();
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
    public boolean validTarget() {
        return true;
    }

    @Override
    public boolean validTarget(type type) {
        return type == this.mobType;
    }

    @Override
    public boolean validTarget(type type0, type type1) {
        return this.validTarget(type0) || this.validTarget(type1);
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public int getHealth() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getBounty() {
        return 0;
    }

    @Override
    public int getProgression() {
        return 0;
    }

    @Override
    public float getSpeed() {
        return 0;
    }

    @Override
    public void doTick(int gameTime) {
    }

    @Override
    public <R> R accept(EnemyMobVisitor<R> visitor) {
        throw new UnsupportedOperationException("RecordingEnemyMob is not a real enemy kind");
    }

    @Override
    public String getInfoString() {
        return "recording";
    }
}
