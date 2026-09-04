package td.enemy;

import td.util.Context;

import java.awt.*;

/**
 * Zadny nepritel, vytvoren kvuli mezeram mezi neprateli v jedne vlne
 *
 * @author Jirka
 *
 */
public class EnemyMobEmpty extends AbstractEnemyMob {

    public EnemyMobEmpty() {
    }

    protected void doInit(Context context, int delay, int health, int price) {
    }

    public void doTick(int gameTime) {
    }

    public void paint(Graphics2D g2, int gameTime) {
    }

    public String getInfoString() {
        return null;
    }

    public boolean validTarget() {
        return false;
    }

}
