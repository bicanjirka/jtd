package td.enemy;

import td.term.TermText;

/** One ability as the player sees it: its name and when and what it does. */
public record AbilityEntry(AbilityTerm term, TermText text) {
}
