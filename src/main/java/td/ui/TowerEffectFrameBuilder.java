package td.ui;

import td.enemy.EnemyMob;
import td.tower.TowerFour;
import td.tower.TowerOne;
import td.tower.TowerThree;
import td.tower.TowerTwo;
import td.tower.TowerUpgrade;
import td.tower.TowerVisitor;
import td.ui.render.BeamDraw;
import td.ui.render.Palette;
import td.ui.render.PulseDraw;
import td.ui.render.SplashDraw;
import td.ui.render.TowerEffectDraw;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes each tower's transient targeting effect (beam, splash, pulse) as
 * {@link TowerEffectDraw} commands. One visit method per concrete tower type,
 * since - unlike the sprite - these genuinely differ by tower.
 */
public final class TowerEffectFrameBuilder implements TowerVisitor<Void> {

    private final List<TowerEffectDraw> draws = new ArrayList<>();

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

    public Void visitTowerThree(TowerThree tower) {
        int[] lineSteps = tower.getLineSteps();
        int[] enemyX = tower.getEnemyX();
        int[] enemyY = tower.getEnemyY();
        float maxSteps = tower.getCoolDownRecharge() / 2f;
        for (int i = 0; i < lineSteps.length; i++) {
            if (lineSteps[i] > 0) {
                this.draws.add(new BeamDraw(Palette.TOWER_THREE_BEAM, tower.getX(), tower.getY(),
                        enemyX[i], enemyY[i], beamWidth(lineSteps[i] / maxSteps)));
            }
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
        return null;
    }
}
