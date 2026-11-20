package td.tower.mortar;

import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Mortar may shell and how its magazine draws, what a Heavy and a payload add, how many shells one firing
 * loads, whether it leads its target and how many stacks of Crack each shell's share is worth: the base rules reshaped by each mechanic it owns.
 * Each {@code withX} is a copy.
 *
 * @param reach       the enemies it may fire at
 * @param magazine    how a new shell is drawn
 * @param heavy       what a Heavy shell adds
 * @param payload     how far a payload's blast and ground grow
 * @param volley      how many shells a firing loads
 * @param leadsTarget whether it aims where the enemy will be when the shell lands
 * @param crackStacks the stacks each shell's share of Crack is worth; none for a Mortar that doesn't crack
 */
public record MortarSpec(Reach reach, MagazineSpec magazine, HeavyRider heavy, PayloadSpec payload, VolleySpec volley,
                         boolean leadsTarget, int crackStacks) {

    /** The stacks a Deep Crack shell's share of Crack is worth. */
    public static final int DEEP_CRACK_STACKS = 2;

    /** The visible enemies in range, a magazine of Normals and Heavies, one shell a firing, no Crack. */
    public static MortarSpec from(Viewpoint view) {
        return new MortarSpec(Reach.visible(view), MagazineSpec.standard(), HeavyRider.NONE, PayloadSpec.none(),
                VolleySpec.single(), false, 0);
    }

    public MortarSpec withReach(Reach reach) {
        return new MortarSpec(reach, this.magazine, this.heavy, this.payload, this.volley, this.leadsTarget,
                this.crackStacks);
    }

    public MortarSpec withMagazine(MagazineSpec magazine) {
        return new MortarSpec(this.reach, magazine, this.heavy, this.payload, this.volley, this.leadsTarget,
                this.crackStacks);
    }

    public MortarSpec withHeavy(HeavyRider heavy) {
        return new MortarSpec(this.reach, this.magazine, heavy, this.payload, this.volley, this.leadsTarget,
                this.crackStacks);
    }

    public MortarSpec withPayload(PayloadSpec payload) {
        return new MortarSpec(this.reach, this.magazine, this.heavy, payload, this.volley, this.leadsTarget,
                this.crackStacks);
    }

    public MortarSpec withVolley(VolleySpec volley) {
        return new MortarSpec(this.reach, this.magazine, this.heavy, this.payload, volley, this.leadsTarget,
                this.crackStacks);
    }

    public MortarSpec withLeadingTheTarget() {
        return new MortarSpec(this.reach, this.magazine, this.heavy, this.payload, this.volley, true, this.crackStacks);
    }

    /** Each shell's share of Crack worth at least {@code stacks}. */
    public MortarSpec withCrackStacks(int stacks) {
        return new MortarSpec(this.reach, this.magazine, this.heavy, this.payload, this.volley, this.leadsTarget,
                Math.max(this.crackStacks, stacks));
    }
}
