package td.save;

import td.tower.TowerFactory;
import td.tower.TowerTally;
import td.tower.TowerUse;
import td.wave.Point;

import java.util.List;
import java.util.OptionalInt;

/**
 * One tower of a {@link GameSave}.
 *
 * @param nodes the ids of the upgrades it owns, in the order bought
 * @param paid  what it was bought for; empty for a save from before prices were kept, which rebuilds at today's price
 * @param use   what of it has been used; all of it, for a save from before that was kept
 */
public record TowerSave(TowerFactory.Type type, Point cell, List<String> nodes, TowerTally tally, OptionalInt paid,
                        TowerUse use) {

    public TowerSave {
        nodes = List.copyOf(nodes);
    }

    public static TowerSave of(TowerFactory.Type type, Point cell, List<String> nodes, TowerTally tally) {
        return new TowerSave(type, cell, nodes, tally, OptionalInt.empty(), TowerUse.used());
    }

    public TowerSave withPaid(int price) {
        return new TowerSave(this.type, this.cell, this.nodes, this.tally, OptionalInt.of(price), this.use);
    }

    public TowerSave withUse(TowerUse next) {
        return new TowerSave(this.type, this.cell, this.nodes, this.tally, this.paid, next);
    }
}
