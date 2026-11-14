package td.ui.render;

/**
 * A tower's body, plus its range ring when {@code selected}, and the upgrade arrow when {@code upgradeReady}: it can
 * buy an upgrade or a tier's unlock now.
 */
public record TowerSpriteDraw(TowerLook look, int boardX, int boardY, boolean selected,
                              float centerX, float centerY, float rangeReal, boolean upgradeReady) {
}
