package td.tower;

import td.damage.AttackProfile;
import td.stat.DisruptionPenalty;
import td.stat.ResolvedStats;
import td.stat.StatModifiers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A tower's current stats as one immutable value. They are correlated and recomputed on the EDT
 * while tick code reads them, so they are swapped as a whole through one volatile field.
 *
 * @param values     every {@link TowerStat}, resolved
 * @param rangeReal  range in pixels
 * @param rangeReal2 {@code rangeReal} squared
 * @param attack     what every hit carries: crit chance and multiplier, penetration
 * @param disruption the enemy disruption these stats already include
 * @param reachReal  range in pixels before disruption: how far a tower sees for XP
 * @param received   what other towers' emissions give this one
 * @param capabilities what its nodes and statuses let it do
 */
public record TowerStats(ResolvedStats<TowerStat> values, float rangeReal, float rangeReal2, AttackProfile attack,
                         DisruptionPenalty disruption, float reachReal, StatModifiers<TowerStat> received,
                         Set<TowerCapability> capabilities) {

    public TowerStats {
        capabilities = capabilities.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
    }

    /**
     * Resolves the base with the tower's own modifiers, what it receives, and the disruption scaled by
     * what reaches it; {@code scale} converts range from cells to pixels.
     */
    public static TowerStats of(TowerBaseStats base, StatModifiers<TowerStat> own, StatModifiers<TowerStat> received,
                                DisruptionPenalty disruption, int scale, Set<TowerCapability> capabilities) {
        StatModifiers<TowerStat> buffs = own.plus(received);
        ResolvedStats<TowerStat> undisrupted = ResolvedStats.resolve(base.stats(), buffs, TowerStat.class);
        ResolvedStats<TowerStat> values = undisrupted;
        if (!disruption.isNone()) {
            float taken = undisrupted.value(TowerStat.DISRUPTION_TAKEN);
            values = ResolvedStats.resolve(base.stats(), buffs
                    .plus(TowerModifiers.attackSpeed(-disruption.attackSpeed() * taken))
                    .plus(TowerModifiers.range(-disruption.rangeCells() * taken)), TowerStat.class);
        }
        float rangeReal = values.value(TowerStat.RANGE) * scale;
        AttackProfile attack = AttackProfile.critChance(values.value(TowerStat.CRIT_CHANCE))
                .withAccuracy(values.value(TowerStat.ACCURACY))
                .withCritMultiplier(values.value(TowerStat.CRIT_MULTIPLIER))
                .withArmorPenetration(values.value(TowerStat.ARMOR_PENETRATION),
                        values.value(TowerStat.ARMOR_PENETRATION_FLAT))
                .withMagicPenetration(values.value(TowerStat.MAGIC_PENETRATION), 0f)
                .withFlatPenetration(values.value(TowerStat.FLAT_PENETRATION));
        if (capabilities.contains(TowerCapability.GUARANTEED_CRIT)) {
            attack = attack.withGuaranteedCrit();
        }
        if (capabilities.contains(TowerCapability.CRITS_PIERCE_SHIELDS)) {
            attack = attack.withCritsPierceShields();
        }
        return new TowerStats(values, rangeReal, rangeReal * rangeReal, attack, disruption,
                undisrupted.value(TowerStat.RANGE) * scale, received, capabilities);
    }

    public boolean has(TowerCapability capability) {
        return this.capabilities.contains(capability);
    }

    public float value(TowerStat stat) {
        return this.values.value(stat);
    }

    /** Damage per hit, in hundredths. */
    public int damage() {
        return Math.round(this.value(TowerStat.DAMAGE));
    }

    /** Range in cells. */
    public float range() {
        return this.value(TowerStat.RANGE);
    }

    /** How many times its base rate the tower attacks; {@code 1} is the base. */
    public double attackRate() {
        return 1.0 + this.value(TowerStat.ATTACK_SPEED);
    }

    /** Chance in {@code [0, 1]} that a hit is critical, before the target's own stats. */
    public float critChance() {
        return this.value(TowerStat.CRIT_CHANCE);
    }

    /** The share of a kill's bounty paid on top in credits. */
    public float bountyBonus() {
        return this.value(TowerStat.BOUNTY);
    }

    /** The factor on how long the effects it puts on enemies last. */
    public float effectLength() {
        return this.value(TowerStat.EFFECT_DURATION);
    }

    /** The factor on how long the tower's own timed buffs last. */
    public float timedBuffLength() {
        return this.value(TowerStat.BUFF_DURATION);
    }

    /** Ticks between the hits that apply Vulnerable; {@code 0} for none. */
    public int witherTicks() {
        return Math.round(this.value(TowerStat.WITHER_INTERVAL));
    }
}
