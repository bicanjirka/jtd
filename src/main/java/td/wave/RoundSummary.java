package td.wave;

import td.enemy.Rank;

/**
 * One round of a level at a glance.
 *
 * @param highestRank the highest rank any of its waves sends
 * @param leaked      whether an enemy of it reached the end of its path; false until it is over
 */
public record RoundSummary(Rank highestRank, boolean leaked) {
}
