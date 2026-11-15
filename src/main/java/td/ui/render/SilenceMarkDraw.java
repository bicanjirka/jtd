package td.ui.render;

/** A small struck-out speech mark over an enemy that cannot cast, {@code size} pixels wide. */
public record SilenceMarkDraw(float x, float y, float size, float alpha) implements EnemyOverlayDraw {
}
