package td.effect;

import td.damage.DamageUnits;
import td.stat.EnemyStat;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.List;

/** What each effect kind says about itself, every number read from its rule, the interaction tables or its constants. */
final class EffectSentences {

    private EffectSentences() {
    }

    /** {@code kind}'s sentence, every number read from its rule, the interaction tables or its constants. */
    static TermText of(EffectKind kind) {
        return switch (kind) {
            case CHILL -> slowing("slows by what its source gives");
            case IRRADIATED -> TermText.of("magic damage that halves at a steady pace as it runs, until it deals under "
                    + TermNumbers.number(DamageUnits.inPoints(EffectDynamics.Dose.SPENT_PER_TICK) * TickRate.TICKS_PER_SECOND)
                    + " a second; a second dose adds to what is left of the first; while it lasts its ")
                    .plus(EnemyStat.REGENERATION).plus(" can't rise above 0, so a ").plus(EffectKind.HEAL)
                    .plus(" restores nothing");
            case BURN -> pool().plus("; it adds a stack of ")
                    .plus(earned(kind)).plus(" every " + seconds(Effect.STACK_INTERVAL_TICKS)
                            + " while it lasts, and every tower can see and aim at it");
            case FREEZE -> TermText.of("stops it where it stands and keeps its abilities from firing; ")
                    .plus(stoppedAgility()).plus("; time under it shortens the next one, and walking wins that back, "
                            + "more slowly at higher rank");
            case SHIELD -> TermText.of("takes a hit before armor does, until its amount is spent or it runs out; more ")
                    .plus(EnemyStat.SPIRIT).plus(" makes it hold more, shields from different sources add up, and damage over"
                            + " time goes past it");
            case INVISIBLE -> TermText.of("towers can't aim at it, though damage on an area still reaches it");
            case HEAL -> TermText.of("adds to its ").plus(EnemyStat.REGENERATION)
                    .plus(" while it lasts; heals from different sources add up");
            case VULNERABLE -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN)),
                            EnemyStat.PHYSICAL_DAMAGE_TAKEN, EnemyStat.MAGIC_DAMAGE_TAKEN))
                    .plus(inShares(kind));
            case REVEALED -> TermText.of("every tower can see and aim at it, and it can't gain ")
                    .plus(EffectKind.INVISIBLE);
            case POISON -> pool().plus("; it adds a stack of ")
                    .plus(earned(kind)).plus(" every " + seconds(Effect.STACK_INTERVAL_TICKS) + " while it lasts");
            case SCORCHED -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signed(perStack(kind, EnemyStat.RESILIENCE)), EnemyStat.RESILIENCE))
                    .plus(counted(kind));
            case SICKENED -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signed(perStack(kind, EnemyStat.SPIRIT)), EnemyStat.SPIRIT))
                    .plus(counted(kind));
            case SUNDERED -> stacked(kind, EnemyStat.ARMOR).plus(counted(kind));
            case EXPOSED -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signed(perStack(kind, EnemyStat.RESILIENCE)), EnemyStat.AGILITY, EnemyStat.RESILIENCE))
                    .plus(", more and stronger crits on it").plus(inShares(kind));
            case MARKED -> TermText.of("the next hit on it is a crit, which spends this");
            case PRIORITY -> changes(TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN)),
                    EnemyStat.PHYSICAL_DAMAGE_TAKEN, EnemyStat.MAGIC_DAMAGE_TAKEN)
                    .plus(", and a tower that picks one target picks it");
            case DAZED -> TermText.of("stops it and keeps its abilities from firing, like ").plus(EffectKind.FREEZE)
                    .plus("; ").plus(stoppedAgility());
            case CHARGED -> TermText.of("the next hit on it from another tower lands "
                    + TermNumbers.percent(Effect.CHARGE_DISCHARGE_SHARE)
                    + " of that hit again as magic, twice that on a crit, for the tower that applied it");
            case DOOM -> TermText.of("when it ends, it takes a share of all the damage it took while it lasted, as shadow damage");
            case BLIGHT -> TermText.of("it has ").plus(EffectKind.POISON)
                    .plus(" for as long as this lasts, and a slow that grows to its strongest as it ends");
            case BACKLASH -> TermText.of("its ").plus(EnemyStat.REGENERATION)
                    .plus(" deals it damage instead of restoring health, and a ").plus(EffectKind.HEAL)
                    .plus(" or a ").plus(EffectKind.SHIELD).plus(" strikes it at once with all it would give instead");
            case SILENCED -> TermText.of("its abilities don't fire; abilities on death and auras still do");
            case ANCHORED -> TermText.of("holds ").plus(EnemyStat.MOVE_SPEED)
                    .plus(" to at most " + TermNumbers.percent(1f - kind.rule().slowPerStack())
                            + " of its base, whatever raises it, and it comes back over its last "
                            + seconds(ActiveEffects.SLOW_FADE_TICKS));
            case UNRAVELED -> stacked(kind, EnemyStat.MAGIC_RESIST).plus(counted(kind));
            case BRITTLE -> changes(TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN)),
                    EnemyStat.PHYSICAL_DAMAGE_TAKEN);
            case TOLL -> TermText.of("time spent in an Obelisk's field, a stack a second; each stack: ")
                    .plus(changes(TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN)),
                            EnemyStat.PHYSICAL_DAMAGE_TAKEN, EnemyStat.MAGIC_DAMAGE_TAKEN))
                    .plus(counted(kind));
            case DRAGGED -> TermText.of("slows by as much as the field gives, deeper with every stack of ")
                    .plus(EffectKind.TOLL).plus("; only the strongest slow counts, and speed comes back over its last "
                            + seconds(ActiveEffects.SLOW_FADE_TICKS));
            case DEAD_ZONE -> changes(TermNumbers.signed(kind.rule().perStack().get(EnemyStat.SPIRIT).setTo()), EnemyStat.SPIRIT)
                    .plus(", so ").plus(EffectKind.HEAL).plus(" and ").plus(EffectKind.SHIELD).plus(" are worth nothing");
            case CRACKED -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signed(DamageUnits.inPoints(perStack(kind, EnemyStat.PLATING))), EnemyStat.PLATING))
                    .plus(inShares(kind));
            case SINGED -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signed(DamageUnits.inPoints(perStack(kind, EnemyStat.INSULATION))),
                            EnemyStat.INSULATION))
                    .plus(inShares(kind));
            case TARRED -> slowing("slows by " + TermNumbers.percent(kind.rule().slowPerStack()));
            case BLEEDING -> TermText.of("physical damage for every cell it travels, ignoring ").plus(EnemyStat.ARMOR)
                    .plus(" and ").plus(EnemyStat.PLATING).plus("; standing still, it takes none");
            case SOULFIRE -> pool().plus(", its own beside ").plus(EffectKind.BURN)
                    .plus(" and ").plus(EffectKind.POISON).plus("; it adds a stack of ")
                    .plus(earned(kind)).plus(" every " + seconds(Effect.STACK_INTERVAL_TICKS)
                            + " while it lasts, and every tower can see and aim at it");
            case SPRINTING -> changes(
                    TermNumbers.signedPercent(kind.rule().perStack().get(EnemyStat.MOVE_SPEED).percentAdd()),
                    EnemyStat.MOVE_SPEED);
            case SPELLPROOF -> stacked(kind, EnemyStat.MAGIC_RESIST).plus(counted(kind));
            case RESOLVE -> TermText.of("each stack: ")
                    .plus(changes(TermNumbers.signedPercent(perStack(kind, EnemyStat.PHYSICAL_DAMAGE_TAKEN)),
                            EnemyStat.PHYSICAL_DAMAGE_TAKEN, EnemyStat.MAGIC_DAMAGE_TAKEN))
                    .plus(", held at as many stacks as the ally that gives it has to give; away from it, it loses a stack every "
                            + seconds(Effect.FADE_STEP_TICKS));
            case UNYIELDING -> TermText.join(", ", List.of(
                    changes(TermNumbers.signed(perStack(kind, EnemyStat.RESILIENCE)), EnemyStat.RESILIENCE),
                    changes(TermNumbers.signed(perStack(kind, EnemyStat.SPIRIT)), EnemyStat.SPIRIT),
                    changes(TermNumbers.signed(perStack(kind, EnemyStat.MAGIC_RESIST)), EnemyStat.MAGIC_RESIST)));
            case HASTENED -> changes(TermNumbers.times(kind.rule().perStack().get(EnemyStat.MOVE_SPEED).multiply()),
                    EnemyStat.MOVE_SPEED).plus("; ").plus(EffectKind.FREEZE).plus(" and ")
                    .plus(EffectKind.DAZED).plus(" still stop it");
        };
    }

    /** What every pool does with its fuel. */
    private static TermText pool() {
        return TermText.of("a pool of fuel that it deals in full, faster the more it holds: "
                + TermNumbers.whole(ActiveEffects.REFERENCE_FUEL_POINTS) + " in "
                + TermNumbers.seconds(ActiveEffects.REFERENCE_BURN_SECONDS)
                + ", four times that in twice the time; fuel adds up whoever gives it, with no cap, and ")
                .plus(EnemyStat.TENACITY).plus(" shrinks it as it comes in");
    }

    /** {@code what}, then the rule every slow keeps. */
    private static TermText slowing(String what) {
        return TermText.of(what + "; only the strongest slow counts, and speed comes back over its last "
                + seconds(ActiveEffects.SLOW_FADE_TICKS));
    }

    /** What being stopped does to dodging: agility above none counts for nothing, agility already lost stays lost. */
    private static TermText stoppedAgility() {
        return TermText.of("it can't dodge: its ").plus(EnemyStat.AGILITY).plus(" counts as none unless it is lower still");
    }

    /** "each stack: Armor -5", the change read from {@code kind}'s rule. */
    private static TermText stacked(EffectKind kind, EnemyStat stat) {
        return TermText.of("each stack: ").plus(changes(TermNumbers.signed(perStack(kind, stat)), stat));
    }

    /** The rule every shared count keeps: its cap, its hold and its fade. */
    private static TermText counted(EffectKind kind) {
        return TermText.of(", up to " + kind.maxStacks() + " stacks; what adds stacks holds them " + seconds(Effect.HOLD_TICKS)
                + ", and what builds them over time holds them while it lasts; after that it loses a stack every "
                + seconds(Effect.FADE_STEP_TICKS));
    }

    /** The rule every kind in shares keeps: a share from each source, each on its own clock. */
    private static TermText inShares(EffectKind kind) {
        return TermText.of(", up to " + kind.maxStacks() + " stacks; each tower keeps its own share for as long as it "
                + "keeps it up");
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
