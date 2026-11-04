package td.ui;

import java.awt.Shape;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A shape built once per scale, since a body built from {@code Area}s costs far more to build than to paint. An enemy
 * that pulses asks for one scale per tick of its cycle, so a look holds few; what is remembered starts over past a
 * limit, should a look ever ask for more, rather than growing. The shapes it hands out are shared: draw them, never
 * change them.
 */
final class RememberedShape implements EnemyLooks.ScaledShape {

    /** Far more scales than one look shows at once: its sizes by rank and shape, and a pulse's steps. */
    static final int MOST_SCALES = 256;

    private final EnemyLooks.ScaledShape shape;
    private final Map<Float, Shape> byScale = new ConcurrentHashMap<>();

    RememberedShape(EnemyLooks.ScaledShape shape) {
        this.shape = shape;
    }

    @Override
    public Shape at(float scale) {
        Shape remembered = this.byScale.get(scale);
        if (remembered != null) {
            return remembered;
        }
        if (this.byScale.size() >= MOST_SCALES) {
            this.byScale.clear();
        }
        Shape built = this.shape.at(scale);
        this.byScale.put(scale, built);
        return built;
    }
}
