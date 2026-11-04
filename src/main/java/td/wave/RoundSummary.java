package td.wave;

import td.enemy.Rank;

/**
 * One round of a level at a glance.
 *
 * @param highestRank the highest rank any of its waves sends
 * @param unique      whether it sends an enemy that exists only at Boss rank
 * @param leaked      whether an enemy of it reached the end of its path; false until it is over
 * @param pay         what it pays
 */
public record RoundSummary(Rank highestRank, boolean unique, boolean leaked, RoundPay pay) {

    /** A round that pays nothing. */
    public RoundSummary(Rank highestRank, boolean unique, boolean leaked) {
        this(highestRank, unique, leaked, RoundPay.none());
    }

    public RoundSummary withPay(RoundPay paying) {
        return new RoundSummary(this.highestRank, this.unique, this.leaked, paying);
    }

    public RoundSummary withBonusPaid(int paid) {
        return this.withPay(this.pay.withBonusPaid(paid));
    }

    /** The credits it paid for clearing with no leak; 0 until it is over. */
    public int bonusPaid() {
        return this.pay.bonusPaid();
    }
}
