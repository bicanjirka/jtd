package td.ui;

import td.enemy.EnemyInspection;
import td.enemy.HitResolution;
import td.enemy.Rank;
import td.stat.EnemyStat;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns an {@link EnemyInspection} into player-facing text. Pure: no Swing, no live mob. A stat at
 * its default is left out, so a plain enemy's block stays short.
 */
final class EnemyStatText {

    private EnemyStatText() {
    }

    /** The wave-preview hover: identity, rank line, stats and traits. */
    static String preview(EnemyInspection inspection) {
        List<String> lines = new ArrayList<>();
        lines.add(inspection.name());
        lines.add("");
        lines.add(inspection.description());
        lines.add("");
        lines.add("Rank: " + titleCase(inspection.rank()) + "   Health: " + inspection.maxHealth()
                + "   Bounty: " + inspection.bounty());
        lines.addAll(statLines(inspection));
        lines.addAll(traitBlock(inspection));
        return String.join("\n", lines);
    }

    /** The live inspector: current health, stats, effects with their time left, traits and fate. */
    static String live(EnemyInspection inspection) {
        List<String> lines = new ArrayList<>();
        lines.add(inspection.name() + " (" + titleCase(inspection.rank()) + ")");
        switch (inspection.fate()) {
            case ALIVE -> {
            }
            case KILLED -> lines.add("Killed");
            case LEAKED -> lines.add("Leaked");
        }
        lines.add("");
        lines.add("Health: " + inspection.health() + " / " + inspection.maxHealth() + "   Bounty: " + inspection.bounty());
        lines.addAll(statLines(inspection));
        if (inspection.stat(EnemyStat.FREEZE_DR) >= 1f && inspection.freezeStep() > 0) {
            lines.add("Freeze DR: next freeze " + freezeStepText(inspection.freezeStep()));
        }
        if (!inspection.effects().isEmpty()) {
            lines.add("");
            lines.add("Effects:");
            for (EnemyInspection.EffectState effect : inspection.effects()) {
                lines.add("- " + effectLine(effect));
            }
        }
        lines.addAll(traitBlock(inspection));
        return String.join("\n", lines);
    }

    /** One line per stat that differs from its default, plus speed, which always shows. */
    static List<String> statLines(EnemyInspection inspection) {
        List<String> lines = new ArrayList<>();
        lines.add("Speed " + decimal(inspection.stat(EnemyStat.MOVE_SPEED) * TickRate.TICKS_PER_SECOND) + " px/s");
        for (EnemyStat stat : EnemyStat.values()) {
            float value = inspection.stat(stat);
            if (stat != EnemyStat.MOVE_SPEED && value != stat.defaultBase()) {
                lines.add(statLine(stat, value));
            }
        }
        return lines;
    }

    static String statLine(EnemyStat stat, float value) {
        return switch (stat) {
            case ARMOR -> "Armor " + decimal(value) + " (" + mitigationText(value) + " physical)";
            case MAGIC_RESIST -> "Magic resist " + decimal(value) + " (" + mitigationText(value) + " magic)";
            case PHYSICAL_PLATING -> "Plating " + decimal(value / 100f) + " per physical hit";
            case MAGIC_PLATING -> "Plating " + decimal(value / 100f) + " per magic hit";
            case MOVE_SPEED -> "Speed " + decimal(value * TickRate.TICKS_PER_SECOND) + " px/s";
            case PHYSICAL_DAMAGE_TAKEN -> "Takes " + percent(value) + " physical damage";
            case MAGIC_DAMAGE_TAKEN -> "Takes " + percent(value) + " magic damage";
            case RESILIENCE -> value >= 100f ? "Resilience 100 (immune to crits)"
                    : "Resilience " + decimal(value) + " (-" + percent(value / 100f) + " crit chance and bonus)";
            case CRIT_CHANCE_TAKEN -> "Crit chance taken x" + decimal(value);
            case SPIRIT -> "Spirit " + decimal(value) + " (" + signedPercent(value / 100f) + " heals and shields)";
            case REGENERATION -> "Regenerates " + decimal(value / 100f * TickRate.TICKS_PER_SECOND) + " health/s";
            case SLOW_RESIST -> resistLine("slow", value);
            case BURN_RESIST -> resistLine("burn", value);
            case FREEZE_RESIST -> resistLine("freeze", value);
            case STEALTH -> "Stealthed: towers can't target it";
            case FREEZE_DR -> "Freeze diminishing returns";
        };
    }

    private static List<String> traitBlock(EnemyInspection inspection) {
        if (inspection.traitLines().isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("Traits:");
        inspection.traitLines().forEach(line -> lines.add("- " + line));
        return lines;
    }

    private static String effectLine(EnemyInspection.EffectState effect) {
        String name = switch (effect.kind()) {
            case SLOW -> "Slowed";
            case BURN -> "Burning";
            case FREEZE -> "Frozen";
            case SHIELD -> "Shielded";
            case INVISIBLE -> "Invisible";
            case HEAL -> "Healing";
        };
        if (effect.remainingTicks().isEmpty()) {
            return name;
        }
        return name + " " + String.format(Locale.ROOT, "%.1f", effect.remainingTicks().getAsInt()
                / TickRate.TICKS_PER_SECOND) + "s";
    }

    private static String freezeStepText(int step) {
        return switch (step) {
            case 0 -> "100%";
            case 1 -> "50%";
            case 2 -> "25%";
            default -> "blocked";
        };
    }

    private static String resistLine(String kind, float value) {
        return value >= 1f ? "Immune to " + kind : kind.substring(0, 1).toUpperCase(Locale.ROOT) + kind.substring(1)
                + " resist " + percent(value);
    }

    /** "-50%" for armor that halves a hit, "+25%" for negative armor. */
    private static String mitigationText(float armor) {
        return signedPercent(HitResolution.mitigationMultiplier(armor) - 1f);
    }

    private static String percent(float fraction) {
        return Math.round(fraction * 100) + "%";
    }

    private static String signedPercent(float fraction) {
        int rounded = Math.round(fraction * 100);
        return (rounded > 0 ? "+" : "") + rounded + "%";
    }

    private static String decimal(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String titleCase(Rank rank) {
        String name = rank.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
