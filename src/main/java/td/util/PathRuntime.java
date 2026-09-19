package td.util;

import td.wave.Path;
import td.wave.PathColor;
import td.wave.Wave;

import java.util.List;

/**
 * One lane's runtime state, correlated the same way {@link LoadedLevel} itself is: a path's
 * built geometry, the waves bound to it, and its on-board color travel together as one value -
 * see {@link td.wave.PathDefinition}, the authored form this is built from.
 */
public record PathRuntime(Path path, List<Wave> waves, PathColor color) {

    public PathRuntime {
        waves = List.copyOf(waves);
    }
}
