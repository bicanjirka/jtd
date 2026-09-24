package td.tower.upgrade;

import td.tower.AbstractTower;
import td.tower.TowerBaseStats;
import td.tower.TowerFactory;
import td.tower.TowerVisitor;
import td.util.GameWorld;

/** A tower with a test-supplied {@link UpgradeState}, for testing offers and conditions. */
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

    /** Sets the kill count directly, skipping real damage. */
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
