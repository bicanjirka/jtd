package td.effect;

import td.damage.DamageUnits;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.Side;
import td.term.Term;
import td.term.TermText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Every kind of effect, each one row: its {@link EffectRule}. */
public enum EffectKind implements Term {
    CHILL("Chill", EffectRule.of(EffectCategory.SOFT_CC)
            .tagged(EffectTag.SHARED_BY_SYMPATHY)),
    BURN("Burn", EffectRule.of(EffectCategory.DAMAGE_OVER_TIME).asAPool()
            .perStack(EnemyStat.STEALTH, StatModifier.setTo(0f)).tagged(EffectTag.BURNING)),
    FREEZE("Freeze", EffectRule.of(EffectCategory.HARD_CC)
            .perStack(EnemyStat.MOVE_SPEED, StatModifier.setTo(0f)).perStack(EnemyStat.AGILITY, StatModifier.setTo(-100f))
            .tagged(EffectTag.STOPS, EffectTag.NOT_RENEWED)),
    SHIELD("Shield", EffectRule.of(EffectCategory.RESTORATIVE).perSource().helpsCarrier()),
    INVISIBLE("Invisibility", EffectRule.of(EffectCategory.STEALTH).perStack(EnemyStat.STEALTH, StatModifier.setTo(1f)).helpsCarrier()),
    HEAL("Heal", EffectRule.of(EffectCategory.RESTORATIVE).perSource().helpsCarrier()),
    VULNERABLE("Vulnerable", EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(3)
            .perStack(EnemyStat.PHYSICAL_DAMAGE_TAKEN, 0.15f).perStack(EnemyStat.MAGIC_DAMAGE_TAKEN, 0.15f)
            .tagged(EffectTag.SPREADS_WITH_CURSE, EffectTag.SHARED_BY_SYMPATHY)),
    REVEALED("Reveal", EffectRule.of(EffectCategory.STEALTH)
            .perStack(EnemyStat.STEALTH, StatModifier.setTo(0f))),
    POISON("Poison", EffectRule.of(EffectCategory.DAMAGE_OVER_TIME).asAPool().tagged(EffectTag.SPREADS_WITH_CURSE)),
    /** The lasting mark of burning: each stack lowers resilience by one. */
    SCORCHED("Scorch", EffectRule.of(EffectCategory.DEBUFF).earnedUpTo(100)
            .perStack(EnemyStat.RESILIENCE, -1f).tagged(EffectTag.SPREADS_WITH_CURSE)),
    /** The lasting mark of poison: each stack lowers spirit by one. */
    SICKENED("Sicken", EffectRule.of(EffectCategory.DEBUFF).earnedUpTo(100)
            .perStack(EnemyStat.SPIRIT, -1f).tagged(EffectTag.SPREADS_WITH_CURSE)),
    /** Each stack lowers armor by five. */
    SUNDERED("Sunder", EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(10)
            .perStack(EnemyStat.ARMOR, -5f).tagged(EffectTag.SPREADS_WITH_CURSE, EffectTag.SHARED_BY_SYMPATHY)),
    /** Takes fifty agility and fifty resilience: crits come more often and hit harder. */
    EXPOSED("Expose", EffectRule.of(EffectCategory.SPOTTED).perStack(EnemyStat.AGILITY, -50f).perStack(EnemyStat.RESILIENCE, -50f)
            .tagged(EffectTag.SPREADS_WITH_CURSE, EffectTag.SHARED_BY_SYMPATHY)),
    /** The next hit lands as a guaranteed crit and spends the mark. */
    MARKED("Mark", EffectRule.of(EffectCategory.SPOTTED)),
    /** Takes extra damage from every tower, and single-target towers aim at it. */
    PRIORITY("Priority", EffectRule.of(EffectCategory.SPOTTED)
            .perStack(EnemyStat.PHYSICAL_DAMAGE_TAKEN, 0.15f).perStack(EnemyStat.MAGIC_DAMAGE_TAKEN, 0.15f)),
    /** Each stack raises the magic damage taken. */
    RESONATING("Resonance", EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(3)
            .perStack(EnemyStat.MAGIC_DAMAGE_TAKEN, 0.08f).tagged(EffectTag.SPREADS_WITH_CURSE)),
    /** Each stack lowers resilience by ten, and wears off one stack at a time. */
    FRACTURED("Fracture", EffectRule.of(EffectCategory.DEBUFF).earnedUpTo(5)
            .perStack(EnemyStat.RESILIENCE, -10f).tagged(EffectTag.SPREADS_WITH_CURSE)),
    /** Stops the enemy and its abilities, like a freeze. */
    DAZED("Daze", EffectRule.of(EffectCategory.HARD_CC).perStack(EnemyStat.MOVE_SPEED, StatModifier.setTo(0f))
            .perStack(EnemyStat.AGILITY, StatModifier.setTo(-100f)).tagged(EffectTag.STOPS)),
    /** Each stack makes the Splash's blasts hit it harder; it is the Splash's own rhythm, not a stat. */
    SATURATED("Saturation", EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(3)),
    /** The next hit from another tower discharges it for extra magic, credited to the tower that charged it. */
    CHARGED("Charge", EffectRule.of(EffectCategory.SPOTTED)),
    /** When it ends, the target takes a share of all the damage it took while it lasted. */
    DOOM("Doom", EffectRule.of(EffectCategory.HEX)),
    /** The target is poisoned for as long as it lasts. */
    BLIGHT("Blight", EffectRule.of(EffectCategory.HEX)),
    /** When the target dies, its hexes and debuffs jump to the enemies nearest it. */
    CONTAGION("Contagion", EffectRule.of(EffectCategory.HEX)),
    /** Lowers tenacity: what weakens the target lasts longer on it. */
    MALAISE("Malaise", EffectRule.of(EffectCategory.HEX).perStack(EnemyStat.TENACITY, -50f)),
    /** Heals and shields it receives are dealt to it as damage instead. */
    INVERSION("Inversion", EffectRule.of(EffectCategory.HEX)),
    /** Once a second its Vulnerable, Sundered, Exposed and chill are copied to the Hexer's other hexed enemies nearby. */
    SYMPATHY("Sympathy", EffectRule.of(EffectCategory.HEX)),
    /** When the target dies, the Hexer's Dooms near it release at once and start again. */
    RECKONING("Reckoning", EffectRule.of(EffectCategory.HEX)),
    /** Its abilities don't fire; death abilities and auras still do. */
    SILENCED("Silence", EffectRule.of(EffectCategory.SOFT_CC)),
    /** Its speed is held to a share of its base, whatever raises it. */
    ANCHORED("Anchor", EffectRule.of(EffectCategory.SOFT_CC)
            .slowingBy(0.25f).tagged(EffectTag.SPEED_CEILING)),
    /** Each stack lowers magic resist by ten. */
    UNRAVELED("Unravel", EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(5)
            .perStack(EnemyStat.MAGIC_RESIST, -10f).tagged(EffectTag.SPREADS_WITH_CURSE, EffectTag.SHARED_BY_SYMPATHY)),
    /** Takes extra physical damage. */
    BRITTLE("Brittle", EffectRule.of(EffectCategory.DEBUFF).perStack(EnemyStat.PHYSICAL_DAMAGE_TAKEN, 0.3f)),
    /** Time spent in the field: it sets how strongly the field's effects hold, and fades soon after the enemy leaves. */
    TOLL("Toll", EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(5)),
    /**
     * Lowers armor and magic resist while the enemy is in the field and a while after, three a stack: thirty each at the
     * field's full strength.
     */
    CORRODED("Corrode", EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(FieldStrength.MOST)
            .perStack(EnemyStat.ARMOR, -3f).perStack(EnemyStat.MAGIC_RESIST, -3f)),
    /** The field's slow, a point of speed in a hundred a stack. */
    DRAGGED("Drag", EffectRule.of(EffectCategory.SOFT_CC).stackingOnASharedClock(DragStrength.MOST)
            .slowingBy(0.01f)),
    /** Arc Discharge's mark: each stack adds 5% damage taken from every source, up to four. */
    CONDEMNED("Condemn", EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(4)
            .perStack(EnemyStat.PHYSICAL_DAMAGE_TAKEN, 0.05f).perStack(EnemyStat.MAGIC_DAMAGE_TAKEN, 0.05f)),
    /** While the enemy is Dazed, the field that stilled it takes its Toll away. */
    STILLED("Stasis", EffectRule.of(EffectCategory.DEBUFF)),
    /** Spirit at its lowest while it lasts, so heals, shields and rewinds are worth nothing. */
    DEAD_ZONE("Dead Zone", EffectRule.of(EffectCategory.DEBUFF)
            .perStack(EnemyStat.SPIRIT, StatModifier.setTo(-100f))),
    /** Takes extra damage from every source while the enemy is in the field and a while after: 20% at full strength. */
    FRAIL("Frail", EffectRule.of(EffectCategory.DEBUFF).stackingOnASharedClock(FieldStrength.MOST)
            .perStack(EnemyStat.PHYSICAL_DAMAGE_TAKEN, 0.02f).perStack(EnemyStat.MAGIC_DAMAGE_TAKEN, 0.02f)),
    /** Takes five points of plating and five of insulation. */
    CRACKED("Crack", EffectRule.of(EffectCategory.DEBUFF)
            .perStack(EnemyStat.PLATING, -DamageUnits.ofPoints(5f))
            .perStack(EnemyStat.INSULATION, -DamageUnits.ofPoints(5f))),
    /** Stuck in tar: a plain slow. */
    TARRED("Tar", EffectRule.of(EffectCategory.DEBUFF).slowingBy(0.4f)),
    /** Takes physical damage for every cell it travels; a stopped enemy takes none. */
    BLEEDING("Bleed", EffectRule.of(EffectCategory.DAMAGE_OVER_TIME)),
    /** A third fuel pool: it stacks with burn and poison, burns what is immune to fire and marks resilience and spirit. */
    SOULFIRE("Soulfire", EffectRule.of(EffectCategory.DAMAGE_OVER_TIME).asAPool()
            .perStack(EnemyStat.STEALTH, StatModifier.setTo(0f)).tagged(EffectTag.BURNING)),
    /** A short burst of speed an enemy gives itself. */
    SPRINTING("Sprint", EffectRule.of(EffectCategory.BUFF).perStack(EnemyStat.MOVE_SPEED, StatModifier.percent(0.6f)).helpsCarrier()),
    /** Twice the speed; a freeze or daze still stops it. */
    HASTENED("Hasten", EffectRule.of(EffectCategory.BUFF).perStack(EnemyStat.MOVE_SPEED, StatModifier.times(2f)).helpsCarrier()),
    /** Magic resist that stacks, on one shared clock. */
    SPELLPROOF("Spellproof", EffectRule.of(EffectCategory.BUFF).stackingOnASharedClock(200)
            .perStack(EnemyStat.MAGIC_RESIST, 1f).helpsCarrier()),
    /** A last stand: far more resilience, spirit and magic resist. */
    UNYIELDING("Unyielding", EffectRule.of(EffectCategory.BUFF).perStack(EnemyStat.RESILIENCE, 100f)
            .perStack(EnemyStat.SPIRIT, 100f).perStack(EnemyStat.MAGIC_RESIST, 100f).helpsCarrier()),
    /** Less damage taken of either type, a stack a point; an application holds it at its own stacks. */
    RESOLVE("Resolve", EffectRule.of(EffectCategory.BUFF).stackingOnASharedClock(100)
            .perStack(EnemyStat.PHYSICAL_DAMAGE_TAKEN, -0.01f).perStack(EnemyStat.MAGIC_DAMAGE_TAKEN, -0.01f)
            .helpsCarrier());

    /** The most stacks any stacking kind reaches, so a cap an application raises (Fault Line) still has a bundle. */
    private static final int MOST_STACKS = 100;
    private static final List<EffectKind> STOPPING = Arrays.stream(values()).filter(EffectKind::stopsEnemy).toList();

    private final String title;
    private final EffectRule rule;
    private final List<StatModifiers<EnemyStat>> modifiersByStacks;

    EffectKind(String title, EffectRule rule) {
        this.title = title;
        this.rule = rule;
        boolean stacks = rule.stacking() == EffectStacking.SHARED_CLOCK || rule.stacking() == EffectStacking.EARNED;
        int most = stacks ? Math.max(rule.stackCap(), MOST_STACKS) : 1;
        List<StatModifiers<EnemyStat>> bundles = new ArrayList<>();
        for (int n = 0; n <= most; n++) {
            bundles.add(rule.perStack().times(n));
        }
        this.modifiersByStacks = List.copyOf(bundles);
    }

    @Override
    public String key() {
        return "effect." + this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public TermText describe() {
        return EffectSentences.of(this);
    }

    /** Whether it works for the enemy that carries it or against it. */
    public Side side() {
        return this.rule.side();
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

    /** Whether a reapplication leaves one that is active as it is. */
    public boolean isNotRenewed() {
        return this.rule.tags().contains(EffectTag.NOT_RENEWED);
    }

    /** Whether this kind's slow holds speed to a share of base instead of taking a share of what it finds. */
    public boolean isSpeedCeiling() {
        return this.rule.tags().contains(EffectTag.SPEED_CEILING);
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
            case POOL, EARNED -> true;
            case REFRESH, SHARED_CLOCK, PER_SOURCE -> false;
        };
    }

    /**
     * Whether this kind is a fuel pool: damage that decays exponentially, and a debuff that grows a
     * stack every {@code Effect.STACK_INTERVAL_TICKS} while it lasts.
     */
    public boolean isFuelPool() {
        return this.rule.stacking() == EffectStacking.POOL;
    }

    /** Whether this kind is a fire: the pools that reveal their carrier. */
    public boolean isBurning() {
        return this.rule.tags().contains(EffectTag.BURNING);
    }

    /**
     * Whether this kind is a count of stacks that wear off one at a time. A pool earns {@code SCORCHED} and {@code SICKENED}; a tower applies
     * {@code FRACTURED}.
     */
    public boolean isStackDebuff() {
        return this.rule.stacking() == EffectStacking.EARNED;
    }

    /** The most stacks a stacking kind holds; 0 for a kind that does not stack. */
    public int maxStacks() {
        return switch (this.rule.stacking()) {
            case SHARED_CLOCK, EARNED -> this.rule.stackCap();
            case REFRESH, POOL, PER_SOURCE -> 0;
        };
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
}
