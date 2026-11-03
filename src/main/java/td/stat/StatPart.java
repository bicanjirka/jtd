package td.stat;

import td.term.TermText;

/**
 * One kind of source's part in a stat: who gives it, how many of that kind there are, and what they do together.
 *
 * @param stacks   how many of this kind give it; {@code 1} for one
 * @param stat     the stat {@code modifier} changes, whose unit its change reads in
 */
public record StatPart(TermText source, int stacks, StatTerm stat, StatModifier modifier) {

    /** Whether the part adds before the multipliers: a flat or a percent. */
    public boolean adds() {
        return this.modifier.flat() != 0f || this.modifier.percentAdd() != 0f;
    }

    /** Whether the part acts after the sum: a multiplier, a set value or a cap. */
    public boolean scales() {
        return this.modifier.multiply() != 1f || this.modifier.hasSetValue() || this.modifier.hasCap();
    }

    /** Only what adds: the flat and the percent. */
    public StatPart added() {
        StatModifier adding = StatModifier.flat(this.modifier.flat()).plus(StatModifier.percent(this.modifier.percentAdd()));
        return new StatPart(this.source, this.stacks, this.stat, adding);
    }

    /** Only what scales: the multiplier, the set value and the cap. */
    public StatPart scaled() {
        return new StatPart(this.source, this.stacks, this.stat, new StatModifier(0f, 0f, this.modifier.multiply(),
                this.modifier.setTo(), this.modifier.capShareOfBase()));
    }
}
