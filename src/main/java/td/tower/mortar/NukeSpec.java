package td.tower.mortar;

/**
 * The Tactical Nuke: every {@code every}th shell hits {@code damageFactor} times as hard over
 * {@code radiusFactor} times the blast, and leaves fallout.
 */
public record NukeSpec(int every, float damageFactor, float radiusFactor) {

    public static NukeSpec none() {
        return new NukeSpec(0, 1f, 1f);
    }

    public static NukeSpec of(int every, float damageFactor, float radiusFactor) {
        return new NukeSpec(every, damageFactor, radiusFactor);
    }

    public boolean isActive() {
        return this.every > 0;
    }
}
