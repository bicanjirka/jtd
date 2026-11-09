package td.save;

import td.damage.DamageMix;
import td.economy.EconomyState;
import td.tower.TowerFactory;
import td.tower.TowerTally;
import td.tower.TowerUse;
import td.wave.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * A {@link GameSave} as plain text, one fact a line, so a save can be read and edited by hand:
 * <pre>
 * level Curly Path
 * wave 17
 * economy 120 3400 5
 * seed -4116902350129335519
 * leaked 3 9
 * bonus 0 40
 * damage 1250000 480000
 * tower SNIPER 6 11 400 12 123456 40 paid=120 xp+=0.25 sniper.head.x.1 sniper.head.x.2
 * pin 4 7 9 7
 * </pre>
 * The economy line is credits, score and lives. {@code leaked} lists the 0-based rounds an enemy escaped, and a
 * {@code bonus} line is a round and the clear bonus it paid. {@code damage} is the physical and magic damage landed. A
 * tower line is its type, cell, XP, deeds, damage dealt, kills, then {@code key=value} facts (what it was bought for,
 * {@code unused=tower} for a tower no wave has run with, the fraction of XP carried), then the upgrades it owns in the
 * order bought, a {@code +} before each one bought since the last wave ran. A {@code pin} line is the cell of an
 * Obelisk and of the one its Ley Line is pinned to. Every line but level, wave and economy may be left out.
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
        if (save.seed() != 0L) {
            text.append("seed ").append(save.seed()).append('\n');
        }
        if (!save.history().leaked().isEmpty()) {
            text.append("leaked");
            save.history().leaked().stream().sorted().forEach(round -> text.append(' ').append(round));
            text.append('\n');
        }
        save.history().bonusesPaid().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(paid ->
                text.append("bonus ").append(paid.getKey()).append(' ').append(paid.getValue()).append('\n'));
        if (!save.damage().equals(DamageMix.none())) {
            text.append("damage ").append(save.damage().physical()).append(' ').append(save.damage().magic())
                    .append('\n');
        }
        for (TowerSave tower : save.towers()) {
            TowerTally tally = tower.tally();
            text.append("tower ").append(tower.type().name()).append(' ').append(tower.cell().x()).append(' ')
                    .append(tower.cell().y()).append(' ').append(tally.xp()).append(' ').append(tally.deeds())
                    .append(' ').append(tally.damageDealt()).append(' ').append(tally.kills());
            tower.paid().ifPresent(paid -> text.append(" paid=").append(paid));
            if (!tower.use().towerUsed()) {
                text.append(" unused=tower");
            }
            if (tally.xpCarried() != 0f) {
                text.append(" xp+=").append(tally.xpCarried());
            }
            tower.nodes().forEach(node -> text.append(' ')
                    .append(tower.use().towerUsed() && tower.use().unusedNodes().contains(node) ? "+" : "")
                    .append(node));
            text.append('\n');
        }
        for (LeyLinePin pin : save.pins()) {
            text.append("pin ").append(pin.caster().x()).append(' ').append(pin.caster().y()).append(' ')
                    .append(pin.host().x()).append(' ').append(pin.host().y()).append('\n');
        }
        return text.toString();
    }

    /** The save {@code text} describes, or what is wrong with it. */
    public static Read read(String text) {
        String level = "";
        int wave = 0;
        long seed = 0L;
        Optional<EconomyState> economy = Optional.empty();
        Set<Integer> leaked = new HashSet<>();
        Map<Integer, Integer> bonuses = new HashMap<>();
        DamageMix damage = DamageMix.none();
        List<TowerSave> towers = new ArrayList<>();
        List<LeyLinePin> pins = new ArrayList<>();
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
                    case "seed" -> seed = Long.parseLong(words[1]);
                    case "leaked" -> Arrays.stream(words).skip(1).map(Integer::parseInt).forEach(leaked::add);
                    case "bonus" -> bonuses.put(Integer.parseInt(words[1]), Integer.parseInt(words[2]));
                    case "damage" -> damage = new DamageMix(Long.parseLong(words[1]), Long.parseLong(words[2]));
                    case "pin" -> pins.add(new LeyLinePin(
                            new Point(Integer.parseInt(words[1]), Integer.parseInt(words[2])),
                            new Point(Integer.parseInt(words[3]), Integer.parseInt(words[4]))));
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
        return Read.of(GameSave.of(level, wave, economy.get(), towers).withSeed(seed)
                .withHistory(new RoundHistory(leaked, bonuses)).withDamage(damage).withPins(pins));
    }

    /** How many words a line starting with {@code key} has at least, the key included. */
    private static int fieldsOf(String key) {
        return switch (key) {
            case "wave", "seed" -> 2;
            case "bonus", "damage" -> 3;
            case "economy" -> 4;
            case "pin" -> 5;
            case "tower" -> TOWER_FIELDS;
            default -> 1;
        };
    }

    private static Optional<TowerSave> tower(String[] words) {
        Optional<TowerFactory.Type> type = Arrays.stream(TowerFactory.Type.values())
                .filter(candidate -> candidate.name().equals(words[1].toUpperCase(Locale.ROOT))).findFirst();
        if (type.isEmpty()) {
            return Optional.empty();
        }
        OptionalInt paid = OptionalInt.empty();
        float carried = 0f;
        boolean towerUsed = true;
        List<String> nodes = new ArrayList<>();
        Set<String> unused = new HashSet<>();
        for (String word : List.of(words).subList(TOWER_FIELDS, words.length)) {
            if (word.startsWith("paid=")) {
                paid = OptionalInt.of(Integer.parseInt(word.substring("paid=".length())));
            } else if (word.startsWith("xp+=")) {
                carried = Float.parseFloat(word.substring("xp+=".length()));
            } else if (word.equals("unused=tower")) {
                towerUsed = false;
            } else if (word.startsWith("+")) {
                nodes.add(word.substring(1));
                unused.add(word.substring(1));
            } else {
                nodes.add(word);
            }
        }
        TowerTally tally = TowerTally.of(Integer.parseInt(words[4]), Integer.parseInt(words[5]),
                Long.parseLong(words[6]), Integer.parseInt(words[7])).withXpCarried(carried);
        TowerSave tower = TowerSave.of(type.get(),
                new Point(Integer.parseInt(words[2]), Integer.parseInt(words[3])), nodes, tally)
                .withUse(new TowerUse(towerUsed, towerUsed ? unused : Set.copyOf(nodes)));
        return Optional.of(paid.isPresent() ? tower.withPaid(paid.getAsInt()) : tower);
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
