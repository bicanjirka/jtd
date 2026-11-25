package td.save;

import td.tower.mortar.ShellKind;
import td.wave.Point;

import java.util.List;
import java.util.Optional;

/** The shells a Mortar had ready, front first, and the one it fired last, by the cell it stands on. */
public record MagazineLoad(Point mortar, List<ShellKind> shells, Optional<ShellKind> lastFired) {

    public MagazineLoad {
        shells = List.copyOf(shells);
    }
}
