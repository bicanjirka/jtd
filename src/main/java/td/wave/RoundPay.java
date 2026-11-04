package td.wave;

/**
 * What one round pays.
 *
 * @param bounty     what its wave scripts' enemies pay when killed, before anything they spawn in play
 * @param clearBonus what it offers for clearing with no enemy leaked
 * @param bonusPaid  what it paid of that; 0 until it is over
 */
public record RoundPay(int bounty, int clearBonus, int bonusPaid) {

    private static final RoundPay NONE = new RoundPay(0, 0, 0);

    public static RoundPay none() {
        return NONE;
    }

    public RoundPay withBonusPaid(int paid) {
        return new RoundPay(this.bounty, this.clearBonus, paid);
    }
}
