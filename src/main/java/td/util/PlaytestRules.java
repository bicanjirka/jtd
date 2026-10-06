package td.util;

/**
 * Rule switches the dev panel flips while playtesting. Off by default; nothing but the dev panel
 * turns one on.
 */
public final class PlaytestRules {

    private volatile boolean upgradeGatesIgnored;

    /** Whether a tower may buy an offered node without its XP or gate condition. */
    public boolean upgradeGatesIgnored() {
        return this.upgradeGatesIgnored;
    }

    public void setUpgradeGatesIgnored(boolean ignored) {
        this.upgradeGatesIgnored = ignored;
    }
}
