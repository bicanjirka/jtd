package td.util;

import td.wave.Path;
import td.wave.PathColor;
import td.wave.Wave;

import java.util.List;

/** One lane at runtime: built path, its waves and its colour, travelling together. */
public record PathRuntime(Path path, List<Wave> waves, PathColor color) {

    public PathRuntime {
        waves = List.copyOf(waves);
    }
}
