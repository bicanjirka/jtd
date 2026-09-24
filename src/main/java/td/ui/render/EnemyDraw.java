package td.ui.render;

/** An enemy is exactly one of a live body or a fading corpse. */
public sealed interface EnemyDraw permits EnemyBodyDraw, EnemyFadeDraw {
}
