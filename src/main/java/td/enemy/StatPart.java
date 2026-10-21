package td.enemy;

import td.stat.StatModifier;
import td.term.Term;

/** One source's part in a stat: the trait or effect, and what it does to the stat. */
public record StatPart(Term source, StatModifier modifier) {
}
