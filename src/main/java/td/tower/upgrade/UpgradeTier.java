package td.tower.upgrade;

/**
 * A step of the tree shape every tower shares: which slot it fills, what it costs as a multiple of
 * the tower's list price, and which base node opens it. A node's own line (the level before it)
 * is added with {@link UpgradeNode#after(UpgradeNode)}.
 */
public enum UpgradeTier {
    RANGE_1(UpgradeSlot.BASE, 60),
    RANGE_2(UpgradeSlot.BASE, 120),
    RANGE_3(UpgradeSlot.BASE, 200),
    ATTUNE(UpgradeSlot.BASE, 80),
    AWAKEN(UpgradeSlot.BASE, 100),
    TRANSCENDENT(UpgradeSlot.BASE, 400),
    HEAD_1(UpgradeSlot.HEAD, 100),
    HEAD_2(UpgradeSlot.HEAD, 150),
    HEAD_3(UpgradeSlot.HEAD, 250),
    HEAD_4(UpgradeSlot.HEAD, 400),
    EXTRA_1(UpgradeSlot.HEAD, 80),
    EXTRA_2(UpgradeSlot.HEAD, 120),
    EXTRA_3(UpgradeSlot.HEAD, 200),
    EXTRA_4(UpgradeSlot.HEAD, 300),
    /** Either special slot: the second is a pick from the same set, at the same price. */
    SPECIAL(UpgradeSlot.SPECIAL, 400);

    private final UpgradeSlot slot;
    private final int pricePercent;

    UpgradeTier(UpgradeSlot slot, int pricePercent) {
        this.slot = slot;
        this.pricePercent = pricePercent;
    }

    public UpgradeSlot slot() {
        return this.slot;
    }

    /** This step's price for a tower listed at {@code listPrice}, rounded half up. */
    public int price(int listPrice) {
        return (listPrice * this.pricePercent + 50) / 100;
    }

    /** A node at this step: its slot, its price and the base node that opens it. */
    public UpgradeNode node(String id, String displayName, int listPrice) {
        return UpgradeNode.of(id, this.slot, displayName, this.price(listPrice)).withRequires(this.openedBy());
    }

    /** What the base chain must own before a node at this step is offered. */
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
