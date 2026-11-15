package td.ui.render;

/** A faint dashed ring of frost on the ground round a frozen enemy, out to how far its Ice Wake chills; {@code unit} is a cell. */
public record IceWakeDraw(float centerX, float centerY, float radius, float unit) implements TowerEffectDraw {
}
