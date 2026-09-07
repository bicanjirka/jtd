package td.ui.render;

/** An enemy is either an alive body or a fading corpse - never both, never neither. */
public sealed interface EnemyDraw permits EnemyBodyDraw, EnemyFadeDraw {
}
