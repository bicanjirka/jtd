package td.fixtures;

import td.tower.AbstractTower;
import td.tower.TowerBaseStats;
import td.tower.TowerFactory;
import td.tower.TowerVisitor;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.Optional;

/** A tower with test-supplied upgrades, for testing the upgrade mechanism apart from real content. */
public final class FakeTower extends AbstractTower {

    private final UpgradeTree tree;
    private final Optional<UpgradeState> ownedUpgrades;

    private FakeTower(GameWorld context, int x, int y, UpgradeTree tree, Optional<UpgradeState> ownedUpgrades) {
        super(TowerFactory.Type.SNIPER, new TowerBaseStats(10, 3f, 20), context, x, y);
        this.tree = tree;
        this.ownedUpgrades = ownedUpgrades;
    }

    /** Buys from {@code tree} through the real upgrade mechanism. */
    public static FakeTower offering(GameWorld context, int x, int y, UpgradeTree tree) {
        return new FakeTower(context, x, y, tree, Optional.empty());
    }

    /** Reports {@code upgrades} as owned without buying them, and offers none. */
    public static FakeTower owning(GameWorld context, int x, int y, UpgradeState upgrades) {
        return new FakeTower(context, x, y, UpgradeTree.none(), Optional.of(upgrades));
    }

    @Override
    public UpgradeTree upgradeTree() {
        return this.tree;
    }

    @Override
    public UpgradeState upgrades() {
        return this.ownedUpgrades.orElseGet(super::upgrades);
    }

    /** Sets the kill count directly, skipping real damage. */
    public void setKillCount(int killCount) {
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
