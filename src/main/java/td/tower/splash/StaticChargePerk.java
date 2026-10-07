package td.tower.splash;

import td.enemy.EnemyMob;

/** Static Charge: arcs leave enemies Charged for the rest of the team to discharge. */
public final class StaticChargePerk implements SplashPerk {

    @Override
    public void react(ShotResult result, SplashActions actions) {
        for (EnemyMob enemy : result.arced()) {
            if (!enemy.isDead()) {
                actions.charge(enemy);
            }
        }
    }
}
