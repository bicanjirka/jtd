package td.ui.render;

/** The {@code $} in a tower's corner: some of it sells back in full, since no wave has run with it yet. */
public record RefundMarkDraw(float x, float y, float height) implements TowerEffectDraw {
}
