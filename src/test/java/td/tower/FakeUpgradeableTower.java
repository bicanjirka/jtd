package td.tower;

import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;

import java.util.List;

/**
 * A minimal concrete tower exposing a fixed, test-supplied set of {@link UpgradePath}s, used
 * to exercise {@link AbstractTower}'s upgrade-path mechanism (choosing, permanence,
 * composing with an Aura tower's buff) in isolation from any real tower's own content.
 */
final class FakeUpgradeableTower extends AbstractTower {

    private final List<UpgradePath> paths;

    FakeUpgradeableTower(GameWorld context, int x, int y, List<UpgradePath> paths) {
        super(TowerFactory.Type.first, 10, 1000, 3f, 20, context, x, y);
        this.paths = paths;
    }

    @Override
    public List<UpgradePath> availablePaths() {
        return this.paths;
    }

    @Override
    public void doTick(int gameTime) {
    }

    @Override
    public <R> R accept(TowerVisitor<R> visitor) {
        throw new UnsupportedOperationException("test double, not meant to be rendered");
    }
}
