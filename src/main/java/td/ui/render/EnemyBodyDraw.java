package td.ui.render;

/**
 * A live enemy's body, position already interpolated. {@code badge} is drawn upright regardless of
 * facing. {@code cloakProgress} runs from 0 (solid) to 1 (cloaked). {@code flicker} is 0 for a
 * steady body and 1 for the dimmed half of a flicker.
 */
public record EnemyBodyDraw(EnemyLook look, float x, float y, double facingRadians, float scale,
                            RankBadge badge, float cloakProgress, float flicker) implements EnemyDraw {

    public EnemyBodyDraw(EnemyLook look, float x, float y, double facingRadians, float scale,
                         RankBadge badge, float cloakProgress) {
        this(look, x, y, facingRadians, scale, badge, cloakProgress, 0f);
    }
}
