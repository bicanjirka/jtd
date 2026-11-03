package td.effect;

import td.damage.AttackOrigin;

/**
 * One source's part of an effect that keeps a share per source (a heal, a shield): who it comes from and its own
 * time. {@link SourceShares} combines them.
 *
 * @param <S> the share's own type, so a copy keeps it
 */
interface SourceShare<S extends SourceShare<S>> {

    AttackOrigin source();

    /** The ticks it has left; {@link Effect#LASTING_TICKS} for one that lasts until something ends it. */
    int remainingTicks();

    S withSource(AttackOrigin source);

    S withRemainingTicks(int ticks);

    /** This share as a recast from its own source leaves it. */
    S refreshedBy(S incoming);

    /** Whether it has nothing left to give. */
    boolean isGone();
}
