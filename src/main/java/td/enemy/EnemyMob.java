package td.enemy;

import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;

import java.util.Set;

/**
 * One enemy walking a path, as towers, targeting and the renderer see it. Concrete types are
 * reached only through {@link EnemyMobVisitor}.
 */
public interface EnemyMob {
    void doTick(int gameTime);

    <R> R accept(EnemyMobVisitor<R> visitor);

    double getX();

    double getY();

    /** How far along its lap this mob is; for ranking only. */
    int getProgression();

    boolean validTarget();

    boolean validTarget(Type type);

    boolean validTarget(Type type0, Type type1);

    int getHealth();

    boolean isDead();

    /** Paid on death, and lost as score if it leaks. */
    int getBounty();

    /**
     * Applies a hit and returns what actually landed, which may be less or nothing. Report the
     * return value, not the argument.
     */
    Damage doDamage(Damage damage);

    void applyEffect(Effect effect);

    /** Active effect kinds, in a stable order. */
    Set<EffectKind> activeEffectKinds();

    float getSpeed();

    String getInfoString();

    /**
     * What a tower can see. {@code INVISIBLE} is reachable only by area damage; a mob reports it
     * while an invisibility effect is active. {@code FLYING} is unused.
     */
    enum Type {
        NORMAL,
        FLYING,
        INVISIBLE
    }
}
