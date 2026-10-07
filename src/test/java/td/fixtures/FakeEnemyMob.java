package td.fixtures;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyMobVisitor;
import td.enemy.Rank;

import java.util.ArrayList;
import java.util.EnumSet;
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
    private final boolean hidden;
    private final List<Damage> hits = new ArrayList<>();
    private final List<Effect> appliedEffects = new ArrayList<>();
    private final List<AttackProfile> attackers = new ArrayList<>();
    private final List<Float> shieldBreaks = new ArrayList<>();
    private boolean hitsLandCritical;
    private boolean diesOnHit;
    private boolean appliesEffects;
    private boolean frozen;
    private float physicalReduction;
    private double x;
    private double y;
    private boolean valid = true;
    private Rank rank = Rank.GRUNT;
    private float healthFraction = 1f;

    private FakeEnemyMob(double x, double y, int progression, int health, boolean hidden) {
        this.x = x;
        this.y = y;
        this.progression = progression;
        this.health = health;
        this.hidden = hidden;
    }

    public static FakeEnemyMob at(double x, double y) {
        return new FakeEnemyMob(x, y, 0, Integer.MAX_VALUE, false);
    }

    /** Excluded from primary-target scans but not from splash. */
    public static FakeEnemyMob ghostAt(double x, double y) {
        return at(x, y).hidden();
    }

    public FakeEnemyMob withProgression(int progression) {
        return new FakeEnemyMob(this.x, this.y, progression, this.health, this.hidden).likeThis(this);
    }

    public FakeEnemyMob withHealth(int health) {
        return new FakeEnemyMob(this.x, this.y, this.progression, health, this.hidden).likeThis(this);
    }

    /** This fake itself, with the rank, health fraction and abilities of {@code other}. */
    private FakeEnemyMob likeThis(FakeEnemyMob other) {
        this.rank = other.rank;
        this.healthFraction = other.healthFraction;
        this.appliesEffects = other.appliesEffects;
        return this;
    }

    /** This fake itself, now an enemy that heals, shields or vanishes. */
    public FakeEnemyMob thatAppliesEffects() {
        this.appliesEffects = true;
        return this;
    }

    @Override
    public boolean appliesEffects() {
        return this.appliesEffects;
    }

    /** This fake itself, now of {@code rank}. */
    public FakeEnemyMob ranked(Rank rank) {
        this.rank = rank;
        return this;
    }

    /** This fake itself, now at {@code healthFraction} of its health. */
    public FakeEnemyMob atHealthFraction(float healthFraction) {
        this.healthFraction = healthFraction;
        return this;
    }

    @Override
    public Rank getRank() {
        return this.rank;
    }

    @Override
    public float getHealthFraction() {
        return this.healthFraction;
    }

    /** A copy that stealth keeps towers from targeting. */
    public FakeEnemyMob hidden() {
        return new FakeEnemyMob(this.x, this.y, this.progression, this.health, true).likeThis(this);
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

    /** The attack profile each recorded hit carried, in hit order. */
    public List<AttackProfile> attackers() {
        return List.copyOf(this.attackers);
    }

    /** From now on any hit kills it. */
    public void dieOnAnyHit() {
        this.diesOnHit = true;
    }

    /** Reports a freeze as active, without any real effect. */
    public void reportFrozen() {
        this.frozen = true;
    }

    /** From now on every hit comes back critical, as if every crit roll succeeded. */
    public void landEveryHitCritical() {
        this.hitsLandCritical = true;
    }

    public List<Effect> appliedEffects() {
        return List.copyOf(this.appliedEffects);
    }

    @Override
    public Damage doDamage(Damage damage, AttackProfile attacker) {
        this.hits.add(damage);
        this.attackers.add(attacker);
        if (this.diesOnHit) {
            this.valid = false;
        }
        return this.hitsLandCritical ? new Damage(damage.amount(), damage.type(), true) : damage;
    }

    /**
     * Reports {@code reduction} as its protection against physical hits, for towers that scale with
     * it; the hits it records are still unreduced.
     */
    public void reportPhysicalReduction(float reduction) {
        this.physicalReduction = reduction;
    }

    @Override
    public void breakShield(float fraction) {
        this.shieldBreaks.add(fraction);
    }

    public List<Float> shieldBreaks() {
        return List.copyOf(this.shieldBreaks);
    }

    @Override
    public boolean hasEffect(EffectKind kind) {
        return this.activeEffectKinds().contains(kind);
    }

    @Override
    public List<Effect> activeEffects() {
        return List.copyOf(this.appliedEffects);
    }

    /** Every hit it recorded, added up. */
    @Override
    public long damageTaken() {
        return this.hits.stream().mapToLong(Damage::amount).sum();
    }

    /** Ends every effect of {@code kind} on it, as if it had run out. */
    public void expire(EffectKind kind) {
        this.appliedEffects.removeIf(effect -> effect.kind() == kind);
    }

    /** The stacks every effect of {@code kind} put on it, added up to the highest cap among them. */
    @Override
    public int effectStacks(EffectKind kind) {
        int stacks = 0;
        int cap = 0;
        for (Effect effect : this.appliedEffects) {
            if (effect.kind() == kind) {
                stacks += effect.stacks();
                cap = Math.max(cap, effect.effectiveStackCap());
            }
        }
        return Math.min(stacks, cap);
    }

    @Override
    public float critFactorFor(AttackProfile attacker) {
        return attacker.critMultiplier();
    }

    @Override
    public float reductionAgainst(DamageType type) {
        return type == DamageType.PHYSICAL ? this.physicalReduction : 0f;
    }

    @Override
    public void applyEffect(Effect effect) {
        this.appliedEffects.add(effect);
    }

    @Override
    public Set<EffectKind> activeEffectKinds() {
        Set<EffectKind> kinds = EnumSet.noneOf(EffectKind.class);
        if (this.frozen) {
            kinds.add(EffectKind.FREEZE);
        }
        this.appliedEffects.forEach(effect -> kinds.add(effect.kind()));
        return kinds;
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
    public boolean isHidden() {
        return this.hidden;
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

    /** Entered after every tower, so only its position decides whether a tower reached it. */
    @Override
    public long entryOrdinal() {
        return Long.MAX_VALUE;
    }

    /** It walks nowhere: only where it stands counts. */
    @Override
    public boolean walkedWithin(double pointX, double pointY, double radius) {
        return Math.hypot(this.x - pointX, this.y - pointY) <= radius;
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
}
