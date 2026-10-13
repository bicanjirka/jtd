package td.ui.render;

/**
 * A decoration around a live enemy: shield bubble, aura ring, pulse, trait marker, ice, rune or miss mark. Separate
 * from {@link EnemyDraw}, which is only a body or a corpse.
 */
public sealed interface EnemyOverlayDraw permits EnemyRingDraw, EffectPulseDraw, TraitMarkerDraw, IceCrystalDraw,
        HexRuneDraw, MissMarkDraw {
}
