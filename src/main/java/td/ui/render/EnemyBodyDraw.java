package td.ui.render;

/**
 * A live enemy's body, position already interpolated. {@code badge} is drawn upright regardless of
 * facing. {@code cloakProgress} runs from 0 (solid) to 1 (cloaked).
 */
public record EnemyBodyDraw(Palette palette, float x, float y, double facingRadians, float scale,
                            float healthFraction, RankBadge badge, float cloakProgress) implements EnemyDraw {
}
