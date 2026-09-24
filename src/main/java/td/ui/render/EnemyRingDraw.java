package td.ui.render;

/** A thin static ring centred on an enemy: a shield bubble or an aura's reach. */
public record EnemyRingDraw(Palette palette, float centerX, float centerY, float radius, float alpha) implements EnemyOverlayDraw {
}
