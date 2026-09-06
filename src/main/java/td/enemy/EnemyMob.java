package td.enemy;

import td.util.Context;

import java.awt.Graphics2D;

public interface EnemyMob {
    void doTick(int gameTime);

    void paint(Graphics2D g2, int gameTime);

    int getX();

    int getY();

    int getProgression();

    boolean validTarget();

    boolean validTarget(type type);

    boolean validTarget(type type0, type type1);

    long getHealth();

    void doDamage(int damage);

    int getSpeed();

    String getInfoString();

    EnemyMob create(Context context, int delay, int health, int price, int level);

    enum type {
        Normal,
        Flying,
        Invisible
    }
}
