package td.ui.render;

/** A tower's transient targeting effect: a beam, a splash fill, or a firing pulse. */
public sealed interface TowerEffectDraw permits BeamDraw, SplashDraw, PulseDraw {
}
