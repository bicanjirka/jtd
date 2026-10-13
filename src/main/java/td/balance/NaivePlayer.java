package td.balance;

import td.tower.TowerFactory;
import td.util.ThreadConfined;
import td.wave.Point;

import java.util.Arrays;
import java.util.List;

/**
 * A player who has not learned the game: whenever it can afford a tower it builds one, of a random type, on a
 * random free cell from which that tower reaches some path. It never upgrades and never sells.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class NaivePlayer implements Player {

    private final List<TowerFactory.Type> types;

    NaivePlayer(List<TowerFactory.Type> types) {
        this.types = List.copyOf(types);
    }

    static PlayerSpec spec() {
        return new PlayerSpec("naive", (survey, seed) -> new NaivePlayer(Arrays.asList(TowerFactory.Type.values())));
    }

    @Override
    public void act(Turn turn) {
        while (true) {
            List<TowerFactory.Type> affordable = this.types.stream()
                    .filter(type -> turn.priceOf(type) <= turn.credits()).toList();
            if (affordable.isEmpty()) {
                return;
            }
            TowerFactory.Type type = affordable.get(turn.random().nextIndex(affordable.size()));
            List<Point> cells = turn.survey().buildable().stream()
                    .filter(turn::isFree)
                    .filter(cell -> turn.survey().coverage(type, cell) > 0)
                    .toList();
            if (cells.isEmpty() || turn.build(type, cells.get(turn.random().nextIndex(cells.size()))).isEmpty()) {
                return;
            }
        }
    }
}
