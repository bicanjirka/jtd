package td.ui.render;

/**
 * An alive enemy's body. {@code x}/{@code y} are already interpolated between the
 * mob's previous and current tick position - the backend just draws them.
 */
public record EnemyBodyDraw(Palette palette, float x, float y, double facingRadians, float scale,
                             float healthFraction) implements EnemyDraw {
}
