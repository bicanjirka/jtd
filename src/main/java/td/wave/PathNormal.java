package td.wave;

import java.util.List;

public record PathNormal(List<Vec2> points) implements Path {

    public PathNormal {
        points = List.copyOf(points);
    }
}
