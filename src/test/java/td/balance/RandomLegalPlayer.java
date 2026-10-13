package td.balance;

import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.util.ThreadConfined;
import td.wave.Point;

import java.util.Arrays;
import java.util.List;

/**
 * Random legal play, sells included: at each turn it may build a random tower on a random free cell, buy a random
 * node a random tower may buy, or sell a random tower. Whatever it does, the rules must hold.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class RandomLegalPlayer implements Player {

    private static final double SELL_CHANCE = 0.01;
    /** It sells only with more towers than this standing, so the board is never left empty for long. */
    private static final int KEPT = 3;
    private static final double UPGRADE_CHANCE = 0.4;

    static PlayerSpec spec() {
        return new PlayerSpec("random-legal", (survey, seed) -> new RandomLegalPlayer());
    }

    @Override
    public void act(Turn turn) {
        double roll = turn.random().nextDouble();
        List<Tower> towers = turn.towers();
        if (roll < SELL_CHANCE && towers.size() > KEPT) {
            Tower sold = towers.get(turn.random().nextIndex(towers.size()));
            Point cell = turn.cellOf(sold);
            turn.sell(sold);
            if (turn.towers().contains(sold) || !turn.isFree(cell)) {
                throw new IllegalStateException("A sold tower is still on " + cell);
            }
        } else if (roll < SELL_CHANCE + UPGRADE_CHANCE && !towers.isEmpty()) {
            Tower tower = towers.get(turn.random().nextIndex(towers.size()));
            List<UpgradeNode> nodes = turn.buyable(tower);
            if (!nodes.isEmpty()) {
                turn.upgrade(tower, nodes.get(turn.random().nextIndex(nodes.size())));
            }
        } else {
            List<TowerFactory.Type> affordable = Arrays.stream(TowerFactory.Type.values())
                    .filter(type -> turn.priceOf(type) <= turn.credits()).toList();
            List<Point> free = turn.survey().buildable().stream().filter(turn::isFree).toList();
            if (!affordable.isEmpty() && !free.isEmpty()) {
                turn.build(affordable.get(turn.random().nextIndex(affordable.size())),
                        free.get(turn.random().nextIndex(free.size())));
            }
        }
        if (turn.credits() < 0) {
            throw new IllegalStateException("Credits went negative: " + turn.credits());
        }
    }
}
