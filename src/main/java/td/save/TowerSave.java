package td.save;

import td.tower.TowerFactory;
import td.tower.TowerTally;
import td.wave.Point;

import java.util.List;

/**
 * One tower of a {@link GameSave}.
 *
 * @param nodes the ids of the upgrades it owns, in the order bought
 */
public record TowerSave(TowerFactory.Type type, Point cell, List<String> nodes, TowerTally tally) {

    public TowerSave {
        nodes = List.copyOf(nodes);
    }
}
