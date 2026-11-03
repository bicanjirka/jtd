package td.stat;

import java.util.List;

/**
 * A stat's base, each kind of source that changes it, and what it comes to: the sources that add, what they come
 * to together, then those that scale.
 *
 * @param stat     the row the base, the sum and the result read in
 * @param together the base with every adding source, before any that scales
 */
public record StatBreakdown(StatTerm stat, float base, List<StatPart> added, float together, List<StatPart> scaled,
                            float current) {

    public StatBreakdown {
        added = List.copyOf(added);
        scaled = List.copyOf(scaled);
    }

    /** A stat nothing changes. */
    public static StatBreakdown unchanged(StatTerm stat, float value) {
        return new StatBreakdown(stat, value, List.of(), value, List.of(), value);
    }

    /**
     * {@code parts} split where each really combines; {@code together} is the base with the adding parts, in
     * {@code stat}'s own terms.
     */
    public static StatBreakdown of(StatTerm stat, float base, List<StatPart> parts, float together, float current) {
        return new StatBreakdown(stat, base, parts.stream().filter(StatPart::adds).map(StatPart::added).toList(),
                together, parts.stream().filter(StatPart::scales).map(StatPart::scaled).toList(), current);
    }

    /** Whether any source changes it. */
    public boolean changed() {
        return !this.added.isEmpty() || !this.scaled.isEmpty();
    }

    /**
     * The breakdown of a stat resolved by the shared formula: {@code together} is the base with the adding parts,
     * clamped as the stat is.
     */
    public static <S extends Enum<S> & StatKey & StatTerm> StatBreakdown resolved(S stat, float base,
                                                                                 List<StatPart> parts, float current) {
        StatModifier adding = parts.stream().filter(StatPart::adds).map(part -> part.added().modifier())
                .reduce(StatModifier.none(), StatModifier::plus);
        return of(stat, base, parts, stat.clamp(adding.applyTo(base)), current);
    }
}
