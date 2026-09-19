package td.ui.render;

/**
 * An alive enemy's body. {@code x}/{@code y} are already interpolated between the
 * mob's previous and current tick position - the backend just draws them. {@code badge} is
 * this mob's rank glyph ({@link RankBadge#NONE} for Grunt, the unranked default) - drawn upright,
 * never rotated with {@code facingRadians}, since an insignia reads best right-side up regardless
 * of which way its wearer is facing.
 */
public record EnemyBodyDraw(Palette palette, float x, float y, double facingRadians, float scale,
                            float healthFraction, RankBadge badge) implements EnemyDraw {
}
