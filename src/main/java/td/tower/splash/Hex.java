package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.term.Term;
import td.term.TermText;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** One of the Hexer's hexes: the curse it puts on, whom it picks and how it lays it. Stateless. */
public interface Hex {

    /** The effect that marks an enemy as carrying this hex, and times it. */
    EffectKind kind();

    /** How long it lasts as it is cast, in seconds. */
    float seconds();

    /**
     * Whom it would curse now of {@code candidates}, by its own rule; empty when nobody fits.
     *
     * @param cellPixels one cell, in pixels
     */
    Optional<EnemyMob> target(List<EnemyMob> candidates, float cellPixels);

    /** Puts it on {@code target} for {@code ticks}; a hex that jumped brings the slow it had grown to. */
    void lay(EnemyMob target, int ticks, float grownSlow, HexHand hand);

    /** What it puts on an enemy. */
    default Set<Term> applies() {
        return Set.of(this.kind());
    }

    /** Its mention, with what it does. */
    TermText describe();
}
