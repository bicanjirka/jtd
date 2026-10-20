package td.save;

import td.economy.EconomyState;
import td.tower.TowerFactory;
import td.tower.TowerTally;
import td.wave.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A {@link GameSave} as plain text, one fact a line, so a save can be read and edited by hand:
 * <pre>
 * level Curly Path
 * wave 17
 * economy 120 3400 5
 * tower SNIPER 6 11 400 12 123456 40 sniper.head.x.1 sniper.head.x.2
 * </pre>
 * The economy line is credits, score and lives; a tower line is its type, cell, XP, deeds, damage dealt, kills and
 * the upgrades it owns in the order bought.
 */
public final class GameSaveText {

    private static final int TOWER_FIELDS = 8;

    private GameSaveText() {
    }

    public static String write(GameSave save) {
        StringBuilder text = new StringBuilder()
                .append("level ").append(save.level()).append('\n')
                .append("wave ").append(save.nextWave()).append('\n')
                .append("economy ").append(save.economy().credits()).append(' ').append(save.economy().score())
                .append(' ').append(save.economy().lives()).append('\n');
        for (TowerSave tower : save.towers()) {
            TowerTally tally = tower.tally();
            text.append("tower ").append(tower.type().name()).append(' ').append(tower.cell().x()).append(' ')
                    .append(tower.cell().y()).append(' ').append(tally.xp()).append(' ').append(tally.deeds())
                    .append(' ').append(tally.damageDealt()).append(' ').append(tally.kills());
            tower.nodes().forEach(node -> text.append(' ').append(node));
            text.append('\n');
        }
        return text.toString();
    }

    /** The save {@code text} describes, or what is wrong with it. */
    public static Read read(String text) {
        String level = "";
        int wave = 0;
        Optional<EconomyState> economy = Optional.empty();
        List<TowerSave> towers = new ArrayList<>();
        for (String raw : text.split("\n")) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] words = line.split("\\s+");
            if (words.length < fieldsOf(words[0])) {
                return Read.problem("Too short: " + line);
            }
            try {
                switch (words[0]) {
                    case "level" -> level = line.substring("level".length()).strip();
                    case "wave" -> wave = Integer.parseInt(words[1]);
                    case "economy" -> economy = Optional.of(new EconomyState(Integer.parseInt(words[1]),
                            Integer.parseInt(words[2]), Integer.parseInt(words[3])));
                    case "tower" -> {
                        Optional<TowerSave> tower = tower(words);
                        if (tower.isEmpty()) {
                            return Read.problem("Not a tower: " + line);
                        }
                        towers.add(tower.get());
                    }
                    default -> {
                        return Read.problem("Unknown line: " + line);
                    }
                }
            } catch (NumberFormatException e) {
                return Read.problem("Can't read: " + line);
            }
        }
        if (level.isEmpty() || wave < 1 || economy.isEmpty()) {
            return Read.problem("A save needs a level, a wave and an economy line");
        }
        return Read.of(new GameSave(level, wave, economy.get(), towers));
    }

    /** How many words a line starting with {@code key} has at least, the key included. */
    private static int fieldsOf(String key) {
        return switch (key) {
            case "wave" -> 2;
            case "economy" -> 4;
            case "tower" -> TOWER_FIELDS;
            default -> 1;
        };
    }

    private static Optional<TowerSave> tower(String[] words) {
        Optional<TowerFactory.Type> type = Arrays.stream(TowerFactory.Type.values())
                .filter(candidate -> candidate.name().equals(words[1].toUpperCase(Locale.ROOT))).findFirst();
        return type.map(found -> new TowerSave(found,
                new Point(Integer.parseInt(words[2]), Integer.parseInt(words[3])),
                List.of(words).subList(TOWER_FIELDS, words.length),
                new TowerTally(Integer.parseInt(words[4]), Integer.parseInt(words[5]), Long.parseLong(words[6]),
                        Integer.parseInt(words[7]))));
    }

    /** A save's text read: the save, or the problem that stops it. */
    public record Read(Optional<GameSave> save, String problem) {

        static Read of(GameSave save) {
            return new Read(Optional.of(save), "");
        }

        static Read problem(String problem) {
            return new Read(Optional.empty(), problem);
        }
    }
}
