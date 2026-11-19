package td.tower.mortar;

import td.tower.DrawnShares;

import java.util.List;
import java.util.Map;

/**
 * A Mortar's magazine as its pane shows it: the shells front first, how many of them one firing loads, and the share
 * of its shots each kind it can draw comes to, in the kinds' order.
 */
public record MagazineView(List<ShellKind> shells, int loading, Map<ShellKind, Float> chances) {

    public MagazineView {
        shells = List.copyOf(shells);
        chances = DrawnShares.of(ShellKind.class, chances);
    }
}
