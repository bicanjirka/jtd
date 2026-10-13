package td.tower.aura;

/**
 * How Kinship counts the other tower types near an aura. 0 for a cap means none.
 *
 * @param active          whether the aura counts tower types at all
 * @param xpPerType       the XP share each other type adds to what a buffed tower earns
 * @param xpTypeCap       the most types the XP counts
 * @param extraReachCells how much further than its range the aura counts types
 * @param strengthPerType the buff strength each other type adds
 * @param strengthTypeCap the most types the strength counts
 */
public record KinshipRule(boolean active, float xpPerType, int xpTypeCap, float extraReachCells, float strengthPerType,
                          int strengthTypeCap) {

    static final float ATTUNED_XP_PER_TYPE = 0.05f;
    static final int ATTUNED_TYPE_CAP = 4;

    public static KinshipRule none() {
        return new KinshipRule(false, 0f, 0, 0f, 0f, 0);
    }

    /** Attune's Kinship: +5% XP for each other tower type in range, up to four. */
    public static KinshipRule attuned() {
        return new KinshipRule(true, ATTUNED_XP_PER_TYPE, ATTUNED_TYPE_CAP, 0f, 0f, ATTUNED_TYPE_CAP);
    }

    /** This rule counting {@code cells} further out, with no cap on the XP. */
    public KinshipRule reachingFurther(float cells) {
        return new KinshipRule(this.active, this.xpPerType, 0, this.extraReachCells + cells, this.strengthPerType,
                this.strengthTypeCap);
    }

    /** This rule also strengthening the buff by {@code perType} for each other type. */
    public KinshipRule strengthening(float perType) {
        return new KinshipRule(this.active, this.xpPerType, this.xpTypeCap, this.extraReachCells, perType,
                this.strengthTypeCap);
    }

    /** The XP share Kinship adds for {@code types} other tower types. */
    public float xpFor(int types) {
        return this.xpPerType * (this.xpTypeCap == 0 ? types : Math.min(this.xpTypeCap, types));
    }

    /** The buff strength Kinship adds for {@code types} other tower types. */
    public float strengthFor(int types) {
        return this.strengthPerType * Math.min(this.strengthTypeCap, types);
    }
}
