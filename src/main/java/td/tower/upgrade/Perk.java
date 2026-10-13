package td.tower.upgrade;

/** A mechanic a node adds to its tower, and what it adds in words. */
public interface Perk {

    /** What this perk adds, lower-case first, no full stop: "every 4th shell is a nuke: x4 damage over x1.5 the blast". */
    String describe();
}
