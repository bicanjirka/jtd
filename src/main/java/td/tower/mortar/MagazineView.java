package td.tower.mortar;

import td.tower.DrawnShares;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A Mortar's magazine as its pane shows it: the shells in the order it sends them, each with the payload it carries,
 * the Normals a volley sneaks in among them; how many front ones the next firing sends; and the share of its shots
 * each kind it can draw comes to, in the kinds' order, and what each of those kinds does, a Normal also as it carries
 * each payload.
 */
public record MagazineView(List<LinedUpShell> shells, int loading, Map<ShellKind, Float> chances,
                           List<ShellProfile> profiles) {

    public MagazineView {
        shells = List.copyOf(shells);
        chances = DrawnShares.of(ShellKind.class, chances);
        profiles = List.copyOf(profiles);
    }

    /** What {@code kind} does, carrying nothing; empty for a kind it no longer loads, still in the magazine until the next tick. */
    public Optional<ShellProfile> profileOf(ShellKind kind) {
        return this.profileOf(new HeldShell(kind, Optional.empty()));
    }

    /** What {@code shell} does, with the payload it carries; empty for a kind it no longer loads. */
    public Optional<ShellProfile> profileOf(HeldShell shell) {
        return this.profiles.stream()
                .filter(profile -> profile.kind() == shell.kind() && profile.carried().equals(shell.carried()))
                .findFirst();
    }
}
