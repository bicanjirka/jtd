package td.ui.render;

/**
 * A shell in flight. {@code x}/{@code y} are already interpolated between the projectile's
 * previous and current tick position, the same contract {@link EnemyBodyDraw} follows. No
 * facing: a cannonball flies a fixed straight line and never re-aims, so its shape needs no
 * heading to read correctly.
 */
public record CannonballDraw(Palette palette, float x, float y) implements ProjectileDraw {
}
