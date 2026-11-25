package td.tower.targeting;

/** Whom a tower aims at instead of its own choice, as a node sets it. */
public enum TargetPriority {
    HIGHEST_RANK("highest rank"), MOST_HEALTH("most health"),
    STOPPED_FIRST("frozen or dazed first"), BIGGEST_GROUP_AHEAD("biggest group ahead"),
    EXECUTABLE_FIRST("executable first"), HUNTED_FIRST("Elites and Bosses first");

    /** How near another enemy stands to count as a neighbour, about as far as a blast reaches. */
    private static final float NEIGHBOUR_CELLS = 1.5f;

    private final String label;

    TargetPriority(String label) {
        this.label = label;
    }

    /** The selector that picks this way for a tower that tells it {@code pick}. */
    public TargetSelector selector(PickContext pick) {
        return switch (this) {
            case HIGHEST_RANK -> new HighestRankSelector();
            case MOST_HEALTH -> new HighestHealthSelector();
            case STOPPED_FIRST -> PreferringSelector.stoppedFirst(new HighestHealthSelector());
            case BIGGEST_GROUP_AHEAD -> new GroupAheadSelector(NEIGHBOUR_CELLS * pick.cellSize());
            case EXECUTABLE_FIRST -> new ExecutionerSelector(ExecuteThreshold.EXECUTIONER, pick.critHit());
            case HUNTED_FIRST -> new HuntedFirstSelector();
        };
    }

    public String label() {
        return this.label;
    }
}
