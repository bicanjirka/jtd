package td.ui.render;

/** A tower's transient effect: a beam, a splash fill, a firing pulse, an upgrade aura, or a flame cone. */
public sealed interface TowerEffectDraw permits BeamDraw, SplashDraw, PulseDraw, AuraDraw, ConeDraw {
}
