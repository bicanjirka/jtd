package td.ui.render;

/**
 * A live enemy's body, position already interpolated. {@code badge} is drawn upright regardless of
 * facing. {@code cloakProgress} runs from 0 (solid) to 1 (cloaked). {@code flicker} is 0 for a
 * steady body and 1 for the dimmed half of a flicker.
 */
public record EnemyBodyDraw(Palette palette, float x, float y, double facingRadians, float scale,
                            float healthFraction, RankBadge badge, float cloakProgress,
                            float flicker) implements EnemyDraw {

    public EnemyBodyDraw(Palette palette, float x, float y, double facingRadians, float scale,
                         float healthFraction, RankBadge badge, float cloakProgress) {
        this(palette, x, y, facingRadians, scale, healthFraction, badge, cloakProgress, 0f);
    }
}
