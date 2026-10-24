package td.tower.upgrade;

import java.util.Optional;

/**
 * A step of the tree shape every tower shares: which slot it fills, what it costs as a multiple of
 * the tower's list price, the XP it needs, and which Core node opens it. A node's own line (the
 * level before it) is added with {@link UpgradeNode#after(UpgradeNode)}. Steps behind
 * Transcendent need no XP of their own: Transcendent is their gate.
 */
public enum UpgradeTier {
    RANGE_1(UpgradeSlot.BASE, 60, 0),
    RANGE_2(UpgradeSlot.BASE, 120, 0),
    RANGE_3(UpgradeSlot.BASE, 200, 0),
    ATTUNE(UpgradeSlot.CORE, 80, 0),
    AWAKEN(UpgradeSlot.CORE, 100, 0),
    TRANSCENDENT(UpgradeSlot.CORE, 400, 300),
    HEAD_1(UpgradeSlot.HEAD, 100, 0),
    HEAD_2(UpgradeSlot.HEAD, 150, 50),
    /** Also the tower's purpose gate, once its own feature gives it a deed. */
    HEAD_3(UpgradeSlot.HEAD, 250, 150),
    HEAD_4(UpgradeSlot.HEAD, 400, 0),
    EXTRA_1(UpgradeSlot.HEAD, 80, 25),
    EXTRA_2(UpgradeSlot.HEAD, 120, 75),
    EXTRA_3(UpgradeSlot.HEAD, 200, 200),
    EXTRA_4(UpgradeSlot.HEAD, 300, 0),
    /**
     * Either special slot: the second is a pick from the same set, at the same price, and its XP
     * is long met by then.
     */
    SPECIAL(UpgradeSlot.SPECIAL, 400, 150);

    private final UpgradeSlot slot;
    private final int pricePercent;
    private final int xp;

    UpgradeTier(UpgradeSlot slot, int pricePercent, int xp) {
        this.slot = slot;
        this.pricePercent = pricePercent;
        this.xp = xp;
    }

    public UpgradeSlot slot() {
        return this.slot;
    }

    /** This step's price for a tower listed at {@code listPrice}, rounded half up. */
    public int price(int listPrice) {
        return (listPrice * this.pricePercent + 50) / 100;
    }

    public int xp() {
        return this.xp;
    }

    /** This step's level in its line: 1 to 4, or 0 for a special, which has none. */
    public int level() {
        return switch (this) {
            case RANGE_1, ATTUNE, HEAD_1, EXTRA_1 -> 1;
            case RANGE_2, AWAKEN, HEAD_2, EXTRA_2 -> 2;
            case RANGE_3, TRANSCENDENT, HEAD_3, EXTRA_3 -> 3;
            case HEAD_4, EXTRA_4 -> 4;
            case SPECIAL -> 0;
        };
    }

    /** Whether a node at this step shows art of its own rather than its line's: a Core step, a level IV, a special. */
    public boolean hasOwnArt() {
        return this.slot == UpgradeSlot.CORE || this == HEAD_4 || this == SPECIAL;
    }

    /** The step before this one in its line; empty for the first step and for a special. */
    public Optional<UpgradeTier> previous() {
        return switch (this) {
            case RANGE_2 -> Optional.of(RANGE_1);
            case RANGE_3 -> Optional.of(RANGE_2);
            case AWAKEN -> Optional.of(ATTUNE);
            case TRANSCENDENT -> Optional.of(AWAKEN);
            case HEAD_2 -> Optional.of(HEAD_1);
            case HEAD_3 -> Optional.of(HEAD_2);
            case HEAD_4 -> Optional.of(HEAD_3);
            case EXTRA_2 -> Optional.of(EXTRA_1);
            case EXTRA_3 -> Optional.of(EXTRA_2);
            case EXTRA_4 -> Optional.of(EXTRA_3);
            case RANGE_1, ATTUNE, HEAD_1, EXTRA_1, SPECIAL -> Optional.empty();
        };
    }

    /** The first step of a line that a node of {@code slot} starts at unless it says otherwise. */
    static UpgradeTier startOf(UpgradeSlot slot) {
        return switch (slot) {
            case CORE -> ATTUNE;
            case BASE -> RANGE_1;
            case HEAD -> HEAD_1;
            case SPECIAL -> SPECIAL;
        };
    }

    /** A node at this step: its slot, its price, its XP and the Core node that opens it. */
    public UpgradeNode node(String id, String displayName, int listPrice) {
        return UpgradeNode.of(id, this.slot, displayName, this.price(listPrice))
                .withRequires(this.openedBy())
                .withXp(this.xp)
                .withTier(this);
    }

    /** What the Core chain must own before a node at this step is offered. */
    public UpgradeCondition openedBy() {
        return switch (this) {
            case RANGE_1, ATTUNE -> UpgradeCondition.always();
            case AWAKEN, HEAD_1, HEAD_2, EXTRA_1, EXTRA_2 -> UpgradeCondition.owns(StandardBaseSlot.ATTUNE_ID);
            case RANGE_2, TRANSCENDENT, HEAD_3, EXTRA_3 -> UpgradeCondition.owns(StandardBaseSlot.AWAKEN_ID);
            case RANGE_3, HEAD_4, EXTRA_4 -> UpgradeCondition.owns(StandardBaseSlot.TRANSCENDENT_ID);
            // A second special waits for Transcendent; how many a tower may own is its ExclusiveChoice.
            case SPECIAL -> UpgradeCondition.owns(StandardBaseSlot.AWAKEN_ID)
                    .and(UpgradeCondition.slotEmpty(UpgradeSlot.SPECIAL)
                            .or(UpgradeCondition.owns(StandardBaseSlot.TRANSCENDENT_ID)));
        };
    }
}
