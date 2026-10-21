package td.enemy;

import td.term.Term;

/**
 * Memory a tower keeps about this enemy, named for the player by the tower's display name.
 *
 * @param term     what the mark is called
 * @param steps    how far it has built
 * @param maxSteps where it tops out
 * @param source   the display name of the tower that holds it
 */
public record EnemyMark(Term term, int steps, int maxSteps, String source) {
}
