package td.effect;

import td.damage.DamageUnits;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Every kind of effect, each one row: its {@link EffectRule}. */
public enum EffectKind {
    CHILL(EffectRule.of(EffectCategory.SOFT_CC).resistedBy(EnemyStat.CHILL_RESIST).asALevel().pacedBySpirit()
            .tagged(EffectTag.SHARED_BY_SYMPATHY)),
    BURN(EffectRule.of(EffectCategory.DAMAGE_OVER_TIME).resistedBy(EnemyStat.BURN_RESIST).asAPool()
            .tagged(EffectTag.BURNING)),
    FREEZE(EffectRule.of(EffectCategory.HARD_CC).resistedBy(EnemyStat.FREEZE_RESIST)
            .perStack(EnemyStat.MOVE_SPEED, StatModifier.setTo(0f)).perStack(EnemyStat.AGILITY, StatModifier.setTo(0f))
            .tagged(EffectTag.STOPS)),
    SHIELD(EffectRule.of(EffectCategory.RESTORATIVE)),
    INVISIBLE(EffectRule.of(EffectCategory.STEALTH).perStack(EnemyStat.STEALTH, StatModifier.setTo(1f))),
    HEAL(EffectRule.of(EffectCategory.RESTORATIVE)),
    VULNERABLE(EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(3).pacedBySpirit()
            .perStack(EnemyStat.PHYSICAL_DAMAGE_TAKEN, 0.15f).perStack(EnemyStat.MAGIC_DAMAGE_TAKEN, 0.15f)
            .tagged(EffectTag.SPREADS_WITH_CURSE, EffectTag.SHARED_BY_SYMPATHY)),
    REVEALED(EffectRule.of(EffectCategory.STEALTH).pacedBySpirit()
            .perStack(EnemyStat.STEALTH, StatModifier.setTo(0f))),
    POISON(EffectRule.of(EffectCategory.DAMAGE_OVER_TIME).asAPool().tagged(EffectTag.SPREADS_WITH_CURSE)),
    /** The lasting mark of burning: each stack lowers resilience by one. */
    SCORCHED(EffectRule.of(EffectCategory.DEBUFF).earnedUpTo(100).pacedBySpiritUnfloored()
            .perStack(EnemyStat.RESILIENCE, -1f).tagged(EffectTag.SPREADS_WITH_CURSE)),
    /** The lasting mark of poison: each stack lowers spirit by one. */
    SICKENED(EffectRule.of(EffectCategory.DEBUFF).earnedUpTo(100).pacedBySpiritUnfloored()
            .perStack(EnemyStat.SPIRIT, -1f).tagged(EffectTag.SPREADS_WITH_CURSE)),
    /** Each stack lowers armor by five. */
    SUNDERED(EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(10).pacedBySpirit()
            .perStack(EnemyStat.ARMOR, -5f).tagged(EffectTag.SPREADS_WITH_CURSE, EffectTag.SHARED_BY_SYMPATHY)),
    /** Takes fifty resilience: crits come more often and hit harder. */
    EXPOSED(EffectRule.of(EffectCategory.SPOTTED).pacedBySpirit().perStack(EnemyStat.RESILIENCE, -50f)
            .tagged(EffectTag.SPREADS_WITH_CURSE, EffectTag.SHARED_BY_SYMPATHY)),
    /** The next hit lands as a guaranteed crit and spends the mark. */
    MARKED(EffectRule.of(EffectCategory.SPOTTED).pacedBySpirit()),
    /** Takes extra damage from every tower, and single-target towers aim at it. */
    PRIORITY(EffectRule.of(EffectCategory.SPOTTED).pacedBySpirit()
            .perStack(EnemyStat.PHYSICAL_DAMAGE_TAKEN, 0.15f).perStack(EnemyStat.MAGIC_DAMAGE_TAKEN, 0.15f)),
    /** Each stack raises the magic damage taken. */
    RESONATING(EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(3).pacedBySpirit()
            .perStack(EnemyStat.MAGIC_DAMAGE_TAKEN, 0.08f).tagged(EffectTag.SPREADS_WITH_CURSE)),
    /** Each stack lowers resilience by ten, and wears off one stack at a time. */
    FRACTURED(EffectRule.of(EffectCategory.DEBUFF).earnedUpTo(5).pacedBySpirit()
            .perStack(EnemyStat.RESILIENCE, -10f).tagged(EffectTag.SPREADS_WITH_CURSE)),
    /** Stops the enemy and its abilities, like a freeze, but keeps its burn and its chill. */
    DAZED(EffectRule.of(EffectCategory.HARD_CC).perStack(EnemyStat.MOVE_SPEED, StatModifier.setTo(0f))
            .perStack(EnemyStat.AGILITY, StatModifier.setTo(0f)).tagged(EffectTag.STOPS)),
    /** Each stack makes the Splash's blasts hit it harder; it is the Splash's own rhythm, not a stat. */
    SATURATED(EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(3)),
    /** The next hit from another tower discharges it for extra magic, credited to the tower that charged it. */
    CHARGED(EffectRule.of(EffectCategory.SPOTTED).pacedBySpirit()),
    /** When it ends, the target takes a share of all the damage it took while it lasted. */
    DOOM(EffectRule.of(EffectCategory.HEX).pacedBySpirit()),
    /** The target is poisoned for as long as it lasts. */
    BLIGHT(EffectRule.of(EffectCategory.HEX).pacedBySpirit()),
    /** When the target dies, its hexes and debuffs jump to the enemies nearest it. */
    CONTAGION(EffectRule.of(EffectCategory.HEX).pacedBySpirit()),
    /** Freezing the target buys twice the chill's extra time, and lands its burn at once instead of putting it out. */
    RIME(EffectRule.of(EffectCategory.HEX).pacedBySpirit()),
    /** The target's burn and poison hold twice as much and mark it twice as fast; it can't be frozen and shrugs off chill. */
    ASH(EffectRule.of(EffectCategory.HEX).pacedBySpirit()),
    /** Heals and shields it receives are dealt to it as damage instead, and it can't turn invisible. */
    INVERSION(EffectRule.of(EffectCategory.HEX).pacedBySpirit()),
    /** Once a second its Vulnerable, Sundered, Exposed and chill are copied to the Hexer's other hexed enemies nearby. */
    SYMPATHY(EffectRule.of(EffectCategory.HEX).pacedBySpirit()),
    /** When the target dies, the Hexer's Dooms near it release at once and start again. */
    RECKONING(EffectRule.of(EffectCategory.HEX).pacedBySpirit()),
    /** Its abilities don't fire; death abilities and auras still do. */
    SILENCED(EffectRule.of(EffectCategory.SOFT_CC).tagged(EffectTag.FIELD)),
    /** Can't be sped up, and its speed is held to three quarters of its base. */
    ANCHORED(EffectRule.of(EffectCategory.SOFT_CC).perStack(EnemyStat.MOVE_SPEED, StatModifier.capAtShareOfBase(0.75f))
            .tagged(EffectTag.FIELD)),
    /** Each stack lowers magic resist by ten. */
    UNRAVELED(EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(5).pacedBySpirit()
            .perStack(EnemyStat.MAGIC_RESIST, -10f).tagged(EffectTag.SPREADS_WITH_CURSE, EffectTag.SHARED_BY_SYMPATHY)),
    /** A frozen enemy takes extra physical damage. */
    BRITTLE(EffectRule.of(EffectCategory.DEBUFF).pacedBySpirit().perStack(EnemyStat.PHYSICAL_DAMAGE_TAKEN, 0.3f)),
    /** Each stack makes every other debuff wear off a tenth slower; it fades soon after the enemy leaves the field. */
    TOLL(EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(5)),
    /** Lowers armor by thirty while the enemy is in the field. */
    CORRODED(EffectRule.of(EffectCategory.DEBUFF).perStack(EnemyStat.ARMOR, -30f).tagged(EffectTag.FIELD)),
    /** Chilled a quarter that doesn't fade while the enemy is in the field, and counts as a chill for a freeze. */
    UNDERTOW(EffectRule.of(EffectCategory.SOFT_CC).perStack(EnemyStat.MOVE_SPEED, StatModifier.times(0.75f))
            .tagged(EffectTag.FIELD)),
    /** Heals and shields can't take hold while the enemy is in the field. */
    DEAD_ZONE(EffectRule.of(EffectCategory.DEBUFF).tagged(EffectTag.FIELD)),
    /** Heals can't take hold while the enemy is in the field; a shield still can. */
    FALLOUT(EffectRule.of(EffectCategory.DEBUFF)),
    /** Takes extra damage from every source while the enemy is in the field. */
    KILL_ZONE(EffectRule.of(EffectCategory.DEBUFF)
            .perStack(EnemyStat.PHYSICAL_DAMAGE_TAKEN, 0.25f).perStack(EnemyStat.MAGIC_DAMAGE_TAKEN, 0.25f)
            .tagged(EffectTag.FIELD)),
    /** Takes five points of plating and five of insulation. */
    CRACKED(EffectRule.of(EffectCategory.DEBUFF).pacedBySpirit()
            .perStack(EnemyStat.PLATING, -DamageUnits.ofPoints(5f))
            .perStack(EnemyStat.INSULATION, -DamageUnits.ofPoints(5f))),
    /** Stuck in tar: slowed by 40%, a burn it catches starts at double the pool, and a freeze it suffers lasts a second longer. */
    TARRED(EffectRule.of(EffectCategory.DEBUFF).perStack(EnemyStat.MOVE_SPEED, StatModifier.times(0.6f))),
    /** Takes physical damage for every cell it travels; a stopped enemy takes none. */
    BLEEDING(EffectRule.of(EffectCategory.DAMAGE_OVER_TIME)),
    /** A third fuel pool: it stacks with burn and poison, burns what is immune to fire and marks resilience and spirit. */
    SOULFIRE(EffectRule.of(EffectCategory.DAMAGE_OVER_TIME).asAPool().tagged(EffectTag.BURNING)),
    /** A short burst of speed an enemy gives itself. */
    SPRINTING(EffectRule.of(EffectCategory.BUFF).perStack(EnemyStat.MOVE_SPEED, StatModifier.percent(0.6f)));

    /** The most stacks any stacking kind reaches, so a cap an application raises (Fault Line) still has a bundle. */
    private static final int MOST_STACKS = 100;
    private static final List<EffectKind> STOPPING = Arrays.stream(values()).filter(EffectKind::stopsEnemy).toList();

    private final EffectRule rule;
    private final List<StatModifiers<EnemyStat>> modifiersByStacks;

    EffectKind(EffectRule rule) {
        this.rule = rule;
        boolean stacks = rule.stacking() == EffectStacking.SHARED_CLOCK || rule.stacking() == EffectStacking.EARNED;
        int most = stacks ? Math.max(rule.stackCap(), MOST_STACKS) : 1;
        List<StatModifiers<EnemyStat>> bundles = new ArrayList<>();
        for (int n = 0; n <= most; n++) {
            bundles.add(rule.perStack().times(n));
        }
        this.modifiersByStacks = List.copyOf(bundles);
    }

    public EffectRule rule() {
        return this.rule;
    }

    public EffectCategory category() {
        return this.rule.category();
    }

    /** Every kind that {@link #stopsEnemy() stops an enemy}. */
    public static List<EffectKind> stopping() {
        return STOPPING;
    }

    /** What {@code stacks} stacks of this kind do to the enemy's stats; precomputed, so nothing allocates. */
    public StatModifiers<EnemyStat> modifiersAt(int stacks) {
        return this.modifiersByStacks.get(Math.max(0, Math.min(this.modifiersByStacks.size() - 1, stacks)));
    }

    /** Whether this kind stops the enemy where it stands and keeps it from casting. */
    public boolean stopsEnemy() {
        return this.rule.tags().contains(EffectTag.STOPS);
    }

    /**
     * Whether this kind is a decaying level rather than a countdown: it ends when the level runs
     * out, and has no time left to show.
     */
    public boolean isDecaying() {
        return switch (this.rule.stacking()) {
            case LEVEL, POOL, EARNED -> true;
            case REFRESH, SHARED_CLOCK -> false;
        };
    }

    /**
     * Whether this kind is a fuel pool: damage that decays exponentially, and a debuff that grows a
     * stack every {@code Effect.STACK_INTERVAL_TICKS} while it lasts.
     */
    public boolean isFuelPool() {
        return this.rule.stacking() == EffectStacking.POOL;
    }

    /** Whether this kind is a fire: the pools a freeze puts out and a chill cools. */
    public boolean isBurning() {
        return this.rule.tags().contains(EffectTag.BURNING);
    }

    /**
     * Whether this kind is a count of stacks that wear off one at a time, at a pace the enemy's
     * spirit sets. A pool earns {@code SCORCHED} and {@code SICKENED}; a tower applies
     * {@code FRACTURED}.
     */
    public boolean isStackDebuff() {
        return this.rule.stacking() == EffectStacking.EARNED;
    }

    /** The most stacks a stacking kind holds; 0 for a kind that does not stack. */
    public int maxStacks() {
        return switch (this.rule.stacking()) {
            case SHARED_CLOCK, EARNED -> this.rule.stackCap();
            case REFRESH, LEVEL, POOL -> 0;
        };
    }

    /**
     * Whether the enemy's spirit sets how fast this kind's timer runs. Stack debuffs have their own
     * pace, a pool's damage keeps its curve, and an enemy's own invisibility, shield and heal are
     * not debuffs.
     */
    public boolean isPacedBySpirit() {
        return this.rule.pacing() == EffectPacing.SPIRIT && this.rule.stacking() != EffectStacking.EARNED;
    }

    /**
     * Whether a curse carries this kind on to the next enemy when its carrier dies: the debuffs
     * that weaken an enemy, not the marks a tower leaves for itself.
     */
    public boolean spreadsWithCurse() {
        return this.rule.tags().contains(EffectTag.SPREADS_WITH_CURSE);
    }

    /** Whether a Sympathy carrier shares this kind with the hexed enemies near it. */
    public boolean sharedBySympathy() {
        return this.rule.tags().contains(EffectTag.SHARED_BY_SYMPATHY);
    }

    /** Whether a tower's field holds this kind on the enemies inside it, refreshing it every tick. */
    public boolean isFieldEffect() {
        return this.rule.tags().contains(EffectTag.FIELD);
    }

    /** The resistance that shortens this kind; empty for kinds an enemy cannot resist. */
    public Optional<EnemyStat> resistedBy() {
        return this.rule.resistedBy();
    }
}
