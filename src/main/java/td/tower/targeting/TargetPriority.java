package td.tower.targeting;

/** Whom a tower aims at instead of its own choice, as a node sets it. */
public enum TargetPriority {
    HIGHEST_RANK("highest rank"), MOST_HEALTH("most health"),
    STOPPED_FIRST("frozen or dazed first");

    private final String label;

    TargetPriority(String label) {
        this.label = label;
    }

    /** The selector that picks this way on a board of {@code cellSize} pixels a cell. */
    public TargetSelector selector(float cellSize) {
        return switch (this) {
            case HIGHEST_RANK -> new HighestRankSelector();
            case MOST_HEALTH -> new HighestHealthSelector();
            case STOPPED_FIRST -> PreferringSelector.stoppedFirst(new HighestHealthSelector());
        };
    }

    public String label() {
        return this.label;
    }
}
