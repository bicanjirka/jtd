package td.tower.mortar;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The long-run share of each kind among the shells a magazine fires. A slot's kind is settled once the slot behind it
 * is drawn, since with Fusion that draw can turn a Heavy into a Nuke. The shares are counted over the later half of a
 * long walk of that chain, since Rich Mix with two kinds makes it alternate rather than settle.
 */
final class MagazineOdds {

    /** Steps the chain is walked: far past where its shares stop moving for any weights the tree gives. */
    private static final int STEPS = 400;

    private MagazineOdds() {
    }

    static Map<ShellKind, Float> of(MagazineSpec spec) {
        ShellKind[] kinds = ShellKind.values();
        double[] waiting = new double[kinds.length];
        waiting[ShellKind.NORMAL.ordinal()] = 1.0;
        double[] fired = new double[kinds.length];
        for (int step = 0; step < STEPS; step++) {
            double[] next = new double[kinds.length];
            double[] firedNow = new double[kinds.length];
            for (ShellKind last : kinds) {
                double share = waiting[last.ordinal()];
                if (share == 0.0) {
                    continue;
                }
                for (ShellKind drawn : kinds) {
                    double chance = share * spec.chanceOf(drawn, Optional.of(last));
                    if (chance == 0.0) {
                        continue;
                    }
                    if (spec.fuses() && last == ShellKind.HEAVY && drawn == ShellKind.HEAVY) {
                        next[ShellKind.NUKE.ordinal()] += chance;
                    } else {
                        firedNow[last.ordinal()] += chance;
                        next[drawn.ordinal()] += chance;
                    }
                }
            }
            waiting = next;
            if (step >= STEPS / 2) {
                for (int i = 0; i < fired.length; i++) {
                    fired[i] += firedNow[i];
                }
            }
        }
        double total = 0.0;
        for (double share : fired) {
            total += share;
        }
        Map<ShellKind, Float> chances = new EnumMap<>(ShellKind.class);
        for (ShellKind kind : kinds) {
            chances.put(kind, total == 0.0 ? 0f : (float) (fired[kind.ordinal()] / total));
        }
        return chances;
    }
}
