package td.ui.render;

/**
 * A faceted ice-crystal cluster overlaid on a frozen enemy's body - additive to the ordinary
 * {@code STATUS_MARKER_FREEZE} marker-row dot, not a replacement for it, so a frozen enemy
 * reads apart from a merely slowed one even in a crowd. Static, not timed, since it reflects an
 * ongoing state ({@code EffectKind.FREEZE} being active) rather than a one-shot transition -
 * shaped like {@link EnemyRingDraw}'s shield bubble, just a different fixed geometry on the
 * renderer side.
 */
public record IceCrystalDraw(Palette palette, float centerX, float centerY, float scale) implements EnemyOverlayDraw {
}
