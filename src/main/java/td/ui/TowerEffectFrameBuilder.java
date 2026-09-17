package td.ui;

import td.enemy.EnemyMob;
import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.PulseTower;
import td.tower.MortarTower;
import td.tower.SniperTower;
import td.tower.SeekerTower;
import td.tower.SonarTower;
import td.tower.SplashTower;
import td.tower.TowerVisitor;
import td.ui.render.AuraDraw;
import td.ui.render.BeamDraw;
import td.ui.render.ConeDraw;
import td.ui.render.Palette;
import td.ui.render.PulseDraw;
import td.ui.render.SplashDraw;
import td.ui.render.TowerEffectDraw;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes each tower's transient targeting effect (beam, splash, pulse, cone, or - for the
 * aura tower, which never attacks - a pulsing ring) as {@link TowerEffectDraw} commands. One
 * visit method per concrete tower type, since - unlike the sprite - these genuinely differ by
 * tower.
 */
public final class TowerEffectFrameBuilder implements TowerVisitor<Void> {

    // Two rings half a period apart read as a continuous "breathing" aura rather than one ring
    // blinking in and out; purely cosmetic, so this is a function of elapsed time like the
    // spinning turret heads, not tick-based domain state.
    private static final double AURA_PERIOD_SECONDS = 1.8;
    private static final double[] AURA_PHASE_OFFSETS = {0.0, 0.5};
    private static final float CINDER_CONE_ALPHA = 0.35f;

    private final List<TowerEffectDraw> draws = new ArrayList<>();
    private final int gameTime;
    private final double interpolationAlpha;
    private final double animationSeconds;

    public TowerEffectFrameBuilder(int gameTime, double interpolationAlpha, double animationSeconds) {
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
        this.animationSeconds = animationSeconds;
    }

    public List<TowerEffectDraw> build() {
        return this.draws;
    }

    private static float beamWidth(float coolDownFraction) {
        return 3.0f * coolDownFraction;
    }

    public Void visitSniperTower(SniperTower tower) {
        EnemyMob target = tower.getCurrentTarget();
        if (target != null) {
            this.draws.add(new BeamDraw(Palette.TOWER_SNIPER_BEAM, tower.getX(), tower.getY(),
                    (float) target.getX(), (float) target.getY(), beamWidth(tower.getCoolDownFraction())));
        }
        return null;
    }

    public Void visitSplashTower(SplashTower tower) {
        EnemyMob target = tower.getPrimaryTarget();
        if (target != null) {
            this.draws.add(new BeamDraw(Palette.TOWER_SPLASH_BEAM, tower.getX(), tower.getY(),
                    (float) target.getX(), (float) target.getY(), beamWidth(tower.getCoolDownFraction())));
            for (EnemyMob splashTarget : tower.getSplashTargets()) {
                this.draws.add(new BeamDraw(Palette.TOWER_SPLASH_LINE, (float) target.getX(), (float) target.getY(),
                        (float) splashTarget.getX(), (float) splashTarget.getY(), 1.0f));
            }
        }
        if (tower.isSplashVisible()) {
            this.draws.add(new SplashDraw(Palette.TOWER_SPLASH_FILL,
                    tower.getSplashCenterX(), tower.getSplashCenterY(), tower.getSpreadRadius()));
        }
        return null;
    }

    /**
     * A fading beam to each enemy the scan has recently caught. The scan line itself is not
     * drawn out across the board - the tower's own turret head shows where it is pointing,
     * and a full range-length beam swinging around read as an attack rather than as a scan.
     */
    public Void visitSonarTower(SonarTower tower) {
        for (SonarTower.SonarHit hit : tower.getRecentHits()) {
            this.draws.add(new BeamDraw(Palette.TOWER_SONAR_BEAM, tower.getX(), tower.getY(),
                    hit.x(), hit.y(), beamWidth(tower.hitFade(hit, this.gameTime))));
        }
        return null;
    }

    public Void visitPulseTower(PulseTower tower) {
        if (tower.isFiring()) {
            this.draws.add(new PulseDraw(Palette.TOWER_PULSE_RING, tower.getX(), tower.getY(), tower.getRangeReal()));
        }
        return null;
    }

    public Void visitAuraTower(AuraTower tower) {
        float maxRadius = tower.getRangeReal();
        for (double phaseOffset : AURA_PHASE_OFFSETS) {
            double phase = phaseFraction(this.animationSeconds, phaseOffset);
            this.draws.add(new AuraDraw(Palette.TOWER_AURA_RING, tower.getX(), tower.getY(),
                    (float) (phase * maxRadius), (float) (1.0 - phase)));
        }
        return null;
    }

    /** No transient effect of its own - the shell in flight is what's visible, drawn as a {@code ProjectileDraw}. */
    public Void visitMortarTower(MortarTower tower) {
        return null;
    }

    /** No transient effect of its own - the missile in flight is what's visible, drawn as a {@code ProjectileDraw}. */
    public Void visitSeekerTower(SeekerTower tower) {
        return null;
    }

    /** The wedge itself, in the same heading {@code InWedgeTargetQuery} decides hits against. */
    public Void visitCinderTower(CinderTower tower) {
        float headingRadians = (float) tower.getTurretAim().radiansAt(this.interpolationAlpha);
        this.draws.add(new ConeDraw(Palette.TOWER_CINDER_CONE, tower.getX(), tower.getY(),
                headingRadians, tower.getRangeReal(), (float) tower.getHalfWidthRadians(), CINDER_CONE_ALPHA));
        return null;
    }

    /** Where in a looping {@code [0, 1)} cycle {@code seconds} sits, offset by {@code phaseOffset}. */
    private static double phaseFraction(double seconds, double phaseOffset) {
        double t = (seconds / AURA_PERIOD_SECONDS + phaseOffset) % 1.0;
        return t < 0 ? t + 1.0 : t;
    }
}
