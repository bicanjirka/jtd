package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.MissileLook;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/**
 * What a missile carries besides its damage. Stateless: which missile carries which is the
 * {@link PayloadPlan}'s to say.
 */
public interface Payload {

    /** How a missile carrying it is drawn. */
    MissileLook look();

    /** The buff term the missile carrying it is. */
    TowerTerm term();

    /** What the info rows call it. */
    String label();

    /** What it does to the target: "applies [Freeze] to its target". */
    TermText effect();

    /** The terms the missile carrying it puts on its target. */
    Set<EffectKind> applies();

    /** Whether the missile freezes its target. */
    default boolean freezes() {
        return false;
    }

    /** Puts it on {@code target}, {@code strength} times as strong as the base payload. */
    void deliver(EnemyMob target, float strength, PayloadActions actions);
}
