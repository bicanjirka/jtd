package td.enemy;

import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.damage.Delivery;
import td.effect.EffectKind;
import td.stat.EnemyStat;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One trace line per event: {@code OBSERVE t=<tick> #<id> <Name> <Rank> <EVENT> key=value ...}, in points and the
 * game's names, so a grep for an id pulls out one enemy's story.
 */
public final class MobEventText {

    /** Stats kept in units, like health, and shown in points. */
    private static final Set<EnemyStat> IN_UNITS = Set.of(EnemyStat.PLATING, EnemyStat.INSULATION, EnemyStat.REGENERATION);

    private MobEventText() {
    }

    public static String line(int tick, MobLabel label, MobEvent event) {
        return "OBSERVE t=" + tick + " #" + label.id() + " " + label.name() + " " + titleCase(label.rank().name()) + " "
                + body(label, event);
    }

    /** A line about no enemy in particular, such as a snapshot with nothing to show. */
    public static String note(int tick, String text) {
        return "OBSERVE t=" + tick + " " + text;
    }

    private static String body(MobLabel label, MobEvent event) {
        return switch (event) {
            case MobEvent.Started started -> "START via=" + started.via() + " " + snapshot(label, started.snapshot());
            case MobEvent.Snapshot snapshot -> "SNAPSHOT " + snapshot(label, snapshot.snapshot());
            case MobEvent.Hit hit -> hit(label, hit);
            case MobEvent.Dodged dodged -> "DODGE from=" + dodged.from() + " " + arrived(dodged.arrived());
            case MobEvent.Killed killed -> "KILLED by=" + killed.by() + " overkill=" + points(killed.overkill());
            case MobEvent.EffectRequested requested -> "EFFECT " + requested.kind().title() + " requested from="
                    + requested.from() + " length=" + requested.ticks() + "t";
            case MobEvent.EffectShortened shortened -> "EFFECT " + shortened.kind().title() + " shortened resist="
                    + factor(shortened.resisted()) + " dr=" + factor(shortened.diminished()) + " length=" + shortened.ticks() + "t";
            case MobEvent.EffectBlocked blocked -> "EFFECT " + blocked.kind().title() + " blocked: " + blocked.why();
            case MobEvent.EffectInverted inverted -> "EFFECT " + inverted.kind().title() + " inverted: " + inverted.into();
            case MobEvent.EffectChanged changed -> "EFFECT " + changed.kind().title() + " " + change(changed)
                    + " cause=" + changed.cause();
            case MobEvent.HealthChanged health -> "HEAL cause=" + health.cause() + " hp=" + points(health.before()) + "->"
                    + points(health.after()) + "/" + points(label.maxHealth());
            case MobEvent.Cast cast -> "CAST " + cast.kind().title()
                    + (cast.radiusCells() > 0f ? " radius=" + number(cast.radiusCells()) + " targets=" + cast.targets() : " on=self");
            case MobEvent.ZoneLeft zone -> "CAST " + zone.kind().name().toLowerCase(Locale.ROOT).replace('_', '-')
                    + " zone radius=" + number(zone.radiusCells());
            case MobEvent.Spawned spawned -> spawned.hatched() ? "HATCH into=" + ids(spawned.ids()) : "SPAWN " + ids(spawned.ids());
            case MobEvent.Ended ended -> "END " + ended.how() + " lived=" + ended.livedTicks() + "t damage=["
                    + ended.totals().landed().entrySet().stream().map(entry -> entry.getKey() + "=" + points(entry.getValue()))
                    .collect(Collectors.joining(", "))
                    + "] absorbed=" + points(ended.totals().absorbed()) + " dodges=" + ended.totals().dodges() + " overkill="
                    + points(ended.totals().overkill()) + " effects=" + effects(ended.effects());
        };
    }

    private static String snapshot(MobLabel label, MobSnapshot snapshot) {
        String stats = snapshot.stats().stream().map(reading -> " " + stat(reading)).collect(Collectors.joining());
        return "hp=" + points(snapshot.health()) + "/" + points(label.maxHealth()) + stats + " traits="
                + snapshot.traits() + " effects=" + effects(snapshot.effects());
    }

    private static String stat(MobSnapshot.StatReading reading) {
        String value = statValue(reading.stat(), reading.value());
        String base = statValue(reading.stat(), reading.base());
        return reading.stat().name().toLowerCase(Locale.ROOT) + "=" + value + (base.equals(value) ? "" : "(base " + base + ")");
    }

    private static String statValue(EnemyStat stat, float value) {
        return IN_UNITS.contains(stat) ? points(value) : number(value);
    }

    private static String hit(MobLabel label, MobEvent.Hit hit) {
        HitSteps steps = hit.steps();
        DamageType type = steps.arrived().type();
        StringBuilder text = new StringBuilder(steps.delivery() == Delivery.PERIODIC ? "PERIODIC" : "HIT");
        text.append(" from=").append(hit.cause().from());
        hit.cause().source().ifPresent(source -> text.append(" effect=").append(source.title()));
        text.append(' ').append(arrived(steps.arrived()));
        if (hit.cause().markSpent()) {
            text.append(" mark=spent");
        }
        if (steps.arrived().critical()) {
            text.append(" crit=arrived");
        }
        for (HitStep step : steps.steps()) {
            text.append(' ').append(step(type, step));
        }
        MobEvent.HitOutcome outcome = hit.outcome();
        text.append(" resolved=").append(points(steps.result().amount()));
        if (outcome.absorbed() > 0) {
            text.append(" shield=-").append(points(outcome.absorbed()));
        }
        return text.append(" landed=").append(points(outcome.landed().amount())).append(" hp=")
                .append(points(outcome.healthBefore())).append("->").append(points(outcome.healthAfter())).append('/')
                .append(points(label.maxHealth())).toString();
    }

    private static String step(DamageType type, HitStep step) {
        String after = "->" + points(step.after());
        return switch (step.kind()) {
            case CRIT -> "crit=" + factor(step.stat()) + after;
            case MITIGATION -> (type == DamageType.PHYSICAL ? "armor" : "magic_resist") + "=" + number(step.stat())
                    + penetrated(number(step.stat()), number(step.effective())) + after;
            case FLAT_REDUCTION -> (type == DamageType.PHYSICAL ? "plating" : "insulation") + "=" + points(step.stat())
                    + penetrated(points(step.stat()), points(step.effective())) + after;
            case DAMAGE_TAKEN -> "taken=" + factor(step.stat()) + after;
            case PERIODIC_TAKEN -> "periodic_taken=" + factor(step.stat()) + after;
        };
    }

    private static String penetrated(String stat, String effective) {
        return stat.equals(effective) ? "" : "(pen->" + effective + ")";
    }

    private static String arrived(Damage damage) {
        return damage.type().name().toLowerCase(Locale.ROOT) + " in=" + points(damage.amount());
    }

    private static String change(MobEvent.EffectChanged changed) {
        EffectState before = changed.before();
        EffectState after = changed.after();
        return switch (changed.change()) {
            case GAINED -> "gained" + state(after);
            case REFRESHED -> "refreshed left=" + before.remaining().orElse(0) + "t->" + after.remaining().orElse(0) + "t";
            case STACKS -> "stacks=" + before.stacks() + "->" + after.stacks()
                    + (after.remaining().isPresent() ? " left=" + after.remaining().getAsInt() + "t" : "");
            case TOPPED_UP -> "topped-up " + level(before) + "->" + levelValue(after);
            case WEAKENED -> "weakened " + level(before) + "->" + levelValue(after);
            case ENDED -> "ended";
            case UNCHANGED -> "unchanged" + state(after);
        };
    }

    private static String effects(List<EffectState> effects) {
        return effects.stream().map(state -> state.kind().title() + state(state))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String state(EffectState state) {
        StringBuilder text = new StringBuilder();
        if (state.stacks() > 0) {
            text.append(" stacks=").append(state.stacks());
        }
        state.remaining().ifPresent(ticks -> text.append(" left=").append(ticks).append('t'));
        if (state.level().isPresent()) {
            text.append(' ').append(level(state));
        }
        return text.toString();
    }

    /** A shield's pool in points, or how full a level or pool is. */
    private static String level(EffectState state) {
        return (state.kind() == EffectKind.SHIELD ? "pool=" : "full=") + levelValue(state);
    }

    private static String levelValue(EffectState state) {
        return number(state.level().orElse(0));
    }

    private static String ids(List<Long> ids) {
        return ids.stream().map(id -> "#" + id).collect(Collectors.joining(" "));
    }

    private static String points(float units) {
        return number(DamageUnits.inPoints(units));
    }

    /** At most two decimals, and none for a whole number. */
    static String number(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        if (text.contains(".")) {
            text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return text.equals("-0") ? "0" : text;
    }

    private static String factor(float factor) {
        return "x" + String.format(Locale.ROOT, "%.2f", factor);
    }

    private static String titleCase(String constant) {
        return constant.charAt(0) + constant.substring(1).toLowerCase(Locale.ROOT);
    }
}
