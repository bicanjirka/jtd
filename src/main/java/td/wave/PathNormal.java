package td.wave;

import java.util.List;

/** The only {@link Path} implementation - an immutable copy of the points it is given. */
public record PathNormal(List<Vec2> points) implements Path {

    public PathNormal {
        points = List.copyOf(points);
    }
}
