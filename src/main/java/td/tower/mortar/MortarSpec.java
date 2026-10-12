package td.tower.mortar;

import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Mortar may shell, how its shells bracket, how big a blast they make, how the shell flies
 * and which shells carry what: the base rules reshaped by each perk it owns. Each {@code withX} is a
 * copy.
 *
 * @param reach          the enemies it may fire at
 * @param bracket        how landing shell on shell builds up
 * @param daze           whom an impact stuns
 * @param shells         which shells carry what
 * @param nuke           what a nuke does
 * @param centre         what the centre of a blast suffers
 * @param shrapnel       the ring past the blast
 * @param bomblets       the small blasts along the path
 * @param leadsTarget    whether it aims where the enemy will be when the shell lands
 * @param flatCore       whether the inner half of a blast takes full damage
 * @param salvo          how many shells a firing sends
 */
public record MortarSpec(Reach reach, BracketSpec bracket, DazeSpec daze, ShellPlan shells, NukeSpec nuke, CentreSpec centre, ShrapnelSpec shrapnel,
                         BombletSpec bomblets, boolean leadsTarget, boolean flatCore, SalvoSpec salvo) {

    /** The visible enemies in range, no bracketing and only plain shells. */
    public static MortarSpec from(Viewpoint view) {
        return new MortarSpec(Reach.visible(view), BracketSpec.none(), DazeSpec.none(), ShellPlan.none(), NukeSpec.none(),
                CentreSpec.none(), ShrapnelSpec.none(), BombletSpec.none(), false, false, SalvoSpec.single());
    }

    public MortarSpec withReach(Reach reach) {
        return new MortarSpec(reach, this.bracket, this.daze,
                this.shells, this.nuke, this.centre, this.shrapnel, this.bomblets, this.leadsTarget, this.flatCore,
                this.salvo);
    }

    public MortarSpec withBracket(BracketSpec bracket) {
        return new MortarSpec(this.reach, bracket, this.daze,
                this.shells, this.nuke, this.centre, this.shrapnel, this.bomblets, this.leadsTarget, this.flatCore,
                this.salvo);
    }



    public MortarSpec withDaze(DazeSpec daze) {
        return new MortarSpec(this.reach, this.bracket, daze,
                this.shells, this.nuke, this.centre, this.shrapnel, this.bomblets, this.leadsTarget, this.flatCore,
                this.salvo);
    }

    public MortarSpec withShells(ShellPlan shells) {
        return new MortarSpec(this.reach, this.bracket, this.daze, shells, this.nuke, this.centre, this.shrapnel, this.bomblets, this.leadsTarget,
                this.flatCore, this.salvo);
    }

    public MortarSpec withNuke(NukeSpec nuke) {
        return new MortarSpec(this.reach, this.bracket, this.daze, this.shells, nuke, this.centre, this.shrapnel, this.bomblets, this.leadsTarget,
                this.flatCore, this.salvo);
    }

    public MortarSpec withCentre(CentreSpec centre) {
        return new MortarSpec(this.reach, this.bracket, this.daze, this.shells, this.nuke, centre, this.shrapnel, this.bomblets, this.leadsTarget,
                this.flatCore, this.salvo);
    }

    public MortarSpec withShrapnel(ShrapnelSpec shrapnel) {
        return new MortarSpec(this.reach, this.bracket, this.daze, this.shells, this.nuke, this.centre, shrapnel, this.bomblets, this.leadsTarget,
                this.flatCore, this.salvo);
    }

    public MortarSpec withBomblets(BombletSpec bomblets) {
        return new MortarSpec(this.reach, this.bracket, this.daze, this.shells, this.nuke, this.centre, this.shrapnel, bomblets, this.leadsTarget,
                this.flatCore, this.salvo);
    }

    public MortarSpec withLeadingTheTarget() {
        return new MortarSpec(this.reach, this.bracket, this.daze, this.shells, this.nuke, this.centre, this.shrapnel, this.bomblets, true, this.flatCore,
                this.salvo);
    }

    public MortarSpec withFlatCore() {
        return new MortarSpec(this.reach, this.bracket, this.daze, this.shells, this.nuke, this.centre, this.shrapnel, this.bomblets, this.leadsTarget,
                true, this.salvo);
    }

    public MortarSpec withSalvo(SalvoSpec salvo) {
        return new MortarSpec(this.reach, this.bracket, this.daze, this.shells, this.nuke, this.centre, this.shrapnel, this.bomblets, this.leadsTarget,
                this.flatCore, salvo);
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}
