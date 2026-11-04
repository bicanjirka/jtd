package td.ui.render;

/**
 * A live enemy's body, position already interpolated. {@code badge} is drawn upright regardless of
 * facing. {@code cloakProgress} runs from 0 (solid) to 1 (cloaked).
 */
public record EnemyBodyDraw(EnemyLook look, float x, float y, double facingRadians, float scale,
                            RankBadge badge, float cloakProgress) implements EnemyDraw {
}
