package td.ui.render;

/** What is drawn in the body layer: a live body, a fading corpse, or what falls beneath a body. */
public sealed interface EnemyDraw permits EnemyBodyDraw, EnemyFadeDraw, EnemyEmissionDraw {
}
