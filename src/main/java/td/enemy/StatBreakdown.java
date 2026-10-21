package td.enemy;

import td.stat.EnemyStat;

import java.util.List;

/** A stat's base, each source that changes it, and what it comes to. */
public record StatBreakdown(EnemyStat stat, float base, List<StatPart> parts, float current) {

    public StatBreakdown {
        parts = List.copyOf(parts);
    }
}
