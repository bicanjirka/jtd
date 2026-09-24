package td.wave;

import java.util.List;

/** The {@link Path} implementation: an immutable copy of its points. */
public record PathNormal(List<Vec2> points) implements Path {

    public PathNormal {
        points = List.copyOf(points);
    }
}
