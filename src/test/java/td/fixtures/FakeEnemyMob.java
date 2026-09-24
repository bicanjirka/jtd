package td.fixtures;

import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyMobVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An {@link EnemyMob} placed at a chosen point, which a real mob's path cannot do. It records
 * every hit and effect and absorbs nothing, so a recorded hit is exactly what was dealt; its
 * health never drops, so it dies only through {@link #invalidate()}.
 */
public final class FakeEnemyMob implements EnemyMob {

    private final int progression;
    private final int health;
    private final Type mobType;
    private final List<Damage> hits = new ArrayList<>();
    private final List<Effect> appliedEffects = new ArrayList<>();
    private double x;
    private double y;
    private boolean valid = true;

    private FakeEnemyMob(double x, double y, int progression, int health, Type mobType) {
        this.x = x;
        this.y = y;
        this.progression = progression;
        this.health = health;
        this.mobType = mobType;
    }

    public static FakeEnemyMob at(double x, double y) {
        return new FakeEnemyMob(x, y, 0, Integer.MAX_VALUE, Type.NORMAL);
    }

    /** Excluded from primary-target scans but not from splash. */
    public static FakeEnemyMob ghostAt(double x, double y) {
        return at(x, y).withType(Type.INVISIBLE);
    }

    public FakeEnemyMob withProgression(int progression) {
        return new FakeEnemyMob(this.x, this.y, progression, this.health, this.mobType);
    }

    public FakeEnemyMob withHealth(int health) {
        return new FakeEnemyMob(this.x, this.y, this.progression, health, this.mobType);
    }

    public FakeEnemyMob withType(Type mobType) {
        return new FakeEnemyMob(this.x, this.y, this.progression, this.health, mobType);
    }

    public void moveTo(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** Makes it dead and no longer targetable. */
    public void invalidate() {
        this.valid = false;
    }

    public List<Damage> hits() {
        return List.copyOf(this.hits);
    }

    public int onlyHitAmount() {
        if (this.hits.size() != 1) {
            throw new IllegalStateException("expected exactly one hit, got " + this.hits.size());
        }
        return this.hits.getFirst().amount();
    }

    public List<Effect> appliedEffects() {
        return List.copyOf(this.appliedEffects);
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
    public int getProgression() {
        return this.progression;
    }

    @Override
    public boolean validTarget() {
        return this.valid;
    }

    @Override
    public boolean validTarget(Type type) {
        return this.valid && type == this.mobType;
    }

    @Override
    public boolean validTarget(Type type0, Type type1) {
        return this.validTarget(type0) || this.validTarget(type1);
    }

    @Override
    public boolean isDead() {
        return !this.valid;
    }

    @Override
    public int getHealth() {
        return this.health;
    }

    @Override
    public int getBounty() {
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
        throw new UnsupportedOperationException("FakeEnemyMob is not a real enemy kind");
    }

    @Override
    public String getInfoString() {
        return "fake";
    }
}
