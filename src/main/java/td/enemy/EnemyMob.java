package td.enemy;

import td.util.Context;

public interface EnemyMob {
    void doTick(int gameTime);

    <R> R accept(EnemyMobVisitor<R> visitor);

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
