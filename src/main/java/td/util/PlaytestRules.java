package td.util;

/**
 * Rule switches for playtesting, flipped by the dev panel and the headless balance tools. Off by
 * default; nothing in normal play turns one on.
 */
public final class PlaytestRules {

    private static final float PERCENT = 100f;

    private volatile boolean upgradeGatesIgnored;
    private volatile boolean livesUnlimited;
    private volatile int enemyHealthPercent = 100;
    private volatile int bountyPercent = 100;
    private volatile int clearBonusPercent = 100;
    private volatile int spawnSpacingPercent = 100;

    /** Whether a tower may buy an offered node without its XP or gate condition. */
    public boolean upgradeGatesIgnored() {
        return this.upgradeGatesIgnored;
    }

    public void setUpgradeGatesIgnored(boolean ignored) {
        this.upgradeGatesIgnored = ignored;
    }

    /** Whether a leak takes no lives. It still costs its score. */
    public boolean livesUnlimited() {
        return this.livesUnlimited;
    }

    public void setLivesUnlimited(boolean unlimited) {
        this.livesUnlimited = unlimited;
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

    /** What every kill pays, and every leak costs, as a share of the authored bounty; 1 normally. */
    public float bountyScale() {
        return this.bountyPercent / PERCENT;
    }

    /** Enemies built from now on pay {@code percent} of their authored bounty. */
    public void setBountyPercent(int percent) {
        if (percent < 0) {
            throw new IllegalArgumentException("Bounty percent must not be negative: " + percent);
        }
        this.bountyPercent = percent;
    }

    /** What a round pays for clearing with no leak, as a share of what it is authored with; 1 normally. */
    public float clearBonusScale() {
        return this.clearBonusPercent / PERCENT;
    }

    /** Rounds that clear from now on pay {@code percent} of their authored clear bonus. */
    public void setClearBonusPercent(int percent) {
        if (percent < 0) {
            throw new IllegalArgumentException("Clear bonus percent must not be negative: " + percent);
        }
        this.clearBonusPercent = percent;
    }

    /** The gaps between a wave's spawns as a share of the authored ones; 1 normally. */
    public float spawnSpacingScale() {
        return this.spawnSpacingPercent / PERCENT;
    }

    /** Waves that start from now on space their spawns {@code percent} of the authored gap apart. */
    public void setSpawnSpacingPercent(int percent) {
        if (percent <= 0) {
            throw new IllegalArgumentException("Spawn spacing percent must be positive: " + percent);
        }
        this.spawnSpacingPercent = percent;
    }
}
