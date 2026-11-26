package td.tower.seeker;

/** What every so many freezes set off, the Aerie's Tier I pick: nothing, a Volley or a Hatching. */
public enum Release {
    NONE,
    /** Missiles at once, each at a different enemy. */
    VOLLEY,
    /** Missiles that hatch into the nest one after another, past its size. */
    HATCHING
}
