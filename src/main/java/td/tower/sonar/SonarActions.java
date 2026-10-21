package td.tower.sonar;

import td.effect.DamageSink;
import td.effect.Effect;
import td.enemy.EnemyMob;

import java.util.function.Function;

/** What a mechanic may make the Sonar do beyond shaping its hits. */
public interface SonarActions {

    /** Puts an effect on {@code target}, credited to the Sonar for any damage it deals. */
    void apply(EnemyMob target, Function<DamageSink, Effect> effect);

    /**
     * Reveals every invisible enemy in range at least {@code rangeShare} of the range out, for
     * {@code ticks}.
     */
    void revealHiddenBeyond(float rangeShare, int ticks);
}
