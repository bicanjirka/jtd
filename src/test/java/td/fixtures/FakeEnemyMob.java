package td.fixtures;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.ActiveEffects;
import td.effect.Effect;
import td.effect.EffectCategory;
import td.effect.EffectKind;
import td.enemy.EnemyInspection;
import td.enemy.EnemyMob;
import td.enemy.EnemyMobVisitor;
import td.enemy.HitResult;
import td.enemy.Rank;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * An {@link EnemyMob} placed at a chosen point, which a real mob's path cannot do. It records
 * every hit and effect and absorbs nothing, so a recorded hit is exactly what was dealt; its
 * health never drops, so it dies only through {@link #invalidate()}. What it applies goes into a real
 * {@link ActiveEffects}, which answers every effect query, so stacking and caps are the game's; time never
 * runs on it, so an effect ends only through {@link #expire}, a dispel or lost stacks.
 */
public final class FakeEnemyMob implements EnemyMob {

    private final int progression;
    private final int health;
    private final boolean hidden;
    private final List<Damage> hits = new ArrayList<>();
    private final List<Effect> appliedEffects = new ArrayList<>();
    private final ActiveEffects live = new ActiveEffects();
    private final List<AttackProfile> attackers = new ArrayList<>();
    private final List<Float> shieldBreaks = new ArrayList<>();
    private boolean hitsLandCritical;
    private boolean diesOnHit;
    private boolean appliesEffects;
    private boolean regenerates;
    private boolean taunting;
    private float speed;
    private boolean frozen;
    private float spirit;
    private int dispels;
    private int buffDispels;
    private float physicalReduction;
    private double x;
    private double y;
    private double velocityX;
    private double velocityY;
    private boolean valid = true;
    private boolean leaked;
    private final Set<EffectKind> immunities = EnumSet.noneOf(EffectKind.class);
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
        this.regenerates = other.regenerates;
        this.taunting = other.taunting;
        this.speed = other.speed;
        return this;
    }

    /** This fake itself, now moving at {@code speed} pixels a tick. */
    public FakeEnemyMob movingAt(float speed) {
        this.speed = speed;
        return this;
    }

    /** This fake itself, now walking {@code dx}, {@code dy} pixels a tick, as {@link #positionAfter} sees it. */
    public FakeEnemyMob walkingBy(double dx, double dy) {
        this.velocityX = dx;
        this.velocityY = dy;
        return this;
    }

    @Override
    public Vec2 positionAfter(int ticks) {
        return new Vec2(this.x + this.velocityX * ticks, this.y + this.velocityY * ticks);
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

    /** This fake itself, now an enemy with regeneration and no ability behind it. */
    public FakeEnemyMob thatRegenerates() {
        this.regenerates = true;
        return this;
    }

    @Override
    public boolean regenerates() {
        return this.regenerates;
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

    /** Every effect applied to it that took hold, in order, whether or not it still runs. */
    public List<Effect> appliedEffects() {
        return List.copyOf(this.appliedEffects);
    }

    @Override
    public HitResult takeHit(Damage damage, AttackProfile attacker) {
        this.hits.add(damage);
        this.attackers.add(attacker);
        if (this.diesOnHit) {
            this.valid = false;
        }
        return new HitResult(this.hitsLandCritical ? new Damage(damage.amount(), damage.type(), true) : damage, 0);
    }

    /**
     * Reports {@code reduction} as its protection against physical hits, for towers that scale with
     * it; the hits it records are still unreduced.
     */
    public void reportPhysicalReduction(float reduction) {
        this.physicalReduction = reduction;
    }

    @Override
    public void dispelBuffs() {
        this.live.activeKinds().stream().filter(kind -> kind.category() == EffectCategory.BUFF)
                .forEach(this.live::consume);
        this.buffDispels++;
    }

    /** How many times its buffs were dispelled. */
    public int buffDispels() {
        return this.buffDispels;
    }

    @Override
    public void loseStacks(EffectKind kind, int stacks) {
        this.live.loseStacks(kind, stacks);
    }

    @Override
    public void dispelRestoratives() {
        this.live.consume(EffectKind.SHIELD);
        this.live.consume(EffectKind.HEAL);
        this.dispels++;
    }

    /** How many times a dispel reached it. */
    public int dispels() {
        return this.dispels;
    }

    /** Reports {@code spirit} as its spirit, without any real sickness. */
    public void reportSpirit(float spirit) {
        this.spirit = spirit;
    }

    @Override
    public float spirit() {
        return this.spirit;
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
        return this.live.effects();
    }

    /** Every hit it recorded, added up. */
    @Override
    public long damageTaken() {
        return this.hits.stream().mapToLong(Damage::amount).sum();
    }

    /** Ends every effect of {@code kind} on it, as if it had run out. */
    public void expire(EffectKind kind) {
        this.live.consume(kind);
    }

    @Override
    public int effectStacks(EffectKind kind) {
        return this.live.stacks(kind);
    }

    @Override
    public boolean rollsCrit(AttackProfile attacker) {
        return this.hitsLandCritical;
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
        if (!this.immunities.contains(effect.kind())) {
            this.appliedEffects.add(effect);
            this.live.apply(effect);
        }
    }

    /** From now on {@code kind} doesn't take hold on it. */
    public FakeEnemyMob immuneTo(EffectKind kind) {
        this.immunities.add(kind);
        return this;
    }

    @Override
    public Set<EffectKind> activeEffectKinds() {
        Set<EffectKind> kinds = EnumSet.noneOf(EffectKind.class);
        if (this.frozen) {
            kinds.add(EffectKind.FREEZE);
        }
        kinds.addAll(this.live.activeKinds());
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

    /** This fake itself, now an enemy that single-target towers must aim at. */
    public FakeEnemyMob taunting() {
        this.taunting = true;
        return this;
    }

    @Override
    public boolean taunts() {
        return this.taunting;
    }

    @Override
    public boolean isDead() {
        return !this.valid;
    }

    @Override
    public EnemyInspection.Fate fate() {
        if (this.valid) {
            return EnemyInspection.Fate.ALIVE;
        }
        return this.leaked ? EnemyInspection.Fate.LEAKED : EnemyInspection.Fate.KILLED;
    }

    @Override
    public int livesOnLeak() {
        return 1;
    }

    @Override
    public boolean isInactive() {
        return false;
    }

    @Override
    public float getBodyScale() {
        return 1f;
    }

    @Override
    public EnemyInspection inspect() {
        throw new UnsupportedOperationException("FakeEnemyMob has no definition to inspect");
    }

    /** Ends its walk as a leak, not a kill. */
    public void leak() {
        this.valid = false;
        this.leaked = true;
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
        return this.speed;
    }

    @Override
    public void doTick(int gameTime) {
    }

    @Override
    public <R> R accept(EnemyMobVisitor<R> visitor) {
        throw new UnsupportedOperationException("FakeEnemyMob is not a real enemy kind");
    }
}
