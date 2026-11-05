package td.effect;

import td.damage.DamageUnits;
import td.stat.EnemyStat;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/** What each effect kind says about itself, every number read from its rule, the interaction tables or its constants. */
final class EffectSentences {

    private EffectSentences() {
    }

    /** {@code kind}'s sentence, every number read from its rule, the interaction tables or its constants. */
    static TermText of(EffectKind kind) {
        return switch (kind) {
            case CHILL -> slowing("slows by what its source gives");
            case BURN -> TermText.of("damage over time that fades; a top-up adds less the fuller it is, up to "
                    + TermNumbers.times(PoolTuning.standard().capFactor()) + " its strongest application; it adds a stack of ")
                    .plus(earned(kind)).plus(" every " + seconds(Effect.STACK_INTERVAL_TICKS)
                            + " while it lasts, and every tower can see and aim at it");
            case FREEZE -> TermText.of("stops it where it stands and keeps its abilities from firing; ")
                    .plus(stoppedAgility(kind)).plus("; time under it shortens the next one, and walking wins that back, "
                            + "more slowly at higher rank");
            case SHIELD -> TermText.of("takes a hit before armor does, until its amount is spent or it runs out; more ")
                    .plus(EnemyStat.SPIRIT).plus(" makes it hold more, shields from different sources add up, and damage that"
                            + " ticks goes past it");
            case INVISIBLE -> TermText.of("towers can't aim at it, though damage on an area still reaches it");
            case HEAL -> TermText.of("adds to its ").plus(EnemyStat.REGENERATION)
                    .plus(" while it lasts; heals from different sources add up");
            case VULNERABLE -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN)),
                            EnemyStat.PHYSICAL_DAMAGE_TAKEN, EnemyStat.MAGIC_DAMAGE_TAKEN))
                    .plus(", up to " + kind.maxStacks() + " stacks");
            case REVEALED -> TermText.of("every tower can see and aim at it, and it can't gain ")
                    .plus(EffectKind.INVISIBLE);
            case POISON -> TermText.of("damage over time that fades; it adds a stack of ")
                    .plus(earned(kind)).plus(" every " + seconds(Effect.STACK_INTERVAL_TICKS) + " while it lasts");
            case SCORCHED -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signed(perStack(kind, EnemyStat.RESILIENCE)), EnemyStat.RESILIENCE))
                    .plus(", up to " + kind.maxStacks() + "; once nothing adds more, it loses a stack every "
                            + seconds(ActiveEffects.STACK_DECAY_INTERVAL_TICKS));
            case SICKENED -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signed(perStack(kind, EnemyStat.SPIRIT)), EnemyStat.SPIRIT))
                    .plus(", up to " + kind.maxStacks() + "; once nothing adds more, it loses a stack every "
                            + seconds(ActiveEffects.STACK_DECAY_INTERVAL_TICKS));
            case SUNDERED -> stacked(kind, EnemyStat.ARMOR);
            case EXPOSED -> changes(TermNumbers.signed(perStack(kind, EnemyStat.RESILIENCE)), EnemyStat.AGILITY, EnemyStat.RESILIENCE)
                    .plus(": crits find it more often and hit it harder");
            case MARKED -> TermText.of("the next hit on it is a crit, which spends this");
            case PRIORITY -> changes(TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN)),
                    EnemyStat.PHYSICAL_DAMAGE_TAKEN, EnemyStat.MAGIC_DAMAGE_TAKEN)
                    .plus(", and a tower that picks one target picks it");
            case RESONATING -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signedPercent(perStack(kind, EnemyStat.MAGIC_DAMAGE_TAKEN)),
                            EnemyStat.MAGIC_DAMAGE_TAKEN))
                    .plus(", up to " + kind.maxStacks() + " stacks");
            case FRACTURED -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signed(perStack(kind, EnemyStat.RESILIENCE)), EnemyStat.RESILIENCE))
                    .plus(", up to " + kind.maxStacks() + "; it loses a stack every "
                            + seconds(ActiveEffects.STACK_DECAY_INTERVAL_TICKS));
            case DAZED -> TermText.of("stops it and keeps its abilities from firing, like ").plus(EffectKind.FREEZE)
                    .plus("; ").plus(stoppedAgility(kind));
            case SATURATED -> TermText.of("each stack makes a Burst's blasts hit it harder");
            case CHARGED -> TermText.of("the next hit on it from another tower lands "
                    + TermNumbers.percent(Effect.CHARGE_DISCHARGE_SHARE)
                    + " of that hit again as magic, twice that on a crit, for the tower that applied it");
            case DOOM -> TermText.of("when it ends, it takes a share of all the damage it took while it lasted");
            case BLIGHT -> TermText.of("it has ").plus(EffectKind.POISON).plus(" for as long as this lasts");
            case CONTAGION -> TermText.of("when it dies, its hexes and debuffs jump to the enemies nearest it");
            case MALAISE -> changes(TermNumbers.signed(perStack(kind, EnemyStat.TENACITY)), EnemyStat.TENACITY);
            case INVERSION -> TermText.of("its ").plus(EnemyStat.REGENERATION)
                    .plus(" deals it damage instead of restoring health, and a ").plus(EffectKind.SHIELD)
                    .plus(" it receives strikes it instead");
            case SYMPATHY -> TermText.of("once a second, its ")
                    .plus(kinds(EffectKind::sharedBySympathy, " and "))
                    .plus(" are copied, up to what it has, to the other enemies nearby that carry a hex from the same tower");
            case RECKONING -> TermText.of("when it dies, each ").plus(EffectKind.DOOM)
                    .plus(" from the same tower near it releases at once and starts again");
            case SILENCED -> TermText.of("its abilities don't fire; abilities on death and auras still do");
            case ANCHORED -> slowing("slows by " + TermNumbers.percent(kind.rule().slowPerStack()));
            case UNRAVELED -> stacked(kind, EnemyStat.MAGIC_RESIST);
            case BRITTLE -> changes(TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN)),
                    EnemyStat.PHYSICAL_DAMAGE_TAKEN);
            case TOLL -> TermText.of("time spent in an Obelisk's field, a stack a second; it sets how strongly the "
                    + "field holds it, and goes soon after it leaves");
            case CORRODED -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signed(perStack(kind, EnemyStat.ARMOR)), EnemyStat.ARMOR,
                            EnemyStat.MAGIC_RESIST))
                    .plus(", up to " + kind.maxStacks() + " stacks");
            case DRAGGED -> slowing("slows by " + TermNumbers.percent(kind.rule().slowPerStack()) + " a stack, up to "
                    + kind.maxStacks() + " stacks");
            case CONDEMNED -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN)),
                            EnemyStat.PHYSICAL_DAMAGE_TAKEN, EnemyStat.MAGIC_DAMAGE_TAKEN))
                    .plus(", up to " + kind.maxStacks() + " stacks");
            case STILLED -> TermText.of("while it also has ").plus(EffectKind.DAZED)
                    .plus(", the field holding it takes its ").plus(EffectKind.TOLL).plus(" away");
            case DEAD_ZONE -> changes(TermNumbers.signed(kind.rule().perStack().get(EnemyStat.SPIRIT).setTo()), EnemyStat.SPIRIT)
                    .plus(", so ").plus(EffectKind.HEAL).plus(" and ").plus(EffectKind.SHIELD).plus(" are worth nothing");
            case FRAIL -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN)),
                            EnemyStat.PHYSICAL_DAMAGE_TAKEN, EnemyStat.MAGIC_DAMAGE_TAKEN))
                    .plus(", up to " + kind.maxStacks() + " stacks");
            case CRACKED -> changes(TermNumbers.signed(DamageUnits.inPoints(perStack(kind, EnemyStat.PLATING))),
                    EnemyStat.PLATING)
                    .plus(" and ").plus(changes(TermNumbers.signed(DamageUnits.inPoints(perStack(kind,
                            EnemyStat.INSULATION))), EnemyStat.INSULATION));
            case TARRED -> slowing("slows by " + TermNumbers.percent(kind.rule().slowPerStack()));
            case BLEEDING -> TermText.of("physical damage for every cell it travels; standing still, it takes none");
            case SOULFIRE -> TermText.of("damage over time in a pool of its own beside ").plus(EffectKind.BURN)
                    .plus(" and ").plus(EffectKind.POISON).plus("; it adds a stack of ")
                    .plus(earned(kind)).plus(" every " + seconds(Effect.STACK_INTERVAL_TICKS)
                            + " while it lasts, and every tower can see and aim at it");
            case SPRINTING -> changes(
                    TermNumbers.signedPercent(kind.rule().perStack().get(EnemyStat.MOVE_SPEED).percentAdd()),
                    EnemyStat.MOVE_SPEED).plus("; any slow cancels it");
            case SPELLPROOF -> stacked(kind, EnemyStat.MAGIC_RESIST);
            case UNYIELDING -> TermText.join(", ", List.of(
                    changes(TermNumbers.signed(perStack(kind, EnemyStat.RESILIENCE)), EnemyStat.RESILIENCE),
                    changes(TermNumbers.signed(perStack(kind, EnemyStat.SPIRIT)), EnemyStat.SPIRIT),
                    changes(TermNumbers.signed(perStack(kind, EnemyStat.MAGIC_RESIST)), EnemyStat.MAGIC_RESIST)));
            case HASTENED -> changes(TermNumbers.times(kind.rule().perStack().get(EnemyStat.MOVE_SPEED).multiply()),
                    EnemyStat.MOVE_SPEED).plus("; any slow cancels it, and ").plus(EffectKind.FREEZE).plus(" and ")
                    .plus(EffectKind.DAZED).plus(" still stop it");
        };
    }

    /** {@code what}, then the rule every slow keeps. */
    private static TermText slowing(String what) {
        return TermText.of(what + "; the strongest slow holds, no speed gain beats it, and speed comes back over its last "
                + seconds(ActiveEffects.SLOW_FADE_TICKS));
    }

    /** "Agility -100, so it can't dodge and crits find it more often", the value read from {@code kind}'s rule. */
    private static TermText stoppedAgility(EffectKind kind) {
        return changes(TermNumbers.signed(kind.rule().perStack().get(EnemyStat.AGILITY).setTo()), EnemyStat.AGILITY)
                .plus(", so it can't dodge and crits find it more often");
    }

    /** "each stack: Armor -5, up to 3 stacks", the change read from {@code kind}'s rule. */
    private static TermText stacked(EffectKind kind, EnemyStat stat) {
        return TermText.of("each stack: ").plus(changes(TermNumbers.signed(perStack(kind, stat)), stat))
                .plus(", up to " + kind.maxStacks() + " stacks");
    }

    /** Each of {@code stats} named, then {@code change}: "Armor and Magic resist -5". */
    private static TermText changes(String change, EnemyStat... stats) {
        TermText named = TermText.empty();
        for (int i = 0; i < stats.length; i++) {
            named = named.plus(i == 0 ? "" : " and ").plus(stats[i]);
        }
        return named.plus(" " + change);
    }

    /** The flat per-stack modifier {@code kind} puts on {@code stat}. */
    private static float perStack(EffectKind kind, EnemyStat stat) {
        return kind.rule().perStack().get(stat).flat();
    }

    private static String seconds(int ticks) {
        return TermNumbers.seconds(ticks / TickRate.TICKS_PER_SECOND);
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
