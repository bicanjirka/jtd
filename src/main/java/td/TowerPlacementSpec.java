package td;

import td.tower.TowerFactory;

/** One tower a {@link BalanceHarness} loadout places: a type and a cell. */
public record TowerPlacementSpec(TowerFactory.Type type, int cellX, int cellY) {

    public static TowerPlacementSpec of(TowerFactory.Type type, int cellX, int cellY) {
        return new TowerPlacementSpec(type, cellX, cellY);
    }
}
