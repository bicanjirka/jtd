package td.balance;

import td.tower.TowerFactory;
import td.wave.Point;

import java.util.Locale;

/**
 * One thing a player did, in the words a play script uses: {@code build SNIPER 4 10},
 * {@code upgrade 4 10 base.attune}, {@code sell 4 10}. A play is its seed and its actions, so a
 * script replays it exactly.
 */
public sealed interface PlayerAction permits PlayerAction.Build, PlayerAction.Upgrade, PlayerAction.Sell {

    /** The cell the action is about. */
    Point cell();

    String toLine();

    static PlayerAction parse(String line) {
        String[] words = line.trim().split("\\s+");
        return switch (words[0]) {
            case "build" -> new Build(TowerFactory.Type.valueOf(words[1].toUpperCase(Locale.ROOT)),
                    new Point(Integer.parseInt(words[2]), Integer.parseInt(words[3])));
            case "upgrade" -> new Upgrade(new Point(Integer.parseInt(words[1]), Integer.parseInt(words[2])), words[3]);
            case "sell" -> new Sell(new Point(Integer.parseInt(words[1]), Integer.parseInt(words[2])));
            default -> throw new IllegalArgumentException("Unknown action: " + line);
        };
    }

    record Build(TowerFactory.Type type, Point cell) implements PlayerAction {

        @Override
        public String toLine() {
            return "build " + this.type + " " + this.cell.x() + " " + this.cell.y();
        }
    }

    record Upgrade(Point cell, String nodeId) implements PlayerAction {

        @Override
        public String toLine() {
            return "upgrade " + this.cell.x() + " " + this.cell.y() + " " + this.nodeId;
        }
    }

    record Sell(Point cell) implements PlayerAction {

        @Override
        public String toLine() {
            return "sell " + this.cell.x() + " " + this.cell.y();
        }
    }
}
