package td.enemy;

import java.awt.Graphics2D;

// Placeholder enemy used to create timing gaps between real enemies within a wave.
public class EnemyMobEmpty extends AbstractEnemyMob {

    public EnemyMobEmpty() {
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
