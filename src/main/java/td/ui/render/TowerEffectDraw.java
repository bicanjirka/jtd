package td.ui.render;

/** A tower's transient effect: a beam, a splash fill, a firing pulse, or an upgrade aura. */
public sealed interface TowerEffectDraw permits BeamDraw, SplashDraw, PulseDraw, AuraDraw {
}
