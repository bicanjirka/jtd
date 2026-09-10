package td.ui;

import td.enemy.EnemyMob;
import td.tower.TowerFour;
import td.tower.TowerOne;
import td.tower.TowerThree;
import td.tower.TowerTwo;
import td.tower.TowerUpgrade;
import td.tower.TowerVisitor;
import td.ui.render.AuraDraw;
import td.ui.render.BeamDraw;
import td.ui.render.Palette;
import td.ui.render.PulseDraw;
import td.ui.render.SplashDraw;
import td.ui.render.TowerEffectDraw;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes each tower's transient targeting effect (beam, splash, pulse, or - for the upgrade
 * tower, which never attacks - an aura) as {@link TowerEffectDraw} commands. One visit method
 * per concrete tower type, since - unlike the sprite - these genuinely differ by tower.
 */
public final class TowerEffectFrameBuilder implements TowerVisitor<Void> {

    // Two rings half a period apart read as a continuous "breathing" aura rather than one ring
    // blinking in and out; purely cosmetic, so this is a function of elapsed time like the
    // spinning turret heads, not tick-based domain state.
    private static final double AURA_PERIOD_SECONDS = 1.8;
    private static final double[] AURA_PHASE_OFFSETS = {0.0, 0.5};

    private final List<TowerEffectDraw> draws = new ArrayList<>();
    private final int gameTime;
    private final double animationSeconds;

    public TowerEffectFrameBuilder(int gameTime, double animationSeconds) {
        this.gameTime = gameTime;
        this.animationSeconds = animationSeconds;
    }

    public List<TowerEffectDraw> build() {
        return this.draws;
    }

    private static float beamWidth(float coolDownFraction) {
        return 3.0f * coolDownFraction;
    }

    public Void visitTowerOne(TowerOne tower) {
        EnemyMob target = tower.getCurrentTarget();
        if (target != null) {
            this.draws.add(new BeamDraw(Palette.TOWER_ONE_BEAM, tower.getX(), tower.getY(),
                    (float) target.getX(), (float) target.getY(), beamWidth(tower.getCoolDownFraction())));
        }
        return null;
    }

    public Void visitTowerTwo(TowerTwo tower) {
        EnemyMob target = tower.getPrimaryTarget();
        if (target != null) {
            this.draws.add(new BeamDraw(Palette.TOWER_TWO_BEAM, tower.getX(), tower.getY(),
                    (float) target.getX(), (float) target.getY(), beamWidth(tower.getCoolDownFraction())));
            for (EnemyMob splashTarget : tower.getSplashTargets()) {
                this.draws.add(new BeamDraw(Palette.TOWER_TWO_SPLASH_LINE, (float) target.getX(), (float) target.getY(),
                        (float) splashTarget.getX(), (float) splashTarget.getY(), 1.0f));
            }
        }
        if (tower.isSplashVisible()) {
            this.draws.add(new SplashDraw(Palette.TOWER_TWO_SPLASH_FILL,
                    tower.getSplashCenterX(), tower.getSplashCenterY(), tower.getSpreadRadius()));
        }
        return null;
    }

    /**
     * A fading beam to each enemy the scan has recently caught. The scan line itself is not
     * drawn out across the board - the tower's own turret head shows where it is pointing,
     * and a full range-length beam swinging around read as an attack rather than as a scan.
     */
    public Void visitTowerThree(TowerThree tower) {
        for (TowerThree.SonarHit hit : tower.getRecentHits()) {
            this.draws.add(new BeamDraw(Palette.TOWER_THREE_BEAM, tower.getX(), tower.getY(),
                    hit.x(), hit.y(), beamWidth(tower.hitFade(hit, this.gameTime))));
        }
        return null;
    }

    public Void visitTowerFour(TowerFour tower) {
        if (tower.isFiring()) {
            this.draws.add(new PulseDraw(Palette.TOWER_FOUR_PULSE, tower.getX(), tower.getY(), tower.getRangeReal()));
        }
        return null;
    }

    public Void visitTowerUpgrade(TowerUpgrade tower) {
        float maxRadius = tower.getRangeReal();
        for (double phaseOffset : AURA_PHASE_OFFSETS) {
            double phase = phaseFraction(this.animationSeconds, phaseOffset);
            this.draws.add(new AuraDraw(Palette.TOWER_UPGRADE_AURA, tower.getX(), tower.getY(),
                    (float) (phase * maxRadius), (float) (1.0 - phase)));
        }
        return null;
    }

    /** Where in a looping {@code [0, 1)} cycle {@code seconds} sits, offset by {@code phaseOffset}. */
    private static double phaseFraction(double seconds, double phaseOffset) {
        double t = (seconds / AURA_PERIOD_SECONDS + phaseOffset) % 1.0;
        return t < 0 ? t + 1.0 : t;
    }
}
