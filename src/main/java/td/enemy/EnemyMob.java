package td.enemy;

import td.damage.Damage;
import td.util.GameWorld;

public interface EnemyMob {
    void doTick(int gameTime);

    <R> R accept(EnemyMobVisitor<R> visitor);

    double getX();

    double getY();

    int getProgression();

    boolean validTarget();

    boolean validTarget(type type);

    boolean validTarget(type type0, type type1);

    long getHealth();

    boolean isDead();

    void doDamage(Damage damage);

    float getSpeed();

    String getInfoString();

    EnemyMob create(GameWorld context, int delay, int health, int price, int level);

    enum type {
        Normal,
        Flying,
        Invisible
    }
}
