package td.enemy;

import td.damage.DamageType;
import td.effect.EffectKind;
import td.effect.EffectTemplate;
import td.effect.HealTemplate;
import td.effect.InvisibleTemplate;
import td.effect.ShieldTemplate;
import td.effect.SprintTemplate;
import td.stat.DisruptionAura;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

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
            case PeriodicTrigger periodic -> "every " + seconds(periodic.intervalTicks());
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
            case SpawnEnemiesAction spawn -> spawn.consumesSelf()
                    ? TermText.of("turns into " + words.enemyName().apply(spawn.definitionId()))
                    : TermText.of("calls " + spawn.shape().members() + " " + words.enemyName().apply(spawn.definitionId()));
        };
    }

    private static TermText effect(EffectTemplate template) {
        return switch (template) {
            case ShieldTemplate shield -> TermText.of(EffectKind.SHIELD)
                    .plus(" of " + TermNumbers.number(shield.points()) + against(shield.restrictedTo()) + " for "
                            + seconds(shield.durationTicks()));
            case HealTemplate heal -> TermText.of(EffectKind.HEAL)
                    .plus(" of " + TermNumbers.number(heal.pointsPerTick() * heal.durationTicks()) + " over "
                            + seconds(heal.durationTicks()));
            case SprintTemplate sprint -> TermText.of(EffectKind.SPRINTING)
                    .plus(" for " + seconds(sprint.durationTicks()));
            case InvisibleTemplate invisible -> TermText.of(EffectKind.INVISIBLE)
                    .plus(" for " + seconds(invisible.durationTicks()));
        };
    }

    private static String against(Optional<DamageType> type) {
        return type.map(restricted -> " against " + restricted.name().toLowerCase(java.util.Locale.ROOT)).orElse("");
    }

    private static String seconds(int ticks) {
        return TermNumbers.seconds(ticks / TickRate.TICKS_PER_SECOND);
    }
}
