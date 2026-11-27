package td.ui.render;

import java.util.List;

/**
 * An enemy in a Beacon's light, lit from the top left as the board is: {@code light} (0..1) is how far inside the
 * edge it stands, {@code seconds} the animation clock and {@code phase} what keeps neighbours out of step.
 * {@code fringe} (Prism) is the marks the light carries, as the colours its light is split into. In a
 * {@code crowded} frame only the pool and the rim are drawn.
 */
public record LitEnemyDraw(float x, float y, float cellPixels, float light, float seconds, float phase,
                           List<Palette> fringe, boolean crowded) implements TowerEffectDraw {

    public LitEnemyDraw {
        fringe = List.copyOf(fringe);
    }
}
