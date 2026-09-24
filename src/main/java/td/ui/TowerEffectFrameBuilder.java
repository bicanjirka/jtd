package td.ui;

import td.enemy.EnemyMob;
import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.MortarTower;
import td.tower.PulseTower;
import td.tower.SeekerTower;
import td.tower.SniperTower;
import td.tower.SonarTower;
import td.tower.SplashTower;
import td.tower.Tower;
import td.tower.TowerVisitor;
import td.ui.render.AuraDraw;
import td.ui.render.BeamDraw;
import td.ui.render.ConeDraw;
import td.ui.render.Palette;
import td.ui.render.PulseDraw;
import td.ui.render.SplashDraw;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerStatusDraw;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes each tower's transient effect - beam, splash, pulse, cone, or an aura's ring - as draw
 * commands.
 */
public final class TowerEffectFrameBuilder implements TowerVisitor<Void> {

    // Two rings half a period apart read as one breathing aura, not a blinking ring.
    private static final double AURA_PERIOD_SECONDS = 1.8;
    private static final double[] AURA_PHASE_OFFSETS = {0.0, 0.5};

    private static final float STATUS_MARKER_SCALE_FRACTION = 0.12f;

    private final List<TowerEffectDraw> draws = new ArrayList<>();
    private final int gameTime;
    private final double interpolationAlpha;
    private final double animationSeconds;

    public TowerEffectFrameBuilder(int gameTime, double interpolationAlpha, double animationSeconds) {
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
        this.animationSeconds = animationSeconds;
    }

    private static float beamWidth(float coolDownFraction) {
        return 3.0f * coolDownFraction;
    }

    /** Position of {@code seconds} in a repeating {@code [0, 1)} cycle. */
    private static double phaseFraction(double seconds, double phaseOffset) {
        double t = (seconds / AURA_PERIOD_SECONDS + phaseOffset) % 1.0;
        return t < 0 ? t + 1.0 : t;
    }

    /** A marker in the tower's top-right corner while an enemy disrupts it. */
    public void addStatus(Tower tower, int scale) {
        if (tower.isDisrupted()) {
            this.draws.add(new TowerStatusDraw(Palette.DISRUPTION, tower.getBoardX() + scale * 0.85f,
                    tower.getBoardY() + scale * 0.15f, scale * STATUS_MARKER_SCALE_FRACTION));
        }
    }

    public List<TowerEffectDraw> build() {
        return this.draws;
    }

    public Void visitSniperTower(SniperTower tower) {
        EnemyMob target = tower.getCurrentTarget();
        if (target != null) {
            Palette beamColor = tower.wasLastShotCritical() ? Palette.CRIT_SPARK : Palette.TOWER_SNIPER_BEAM;
            this.draws.add(new BeamDraw(beamColor, tower.getX(), tower.getY(),
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
     * A fading beam to each enemy recently caught. The scan line itself is not drawn; the turret
     * head shows it.
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
        for (Tower buffed : tower.buffedTowers()) {
            this.draws.add(new BeamDraw(Palette.TOWER_AURA_LINK, tower.getX(), tower.getY(),
                    buffed.getX(), buffed.getY(), 1.0f));
        }
        return null;
    }

    /** Nothing: the shell is drawn as a projectile. */
    public Void visitMortarTower(MortarTower tower) {
        return null;
    }

    /** Nothing: the missile is drawn as a projectile. */
    public Void visitSeekerTower(SeekerTower tower) {
        return null;
    }

    /** One cone per wave in flight, its progress interpolated between ticks. */
    public Void visitCinderTower(CinderTower tower) {
        for (CinderTower.FlameWave wave : tower.getInFlightWaves()) {
            float previousProgress = waveProgress(wave, this.gameTime - 1);
            float currentProgress = waveProgress(wave, this.gameTime);
            float progress = previousProgress + (currentProgress - previousProgress) * (float) this.interpolationAlpha;
            this.draws.add(new ConeDraw(Palette.TOWER_CINDER_CONE, tower.getX(), tower.getY(),
                    (float) wave.headingRadians(), tower.getRangeReal(), (float) wave.halfWidthRadians(), progress));
        }
        return null;
    }

    private static float waveProgress(CinderTower.FlameWave wave, int atTick) {
        float travelled = (float) (atTick - wave.firedAtTick()) / CinderTower.WAVE_TRAVEL_TICKS;
        return Math.max(0f, Math.min(1f, travelled));
    }
}
