package td.tower.mortar;

import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Mortar may shell, how its shells bracket, how big a blast they make, how the shell flies
 * and which shells carry what: the base rules reshaped by each perk it owns. Each {@code withX} is a
 * copy.
 *
 * @param reach      the enemies it may fire at, less its dead zone
 * @param bracket    how landing shell on shell builds up
 * @param blastScale the blast radius as a multiple of the base
 * @param speedScale the shell's speed as a multiple of the base
 * @param sizeScale  the shell's drawn size as a multiple of the base
 * @param daze       whom an impact stuns
 * @param shells     which shells carry what
 */
public record MortarSpec(Reach reach, BracketSpec bracket, float blastScale, float speedScale, float sizeScale,
                         DazeSpec daze, ShellPlan shells) {

    /** Nothing closer than this can be shelled. */
    public static final float BASE_DEAD_ZONE_CELLS = 1.5f;

    /** The visible enemies in range beyond the dead zone, no bracketing and only plain shells. */
    public static MortarSpec from(Viewpoint view) {
        return new MortarSpec(Reach.visible(view).withDeadZone(BASE_DEAD_ZONE_CELLS * view.cellSize()),
                BracketSpec.none(), 1f, 1f, 1f, DazeSpec.none(), ShellPlan.none());
    }

    public MortarSpec withReach(Reach reach) {
        return new MortarSpec(reach, this.bracket, this.blastScale, this.speedScale, this.sizeScale, this.daze,
                this.shells);
    }

    public MortarSpec withBracket(BracketSpec bracket) {
        return new MortarSpec(this.reach, bracket, this.blastScale, this.speedScale, this.sizeScale, this.daze,
                this.shells);
    }

    /** This spec with the blast {@code factor} times as wide. */
    public MortarSpec withBlastScaledBy(float factor) {
        return new MortarSpec(this.reach, this.bracket, this.blastScale * factor, this.speedScale, this.sizeScale,
                this.daze, this.shells);
    }

    /** This spec with the shell {@code speed} times as fast and {@code size} times as big. */
    public MortarSpec withShellScaledBy(float speed, float size) {
        return new MortarSpec(this.reach, this.bracket, this.blastScale, this.speedScale * speed,
                this.sizeScale * size, this.daze, this.shells);
    }

    public MortarSpec withDaze(DazeSpec daze) {
        return new MortarSpec(this.reach, this.bracket, this.blastScale, this.speedScale, this.sizeScale, daze,
                this.shells);
    }

    public MortarSpec withShells(ShellPlan shells) {
        return new MortarSpec(this.reach, this.bracket, this.blastScale, this.speedScale, this.sizeScale,
                this.daze, shells);
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}
