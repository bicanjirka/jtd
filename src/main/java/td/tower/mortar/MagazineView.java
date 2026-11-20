package td.tower.mortar;

import td.tower.DrawnShares;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A Mortar's magazine as its pane shows it: the shells front first, how many of them one firing loads, and the share
 * of its shots each kind it can draw comes to, in the kinds' order, and what each of those kinds does.
 */
public record MagazineView(List<ShellKind> shells, int loading, Map<ShellKind, Float> chances,
                           List<ShellProfile> profiles) {

    public MagazineView {
        shells = List.copyOf(shells);
        chances = DrawnShares.of(ShellKind.class, chances);
        profiles = List.copyOf(profiles);
    }

    /** What {@code kind} does; empty for a kind it no longer loads, still in the magazine until the next tick. */
    public Optional<ShellProfile> profileOf(ShellKind kind) {
        return this.profiles.stream().filter(profile -> profile.kind() == kind).findFirst();
    }
}
