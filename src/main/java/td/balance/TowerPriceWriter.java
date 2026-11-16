package td.balance;

import td.tower.TowerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes the prices the tower report suggests into the towers' sources: each judged tower's list price and the price
 * of each of its nodes, where the report calls the price off. A price the report calls right is left, so writing,
 * measuring and judging again settles instead of chasing the bench's noise. It changes a number and nothing else.
 *
 * <p>A node is found by its name in the source of its tower; one that is not there is printed, to be set by hand.
 */
public final class TowerPriceWriter {

    private static final Path SOURCES = Path.of("src", "main", "java");
    /** What the verdict of a price the report wants changed says; a price near enough its suggestion is left. */
    private static final String OFF = "WARN price";
    private static final Pattern TOWER_PRICE = Pattern.compile("(int PRICE = )\\d+;");
    private static final int TOWER = 0;
    private static final int NODE = 1;
    private static final int NAME = 2;
    private static final int PRICE = 4;
    private static final int SUGGESTED = 7;
    /** As near its suggestion as the report calls right. */
    private static final int NEAR_CREDITS = 2;

    private TowerPriceWriter() {
    }

    /** Applies the stored price sheet. */
    public static void main(String[] args) {
        List<List<String>> prices = ReportPage.load(TowerReport.PRICE_SHEET);
        int changed = 0;
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            Path file = SOURCES.resolve(TowerBench.classOf(type).getName().replace('.', '/') + ".java");
            String source = read(file);
            String written = source;
            for (List<String> row : prices.subList(1, prices.size())) {
                if (!row.get(TOWER).equals(type.name()) || !isOff(row)) {
                    continue;
                }
                int price = Integer.parseInt(row.get(SUGGESTED));
                String next = row.get(NODE).isEmpty() ? towerPriced(written, price) : nodePriced(written, row.get(NAME), price);
                boolean found = !next.equals(written);
                System.out.printf(Locale.ROOT, "%-9s %-22s %4s -> %4d%s%n", type.displayName(), row.get(NAME),
                        row.get(PRICE), price, found ? "" : "   NOT FOUND in " + file.getFileName() + ": set it by hand");
                changed += found ? 1 : 0;
                written = next;
            }
            if (!written.equals(source)) {
                write(file, written);
            }
        }
        System.out.println(changed + " prices written. Rerun TowerBench and TowerReport to see what they do.");
    }

    /**
     * Whether {@code row}'s price is to be written: the report calls it off, or the node's verdict is about what it
     * does and so says nothing of its price, which a cluster or a floor still sets.
     */
    static boolean isOff(List<String> row) {
        String verdict = row.getLast();
        if (verdict.contains(OFF)) {
            return true;
        }
        String suggested = row.get(SUGGESTED);
        if (!(verdict.contains("BREACH") || verdict.contains("UNMEASURED")) || suggested.isEmpty()
                || !suggested.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int to = Integer.parseInt(suggested);
        return Math.abs(Integer.parseInt(row.get(PRICE)) - to) > Math.max(NEAR_CREDITS, TowerReport.PRICE_BAND * to);
    }

    /** {@code source} with its tower's list price set to {@code price}. */
    static String towerPriced(String source, int price) {
        return TOWER_PRICE.matcher(source).replaceFirst("$1" + price + ";");
    }

    /** {@code source} with the node named {@code name} priced at {@code price}; the same text when it has no such node. */
    static String nodePriced(String source, String name, int price) {
        Pattern node = Pattern.compile("(UpgradeNode\\.of\\([^,()]+, \"" + Pattern.quote(name) + "\", )\\d+\\)");
        return node.matcher(source).replaceFirst("$1" + Matcher.quoteReplacement(price + ")"));
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No source for a tower at " + file, e);
        }
    }

    private static void write(Path file, String text) {
        try {
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
