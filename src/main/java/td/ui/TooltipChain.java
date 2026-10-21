package td.ui;

import td.ui.render.TooltipKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The open tooltips, shallowest first, by key; depth 0 is the pane, depth n the n-th tooltip. */
record TooltipChain(List<TooltipKey> keys) {

    private static final TooltipChain EMPTY = new TooltipChain(List.of());

    TooltipChain {
        keys = List.copyOf(keys);
    }

    static TooltipChain empty() {
        return EMPTY;
    }

    /**
     * The chain with {@code key} opened from a source at {@code depth}, everything deeper closed; empty when that
     * source already shows {@code key}, or a shallower tooltip shows its subject, since a chain never opens one
     * term twice.
     */
    Optional<TooltipChain> open(int depth, TooltipKey key) {
        if (depth > this.keys.size()) {
            throw new IllegalArgumentException("No source at depth " + depth);
        }
        boolean shownHere = depth < this.keys.size() && this.keys.get(depth).equals(key);
        boolean shownShallower = this.keys.subList(0, depth).stream().anyMatch(open -> open.sameSubject(key));
        if (shownHere || shownShallower) {
            return Optional.empty();
        }
        List<TooltipKey> next = new ArrayList<>(this.keys.subList(0, depth));
        next.add(key);
        return Optional.of(new TooltipChain(next));
    }

    /** Only the tooltips shallower than {@code depth}. */
    TooltipChain closedFrom(int depth) {
        return new TooltipChain(this.keys.subList(0, Math.min(depth, this.keys.size())));
    }
}
