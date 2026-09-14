package td;

import td.tower.TowerFactory;

/**
 * One tower a {@link BalanceHarness} loadout places before a run starts: a type and the cell
 * to build it on. No upgrade-path selection in v1 - see {@code FEATURE-playtesting-and-balance-tooling.md}'s
 * V1 Scope.
 */
public record TowerPlacementSpec(TowerFactory.type type, int cellX, int cellY) {

    public static TowerPlacementSpec of(TowerFactory.type type, int cellX, int cellY) {
        return new TowerPlacementSpec(type, cellX, cellY);
    }
}
