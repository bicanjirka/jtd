package td.fixtures;

import td.tower.mortar.Magazine;
import td.tower.mortar.ShellKind;

import java.util.ArrayList;
import java.util.List;

/** Magazines to load into a Mortar, so a test fires the shells it names rather than the ones a draw would. */
public final class MagazineFixtures {

    private MagazineFixtures() {
    }

    /** {@code front} first, in that order, and Normal shells behind them. */
    public static Magazine frontedBy(ShellKind... front) {
        List<ShellKind> shells = new ArrayList<>(List.of(front));
        while (shells.size() < Magazine.SIZE) {
            shells.add(ShellKind.NORMAL);
        }
        return new Magazine(shells);
    }
}
