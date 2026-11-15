package td.save;

import td.tower.mortar.ShellKind;
import td.wave.Point;

import java.util.List;

/** The shells a Mortar had ready, front first, by the cell it stands on. */
public record MagazineLoad(Point mortar, List<ShellKind> shells) {

    public MagazineLoad {
        shells = List.copyOf(shells);
    }
}
