package td.ui.render;

/**
 * A tower: its look standing on the foundation of {@code tier} in {@code pose}, its head (for a look that aims) turned
 * to {@code headingRadians} ({@code 0} along {@code +X}, toward {@code +Y}), its range ring when {@code selected}, and
 * the upgrade arrow when {@code upgradeReady}: it can buy an upgrade or a tier's unlock now.
 * <p>
 * {@code riseProgress} runs 0..1 as the new tier's foundation grows out of the one of {@code risenFrom}, and
 * {@code buildProgress} 0..1 as a tower just placed settles onto its cell; both rest at 1. {@code lifeSeconds} is the
 * wall clock the tower's own lights breathe on, offset so neighbours never breathe as one.
 */
public record TowerSpriteDraw(TowerLook look, int tier, TowerPose pose, int boardX, int boardY, boolean selected,
                              float centerX, float centerY, float rangeReal, float headingRadians,
                              boolean upgradeReady, int risenFrom, float riseProgress, float buildProgress,
                              float lifeSeconds) {

    /** A settled tower at rest: nothing rising, nothing held. */
    public static TowerSpriteDraw still(TowerLook look, int tier, int boardX, int boardY, float centerX, float centerY,
                                        float rangeReal) {
        return new TowerSpriteDraw(look, tier, TowerPose.idle(), boardX, boardY, false, centerX, centerY, rangeReal, 0f,
                false, tier, 1f, 1f, 0f);
    }
}
