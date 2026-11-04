package td.ui.render;

/**
 * A decoration around a live enemy: shield bubble, aura ring, pulse, trait marker, ice, rune, miss mark or bars. Separate
 * from {@link EnemyDraw}, which is only a body or a corpse.
 */
public sealed interface EnemyOverlayDraw permits EnemyRingDraw, EffectPulseDraw, IceCrystalDraw,
        HexRuneDraw, MissMarkDraw, EnemyBarsDraw {
}
