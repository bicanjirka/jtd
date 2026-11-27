package td.ui.render;

/**
 * Anything drawn on the board over its ground, painted in the one order {@link RenderFrame#scene()} holds: by how high
 * it stands, then from the far edge to the near one.
 */
public sealed interface BoardDraw permits EnemyDraw, EnemyOverlayDraw, TowerEffectDraw, ProjectileDraw, TowerSpriteDraw,
        TowerGroundDraw, UpgradeArrowDraw, CritSparkDraw, ZoneDetailDraw, ZoneGroundPieceDraw, BlastGroundDraw {
}
