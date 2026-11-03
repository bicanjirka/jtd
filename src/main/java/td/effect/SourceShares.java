package td.effect;

import td.damage.AttackOrigin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/** How the shares of a per-source effect combine: one share per source, each on its own clock. */
final class SourceShares {

    private SourceShares() {
    }

    /** Every share keyed to {@code source}. */
    static <S extends SourceShare<S>> List<S> keyedTo(List<S> shares, AttackOrigin source) {
        return shares.stream().map(share -> share.withSource(source)).toList();
    }

    /** {@code mine} with {@code incoming}'s shares: a source already there is refreshed, a new one added. */
    static <S extends SourceShare<S>> List<S> joined(List<S> mine, List<S> incoming) {
        List<S> merged = new ArrayList<>(mine);
        for (S share : incoming) {
            int at = indexOf(merged, share.source());
            if (at < 0) {
                merged.add(share);
            } else {
                merged.set(at, merged.get(at).refreshedBy(share));
            }
        }
        return merged;
    }

    /**
     * Every share's time through {@code time}, a share that lasts until something ends it keeping its own, without
     * the shares that are then gone.
     */
    static <S extends SourceShare<S>> List<S> retimed(List<S> shares, IntUnaryOperator time) {
        return standing(shares.stream().map(share -> share.remainingTicks() == Effect.LASTING_TICKS ? share
                : share.withRemainingTicks(time.applyAsInt(share.remainingTicks()))).toList());
    }

    /** The shares that are not gone. */
    static <S extends SourceShare<S>> List<S> standing(List<S> shares) {
        return shares.stream().filter(share -> !share.isGone()).toList();
    }

    /** The ticks until the last share runs out; {@link Effect#LASTING_TICKS} while one lasts until ended. */
    static int lastEnd(List<? extends SourceShare<?>> shares) {
        int most = 0;
        for (SourceShare<?> share : shares) {
            most = Math.max(most, share.remainingTicks());
        }
        return most;
    }

    private static <S extends SourceShare<S>> int indexOf(List<S> shares, AttackOrigin source) {
        for (int i = 0; i < shares.size(); i++) {
            if (shares.get(i).source() == source) {
                return i;
            }
        }
        return -1;
    }
}
