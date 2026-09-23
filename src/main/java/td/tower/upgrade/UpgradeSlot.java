package td.tower.upgrade;

/**
 * One of a tower's three independent upgrade slots. {@code BASE} covers chassis-level stats
 * (range, and the {@code Awaken} node that unlocks the other two slots); {@code HEAD} covers
 * weapon-level stats; {@code SPECIAL} covers a magical/supernatural augment. Declaration order
 * here is also the on-screen order and the number-key order - see {@code UpgradeTree.offered}.
 */
public enum UpgradeSlot {
    BASE, HEAD, SPECIAL
}
