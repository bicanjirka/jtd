package td.enemy;

import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.EffectTemplate;
import td.effect.HealTemplate;
import td.effect.InvisibleTemplate;
import td.effect.ShieldTemplate;
import td.effect.SpellproofTemplate;
import td.effect.SprintTemplate;
import td.effect.UnyieldingTemplate;
import td.stat.DisruptionAura;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;
import td.zone.Zone;

import java.util.Optional;
import java.util.function.Function;

/** What an ability says: when it fires, a colon, and what it does, every number from its own data. */
final class AbilityText {

    /** What a sentence needs from the world: the board's cell size and enemy names by id. */
    record Words(float cellSize, Function<String, String> enemyName) {
    }

    private AbilityText() {
    }

    static TermText of(Ability ability, Words words) {
        return TermText.of(when(ability.trigger()) + ": ").plus(what(ability.action(), words));
    }

    /** A Jammer's disruption, as the trait the player sees. */
    static TermText jam(DisruptionAura aura, float cellSize) {
        return TermText.of("towers within " + TermNumbers.cells(aura.radius() / cellSize) + ": "
                + TermNumbers.signedPercent(-aura.attackSpeedPenalty()) + " attack speed, "
                + TermNumbers.signed(-aura.rangePenaltyCells()) + " range");
    }

    private static String when(AbilityTrigger trigger) {
        return switch (trigger) {
            case PeriodicTrigger periodic -> "every " + seconds(periodic.intervalTicks())
                    + (periodic.minHealthFraction() > 0f
                            ? ", while above " + TermNumbers.percent(periodic.minHealthFraction()) + " health" : "");
            case OnceTrigger once -> "once, after " + seconds(once.delayTicks());
            case HealthThresholdTrigger threshold -> "once, below " + TermNumbers.percent(threshold.fraction())
                    + " health";
            case OnDeathTrigger _ -> "when it dies";
            case TimeSinceLastHitTrigger idle -> "after " + seconds(idle.windowTicks()) + " without being hit";
            case OnCriticalHitTakenTrigger _ -> "each time it survives a crit";
            case OnFirstDamageTakenTrigger _ -> "the first time it is hit";
            case OnDodgeTrigger _ -> "each time it dodges";
        };
    }

    private static TermText what(AbilityAction action, Words words) {
        return switch (action) {
            case ApplyEffectAction apply -> switch (apply.target()) {
                case SelfTarget _ -> TermText.of("gains ").plus(effect(apply.template()));
                case RadiusTarget radius -> TermText.of("gives every other enemy within "
                        + TermNumbers.number(radius.radius() / words.cellSize()) + " cells ")
                        .plus(effect(apply.template()));
            };
            case LeaveZoneAction zone -> zoneText(zone);
            case SpawnEnemiesAction spawn -> spawn.consumesSelf()
                    ? TermText.of("turns into " + words.enemyName().apply(spawn.definitionId()))
                    : TermText.of("calls " + spawn.shape().members() + " " + words.enemyName().apply(spawn.definitionId()));
        };
    }

    private static TermText zoneText(LeaveZoneAction zone) {
        String reach = TermNumbers.cells(zone.radiusCells());
        String lasts = zone.lifetimeTicks() == Zone.UNTIL_WAVE_END ? "until the wave is over"
                : "for " + seconds(zone.lifetimeTicks());
        return switch (zone.kind()) {
            case HEALING_GROUND -> TermText.of("leaves ground " + reach + " wide that gives enemies on it ")
                    .plus(EffectKind.HEAL).plus(" of " + TermNumbers.number(zone.power() * TickRate.TICKS_PER_SECOND)
                            + " a second, " + lasts);
            case HASTE_GROUND -> TermText.of("leaves ground " + reach + " wide that gives enemies on it ")
                    .plus(EffectKind.HASTENED).plus(", " + lasts);
            case WARD_MINE -> TermText.of("lays a mine " + reach + " wide that gives the first enemy to step on it a ")
                    .plus(EffectKind.SHIELD).plus(" of " + TermNumbers.number(zone.power()) + ", " + lasts);
            case BLINDING_FIELD -> TermText.of("blinds the towers within " + reach + ": "
                    + TermNumbers.percent(zone.power()) + " miss chance, " + lasts);
            case BURNING_GROUND, TAR, FROST_GROUND, FALLOUT, MINE, CURSED_CLOUD -> TermText.of("leaves a zone");
        };
    }

    private static TermText effect(EffectTemplate template) {
        return switch (template) {
            case ShieldTemplate shield -> TermText.of(EffectKind.SHIELD)
                    .plus(" of " + TermNumbers.number(shield.points()) + against(shield.restrictedTo())
                            + (shield.durationTicks() == Effect.LASTING_TICKS ? " until broken" : lasting(shield.durationTicks())));
            case HealTemplate heal -> TermText.of(EffectKind.HEAL)
                    .plus(" of " + TermNumbers.number(heal.pointsPerTick() * heal.durationTicks()) + " over "
                            + seconds(heal.durationTicks()));
            case SprintTemplate sprint -> TermText.of(EffectKind.SPRINTING).plus(lasting(sprint.durationTicks()));
            case SpellproofTemplate spellproof -> TermText.of(EffectKind.SPELLPROOF)
                    .plus(" of " + TermNumbers.number(spellproof.stacks()) + " stacks" + lasting(spellproof.durationTicks()));
            case UnyieldingTemplate unyielding -> TermText.of(EffectKind.UNYIELDING)
                    .plus(lasting(unyielding.durationTicks()));
            case InvisibleTemplate invisible -> TermText.of(EffectKind.INVISIBLE)
                    .plus(lasting(invisible.durationTicks()));
        };
    }

    private static String against(Optional<DamageType> type) {
        return type.map(restricted -> " against " + restricted.name().toLowerCase(java.util.Locale.ROOT)).orElse("");
    }

    private static String lasting(int ticks) {
        return ticks == Effect.LASTING_TICKS ? " for good" : " for " + seconds(ticks);
    }

    private static String seconds(int ticks) {
        return TermNumbers.seconds(ticks / TickRate.TICKS_PER_SECOND);
    }
}
