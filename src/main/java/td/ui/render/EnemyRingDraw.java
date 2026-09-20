package td.ui.render;

/**
 * A static, thin stroked ring centred on an enemy - a shield bubble at body radius, or a
 * support-aura ring at the radius an ability actually projects to. Shaped like
 * {@link TowerEffectDraw}'s own {@code AuraDraw}, which this deliberately mirrors.
 */
public record EnemyRingDraw(Palette palette, float centerX, float centerY, float radius, float alpha) implements EnemyOverlayDraw {
}
