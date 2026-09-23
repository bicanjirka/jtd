package td.tower;

import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

/**
 * A minimal concrete tower exposing a fixed, test-supplied {@link UpgradeTree}, used
 * to exercise {@link AbstractTower}'s upgrade mechanism (buying, exclusivity, composing
 * with an Aura tower's buff) in isolation from any real tower's own content.
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
