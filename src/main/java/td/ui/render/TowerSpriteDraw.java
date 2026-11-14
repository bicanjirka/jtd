package td.ui.render;

/**
 * A tower: its look standing on the foundation of {@code tier}, its head (for a look that aims) turned to
 * {@code headingRadians} ({@code 0} along {@code +X}, toward {@code +Y}), its range ring when {@code selected}, and the
 * upgrade arrow when {@code upgradeReady}: it can buy an upgrade or a tier's unlock now. While its tier rises,
 * {@code riseProgress} runs 0..1 as the new foundation spreads over the one of {@code risenFrom}; {@code -1} outside it.
 */
public record TowerSpriteDraw(TowerLook look, int tier, int boardX, int boardY, boolean selected,
                              float centerX, float centerY, float rangeReal, float headingRadians,
                              boolean upgradeReady, int risenFrom, float riseProgress) {
}
