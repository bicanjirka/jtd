package td.ui.render;

/**
 * A persistent or timed decoration drawn around/above a live enemy, beyond its body and its
 * timed status-marker row - a shield bubble, a support-aura ring, a gain/loss pulse. Kept as its
 * own sealed hierarchy (not a fourth {@link EnemyDraw} permitted type) for the same reason a
 * status marker is its own {@code RenderFrame} list: {@link EnemyDraw}'s contract is "an enemy is
 * either an alive body or a fading corpse," and an overlay is neither on its own - see
 * {@code EnemyFrameBuilder.buildOverlays}.
 */
public sealed interface EnemyOverlayDraw permits EnemyRingDraw, EffectPulseDraw, TraitMarkerDraw {
}
