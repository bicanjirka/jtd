package td.util;

/**
 * Rule switches for playtesting, flipped by the dev panel and the headless balance tools. Off by
 * default; nothing in normal play turns one on.
 */
public final class PlaytestRules {

    private static final float PERCENT = 100f;

    private volatile boolean upgradeGatesIgnored;
    private volatile int enemyHealthPercent = 100;

    /** Whether a tower may buy an offered node without its XP or gate condition. */
    public boolean upgradeGatesIgnored() {
        return this.upgradeGatesIgnored;
    }

    public void setUpgradeGatesIgnored(boolean ignored) {
        this.upgradeGatesIgnored = ignored;
    }

    /** Every enemy's health as a share of what it is authored with, spawns included; 1 normally. */
    public float enemyHealthScale() {
        return this.enemyHealthPercent / PERCENT;
    }

    /** Enemies built from now on get {@code percent} of their authored health. */
    public void setEnemyHealthPercent(int percent) {
        if (percent <= 0) {
            throw new IllegalArgumentException("Enemy health percent must be positive: " + percent);
        }
        this.enemyHealthPercent = percent;
    }
}
