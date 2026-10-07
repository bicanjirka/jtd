package td.tower.mortar;

/**
 * Fragmentation: a ring past the main blast that every shell throws shrapnel into. Each piece hits
 * for {@code damageShare} of the shell's damage, cracks plating, and reaches {@code reachCellsPerStep}
 * further for every Bracketing step.
 *
 * @param active           whether shells throw shrapnel at all
 * @param scale            how much more of everything the shrapnel does: damage, Cracked and effects
 * @param carriesShell     whether it applies its shell's own effect (burn, tar, chill) to what it hits
 * @param bleeds           whether it leaves what it hits bleeding
 */
public record ShrapnelSpec(boolean active, float damageShare, float reachCellsPerStep, float scale,
                           boolean carriesShell, boolean bleeds) {

    public static ShrapnelSpec none() {
        return new ShrapnelSpec(false, 0f, 0f, 1f, false, false);
    }

    public static ShrapnelSpec of(float damageShare, float reachCellsPerStep) {
        return new ShrapnelSpec(true, damageShare, reachCellsPerStep, 1f, false, false);
    }

    /** This shrapnel doing {@code factor} times as much of everything it does. */
    public ShrapnelSpec scaledBy(float factor) {
        return new ShrapnelSpec(this.active, this.damageShare, this.reachCellsPerStep, this.scale * factor,
                this.carriesShell, this.bleeds);
    }

    public ShrapnelSpec carryingTheShellsEffect() {
        return new ShrapnelSpec(this.active, this.damageShare, this.reachCellsPerStep, this.scale, true,
                this.bleeds);
    }

    public ShrapnelSpec bleeding() {
        return new ShrapnelSpec(this.active, this.damageShare, this.reachCellsPerStep, this.scale,
                this.carriesShell, true);
    }
}
