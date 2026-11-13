package td.balance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What damage costs, read from the tower the paper tools hold every other tower against: the damage a second one
 * tower lands on one enemy bare and at each tier, and the credits that tier has cost. It prices a defence no tower in
 * the code has to stand for, so waves and bounties can be judged while the towers are still being redone.
 *
 * @param reachCells how far the tower reaches
 * @param steps      the tower bare, then at each tier in order
 */
record PaperYardstick(double reachCells, List<Step> steps) {

    /** Where the paper tools keep their reference tower. */
    static final Path STORED = Path.of(".claude", "skills", "jtd-balance-tools", "reference.json");

    PaperYardstick {
        steps = List.copyOf(steps);
    }

    /**
     * One state of the tower.
     *
     * @param cost            every credit spent on it so far
     * @param damagePerSecond what it lands on one enemy that stays in reach
     */
    record Step(double cost, double damagePerSecond) {
    }

    static PaperYardstick read(Path file) {
        String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No paper reference at " + file + ": save one with the tree calculator", e);
        }
        double price = number(json, "price");
        double bareMix = number(json, "bare_mix");
        double bare = number(json, "damage") / number(json, "interval");
        List<Step> steps = new ArrayList<>(List.of(new Step(price, bare)));
        // A tier's mix is damage per credit of the tower alone, in the units the bare mix is the whole tower's in.
        Matcher tier = Pattern.compile("\\{\\s*\"mix\":\\s*([0-9.]+),[^}]*?\"cost\":\\s*([0-9.]+)").matcher(json);
        while (tier.find()) {
            double mix = Double.parseDouble(tier.group(1));
            steps.add(new Step(Double.parseDouble(tier.group(2)), bare * mix * price / bareMix));
        }
        return new PaperYardstick(number(json, "range"), steps);
    }

    private static double number(String json, String key) {
        Matcher found = Pattern.compile("\"" + key + "\":\\s*([0-9.]+)").matcher(json);
        if (!found.find()) {
            throw new IllegalStateException("The paper reference has no '" + key + "'");
        }
        return Double.parseDouble(found.group(1));
    }
}
