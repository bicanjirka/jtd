package td.ui.render;

/** A live enemy's body, position already interpolated. {@code cloakProgress} runs from 0 (solid) to 1 (cloaked). */
public record EnemyBodyDraw(EnemyLook look, float x, float y, double facingRadians, float scale,
                            float cloakProgress) implements EnemyDraw {
}
