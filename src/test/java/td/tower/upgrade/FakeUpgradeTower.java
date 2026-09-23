package td.tower.upgrade;

import td.tower.AbstractTower;
import td.tower.TowerBaseStats;
import td.tower.TowerFactory;
import td.tower.TowerVisitor;
import td.util.GameWorld;

/**
 * A minimal concrete tower carrying a fixed, test-supplied {@link UpgradeState}, used to
 * exercise {@link UpgradeTree#offered}/{@link UpgradeCondition} against a real {@code Tower}
 * without needing {@code AbstractTower} to own the buy mechanism yet (see {@code
 * td.tower.AbstractTowerTest}, which still exercises the old path-based mechanism this
 * package's own model doesn't touch until phase 2).
 */
final class FakeUpgradeTower extends AbstractTower {

    private final UpgradeState state;

    FakeUpgradeTower(GameWorld context, int x, int y, UpgradeState state) {
        super(TowerFactory.Type.SNIPER, 10, new TowerBaseStats(1000, 3f, 20), context, x, y);
        this.state = state;
    }

    @Override
    public UpgradeState upgrades() {
        return this.state;
    }

    /**
     * Seeds {@code killCount} directly for a condition's {@code progress()}/{@code
     * isSatisfied()} test - {@code AbstractTower.killCount} is a protected field only its own
     * {@code dealDamage} normally increments, but that method routes a hit through real enemy
     * damage math this test double has no need for.
     */
    void setKillCount(int killCount) {
        this.killCount = killCount;
    }

    @Override
    public void doTick(int gameTime) {
    }

    @Override
    public <R> R accept(TowerVisitor<R> visitor) {
        throw new UnsupportedOperationException("test double, not meant to be rendered");
    }
}
