package td.ui.render;

/**
 * A missile in flight. {@code x}/{@code y} are already interpolated between the projectile's
 * previous and current tick position, the same contract {@link EnemyBodyDraw} follows.
 * {@code facingRadians} is its current direction of travel, derived from that same tick's
 * movement delta - unlike an enemy's facing (see {@code td/enemy/CLAUDE.md}), a missile moves
 * many pixels per tick rather than a fraction of one, so the delta is never degenerate.
 */
public record MissileDraw(Palette palette, float x, float y, double facingRadians) implements ProjectileDraw {
}
