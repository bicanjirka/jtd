package td.effect;

import td.damage.DamageUnits;
import td.stat.EnemyStat;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/** What each effect kind says about itself, every number read from its rule, the interaction tables or its constants. */
final class EffectSentences {

    private EffectSentences() {
    }

    /** {@code kind}'s sentence, every number read from its rule, the interaction tables or its constants. */
    static TermText of(EffectKind kind) {
        return switch (kind) {
            case CHILL -> TermText.of("slows by its level, up to " + TermNumbers.percent(ActiveEffects.MAX_CHILL)
                    + ", and fades; at its most it takes " + TermNumbers.percent(ActiveEffects.BURN_CHILL_DAMPENING)
                    + " off the damage of ").plus(kinds(EffectKind::isBurning, " and "));
            case BURN -> TermText.of("damage over time that fades; a top-up adds less the fuller it is, up to "
                    + TermNumbers.times(PoolTuning.standard().capFactor()) + " its strongest application; it adds a stack of ")
                    .plus(earned(kind)).plus(" every " + seconds(Effect.STACK_INTERVAL_TICKS) + " while it lasts, and ")
                    .plus(EffectKind.FREEZE).plus(" puts it out, landing at least "
                            + TermNumbers.percent(EffectInteractions.FREEZE_BURST_FLOOR) + " of what is left at once");
            case FREEZE -> TermText.of("stops it where it stands and keeps its abilities from firing; it removes ")
                    .plus(EffectKind.CHILL).plus(" and lasts longer by its level, and puts out ")
                    .plus(kinds(EffectKind::isBurning, " and ")).plus("; within "
                            + seconds(DiminishingReturns.WINDOW_TICKS) + ", each fresh one lasts " + ladder(kind));
            case SHIELD -> TermText.of("absorbs damage until its amount is spent or it runs out");
            case INVISIBLE -> TermText.of("towers can't aim at it, though damage on an area still reaches it");
            case HEAL -> TermText.of("restores health over time");
            case VULNERABLE -> TermText.of("each stack: "
                    + TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN))
                    + " physical and magic damage taken, up to " + kind.maxStacks() + " stacks");
            case REVEALED -> TermText.of("every tower can see and aim at it, and it counts as having ")
                    .plus(EffectKind.EXPOSED);
            case POISON -> TermText.of("damage over time that fades, and a slow of up to "
                    + TermNumbers.percent(ActiveEffects.POISON_MAX_SLOW) + " while it is full; it adds a stack of ")
                    .plus(earned(kind)).plus(" every " + seconds(Effect.STACK_INTERVAL_TICKS) + " while it lasts");
            case SCORCHED -> TermText.of("each stack: " + TermNumbers.signed(perStack(kind, EnemyStat.RESILIENCE))
                    + " resilience, up to " + kind.maxStacks() + "; once nothing adds more, it loses a stack every "
                    + seconds(ActiveEffects.STACK_DECAY_INTERVAL_TICKS) + ", sooner with more spirit");
            case SICKENED -> TermText.of("each stack: " + TermNumbers.signed(perStack(kind, EnemyStat.SPIRIT))
                    + " spirit, up to " + kind.maxStacks() + "; once nothing adds more, it loses a stack every "
                    + seconds(ActiveEffects.STACK_DECAY_INTERVAL_TICKS) + ", sooner with more spirit");
            case SUNDERED -> TermText.of("each stack: " + TermNumbers.signed(perStack(kind, EnemyStat.ARMOR))
                    + " armor, up to " + kind.maxStacks() + " stacks");
            case EXPOSED -> TermText.of(TermNumbers.signed(perStack(kind, EnemyStat.RESILIENCE))
                    + " resilience: crits find it more often and hit it harder");
            case MARKED -> TermText.of("the next hit on it is a crit, which spends this");
            case PRIORITY -> TermText.of(TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN))
                    + " physical and magic damage taken, and a tower that picks one target picks it");
            case RESONATING -> TermText.of("each stack: "
                    + TermNumbers.signedPercent(perStack(kind, EnemyStat.MAGIC_DAMAGE_TAKEN))
                    + " magic damage taken, up to " + kind.maxStacks() + " stacks");
            case FRACTURED -> TermText.of("each stack: " + TermNumbers.signed(perStack(kind, EnemyStat.RESILIENCE))
                    + " resilience, up to " + kind.maxStacks() + "; it loses a stack every "
                    + seconds(ActiveEffects.STACK_DECAY_INTERVAL_TICKS));
            case DAZED -> TermText.of("stops it and keeps its abilities from firing, like ").plus(EffectKind.FREEZE)
                    .plus(", but leaves its ").plus(EffectKind.BURN).plus(" and ").plus(EffectKind.CHILL)
                    .plus("; within " + seconds(DiminishingReturns.WINDOW_TICKS) + ", each fresh one lasts "
                            + ladder(kind));
            case SATURATED -> TermText.of("each stack makes a Burst's blasts hit it harder");
            case CHARGED -> TermText.of("the next hit on it from another tower lands "
                    + TermNumbers.percent(Effect.CHARGE_DISCHARGE_SHARE)
                    + " of that hit again as magic, twice that on a crit, for the tower that applied it");
            case DOOM -> TermText.of("when it ends, it takes a share of all the damage it took while it lasted");
            case BLIGHT -> TermText.of("it has ").plus(EffectKind.POISON).plus(" for as long as this lasts");
            case CONTAGION -> TermText.of("when it dies, its hexes and debuffs jump to the enemies nearest it");
            case RIME -> TermText.of("a ").plus(EffectKind.FREEZE).plus(" on it buys "
                    + TermNumbers.times(EffectInteractions.freezeLengthFactor(EnumSet.of(EffectKind.RIME), 1f) - 1f)
                    + " the extra time from ").plus(EffectKind.CHILL).plus(", and lands all of its ")
                    .plus(kinds(EffectKind::isBurning, " or ")).plus(" at once instead of putting it out; ")
                    .plus(EffectKind.ASH).plus(" replaces it");
            case ASH -> TermText.of("its ").plus(kinds(EffectKind::isFuelPool, " and ")).plus(" hold "
                    + TermNumbers.times(EffectInteractions.poolFactor(EnumSet.of(EffectKind.ASH)))
                    + " as much and add their stacks "
                    + TermNumbers.times(EffectInteractions.poolFactor(EnumSet.of(EffectKind.ASH)))
                    + " as fast; it takes only "
                    + TermNumbers.percent(EffectInteractions.chillTaken(EnumSet.of(EffectKind.ASH))) + " of a ")
                    .plus(EffectKind.CHILL).plus(", and ").plus(EffectKind.FREEZE)
                    .plus(" can't take hold; ").plus(EffectKind.RIME).plus(" replaces it");
            case INVERSION -> TermText.of("a ").plus(EffectKind.HEAL).plus(" or ").plus(EffectKind.SHIELD)
                    .plus(" it receives is dealt to it as damage instead, and it can't gain ")
                    .plus(EffectKind.INVISIBLE);
            case SYMPATHY -> TermText.of("once a second, its ")
                    .plus(kinds(EffectKind::sharedBySympathy, " and "))
                    .plus(" are copied, up to what it has, to the other enemies nearby that carry a hex from the same tower");
            case RECKONING -> TermText.of("when it dies, each ").plus(EffectKind.DOOM)
                    .plus(" from the same tower near it releases at once and starts again");
            case SILENCED -> TermText.of("its abilities don't fire; abilities on death and auras still do");
            case ANCHORED -> TermText.of("it can't be sped up, and its speed is held to "
                    + TermNumbers.percent(kind.rule().perStack().get(EnemyStat.MOVE_SPEED).capShareOfBase())
                    + " of its base");
            case UNRAVELED -> TermText.of("each stack: " + TermNumbers.signed(perStack(kind, EnemyStat.MAGIC_RESIST))
                    + " magic resist, up to " + kind.maxStacks() + " stacks");
            case BRITTLE -> TermText.of("while it has ").plus(EffectKind.FREEZE).plus(": "
                    + TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN))
                    + " physical damage taken");
            case TOLL -> TermText.of("time spent in an Obelisk's field, a stack a second; it sets how strongly the "
                    + "field holds it, and goes soon after it leaves");
            case CORRODED -> TermText.of("each stack: " + TermNumbers.signed(perStack(kind, EnemyStat.ARMOR))
                    + " armor and magic resist, up to " + kind.maxStacks() + " stacks");
            case DRAGGED -> TermText.of("each stack takes "
                    + TermNumbers.percent(-kind.rule().perStack().get(EnemyStat.MOVE_SPEED).percentAdd())
                    + " of its speed, up to " + kind.maxStacks() + " stacks; it is no ").plus(EffectKind.CHILL)
                    .plus(", and nothing resists it");
            case UNDERTOW -> TermText.of("a ").plus(EffectKind.FREEZE).plus(" counts it as "
                    + TermNumbers.percent(EffectInteractions.freezeLengthFactor(EnumSet.of(EffectKind.UNDERTOW), 0f) - 1f)
                    + " ").plus(EffectKind.CHILL);
            case CONDEMNED -> TermText.of("each stack: "
                    + TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN))
                    + " physical and magic damage taken, up to " + kind.maxStacks() + " stacks");
            case STILLED -> TermText.of("while it also has ").plus(EffectKind.DAZED)
                    .plus(", the field holding it takes its ").plus(EffectKind.TOLL).plus(" away");
            case UNVEILED -> TermText.of("it can't gain ").plus(EffectKind.INVISIBLE);
            case DEAD_ZONE -> TermText.of(EffectKind.HEAL).plus(" and ").plus(EffectKind.SHIELD)
                    .plus(" can't take hold");
            case FALLOUT -> TermText.of(EffectKind.HEAL).plus(" can't take hold; ").plus(EffectKind.SHIELD)
                    .plus(" still can");
            case FRAIL -> TermText.of("each stack: "
                    + TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN))
                    + " physical and magic damage taken, up to " + kind.maxStacks() + " stacks");
            case CRACKED -> TermText.of(TermNumbers.signed(DamageUnits.inPoints(perStack(kind, EnemyStat.PLATING)))
                    + " plating and " + TermNumbers.signed(DamageUnits.inPoints(perStack(kind, EnemyStat.INSULATION)))
                    + " insulation");
            case TARRED -> TermText.of(
                    TermNumbers.signedPercent(kind.rule().perStack().get(EnemyStat.MOVE_SPEED).multiply() - 1f)
                            + " speed; a ").plus(kinds(EffectKind::isBurning, " or ")).plus(" it catches starts at "
                    + TermNumbers.times(EffectInteractions.fireStartFactor(EnumSet.of(EffectKind.TARRED))) + ", and a ")
                    .plus(EffectKind.FREEZE).plus(" lasts "
                            + seconds(EffectInteractions.freezeExtraTicks(EnumSet.of(EffectKind.TARRED))) + " longer");
            case BLEEDING -> TermText.of("physical damage for every cell it travels; standing still, it takes none");
            case SOULFIRE -> TermText.of("damage over time in a pool of its own beside ").plus(EffectKind.BURN)
                    .plus(" and ").plus(EffectKind.POISON).plus("; nothing resists it, it adds a stack of ")
                    .plus(earned(kind)).plus(" every " + seconds(Effect.STACK_INTERVAL_TICKS)
                            + " while it lasts, and ").plus(EffectKind.FREEZE).plus(" puts it out");
            case SPRINTING -> TermText.of(
                    TermNumbers.signedPercent(kind.rule().perStack().get(EnemyStat.MOVE_SPEED).percentAdd())
                            + " speed");
        };
    }

    /** The flat per-stack modifier {@code kind} puts on {@code stat}. */
    private static float perStack(EffectKind kind, EnemyStat stat) {
        return kind.rule().perStack().get(stat).flat();
    }

    private static String seconds(int ticks) {
        return TermNumbers.seconds(ticks / TickRate.TICKS_PER_SECOND);
    }

    /** "100%, 50%, 25%, then none" from a diminishing-returns ladder. */
    private static String ladder(EffectKind kind) {
        List<String> steps = DiminishingReturns.stepsOf(kind).stream().filter(step -> step > 0f)
                .map(TermNumbers::percent).toList();
        return String.join(", ", steps) + ", then none";
    }

    private static TermText earned(EffectKind pool) {
        return list(EffectInteractions.earnedBy(pool), " and ");
    }

    private static TermText kinds(Predicate<EffectKind> filter, String last) {
        return list(Arrays.stream(EffectKind.values()).filter(filter).toList(), last);
    }

    /** "A", "A and B", "A, B and C": mentions with {@code last} before the final one. */
    private static TermText list(List<EffectKind> kinds, String last) {
        TermText text = TermText.empty();
        for (int i = 0; i < kinds.size(); i++) {
            String separator = i == 0 ? "" : i == kinds.size() - 1 ? last : ", ";
            text = text.plus(separator).plus(kinds.get(i));
        }
        return text;
    }
}
