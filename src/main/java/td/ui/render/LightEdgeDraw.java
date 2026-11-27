package td.ui.render;

import java.util.List;

/**
 * The Beacon's light edge as it stirs, {@code progress} (0..1) through its moment: it grows out of the lantern at
 * ({@code centerX}, {@code centerY}) to {@code radius}, glows and fades. With {@code arcs} (Prism) the edge takes the
 * colour of the towers it lights.
 */
public record LightEdgeDraw(float centerX, float centerY, float radius, float progress,
                            List<LightArc> arcs) implements TowerEffectDraw {

    public LightEdgeDraw {
        arcs = List.copyOf(arcs);
    }
}
