package td.fixtures;

import td.tower.mortar.Magazine;
import td.tower.mortar.ShellKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Magazines to load into a Mortar, so a test fires the shells it names rather than the ones a draw would. */
public final class MagazineFixtures {

    private MagazineFixtures() {
    }

    /** {@code front} first, in that order, and Normal shells behind them. */
    public static Magazine frontedBy(ShellKind... front) {
        return new Magazine(filled(front));
    }

    /** {@code front} first, right behind a {@code lastFired} shell, and Normal shells behind them. */
    public static Magazine firedAfter(ShellKind lastFired, ShellKind... front) {
        return new Magazine(filled(front), Optional.of(lastFired));
    }

    private static List<ShellKind> filled(ShellKind... front) {
        List<ShellKind> shells = new ArrayList<>(List.of(front));
        while (shells.size() < Magazine.SIZE) {
            shells.add(ShellKind.NORMAL);
        }
        return shells;
    }
}
