package td.ui.render;

/**
 * What is drawn around a live enemy: effects, rings and pulses, then its badge, runes, dots and bars. Separate from
 * {@link EnemyDraw}, which is only a body or a corpse.
 */
public sealed interface EnemyOverlayDraw permits EnemyRingDraw, EffectPulseDraw, EnemyIceDraw,
        HexRuneDraw, MissMarkDraw, EnemyBarsDraw, EnemyDotsDraw, RankBadgeDraw, EnemyEggDraw, EnemyBurstDraw, EnemyEmissionDraw, EnemyTintDraw {
}
