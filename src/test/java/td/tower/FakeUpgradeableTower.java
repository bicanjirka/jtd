package td.tower;

import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

/**
 * A tower with a test-supplied {@link UpgradeTree}, for testing the upgrade mechanism apart from
 * real content.
 */
final class FakeUpgradeableTower extends AbstractTower {

    private final UpgradeTree tree;

    FakeUpgradeableTower(GameWorld context, int x, int y, UpgradeTree tree) {
        super(TowerFactory.Type.SNIPER, 10, new TowerBaseStats(1000, 3f, 20), context, x, y);
        this.tree = tree;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return this.tree;
    }

    @Override
    public void doTick(int gameTime) {
    }

    @Override
    public <R> R accept(TowerVisitor<R> visitor) {
        throw new UnsupportedOperationException("test double, not meant to be rendered");
    }
}
