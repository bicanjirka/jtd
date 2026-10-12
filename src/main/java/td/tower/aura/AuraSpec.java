package td.tower.aura;

/**
 * What an aura does beyond its emission: Kinship, the XP mentoring, and how it picks whom it buffs.
 * Each {@code withX} is a copy.
 *
 * @param kinship       how it counts the tower types near it
 * @param tutelageXp    the XP share it adds to what every buffed tower earns
 * @param sharesLessons whether its buffed towers each earn what any of them reached
 * @param apprenticeXp  the XP share it adds to its least experienced buffed tower; 0 for no apprentice
 * @param chosen        whether it buffs only its most experienced tower, more strongly
 * @param rally         whether an enemy on its path spot rallies the towers it buffs
 */
public record AuraSpec(KinshipRule kinship, float tutelageXp, boolean sharesLessons, float apprenticeXp, boolean chosen,
                       boolean rally) {

    /** No Kinship, no mentoring, every tower in range, no Rally. */
    public static AuraSpec none() {
        return new AuraSpec(KinshipRule.none(), 0f, false, 0f, false, false);
    }

    public AuraSpec withKinship(KinshipRule kinship) {
        return new AuraSpec(kinship, this.tutelageXp, this.sharesLessons, this.apprenticeXp, this.chosen, this.rally);
    }

    public AuraSpec withTutelageXp(float xp) {
        return new AuraSpec(this.kinship, xp, this.sharesLessons, this.apprenticeXp, this.chosen, this.rally);
    }

    public AuraSpec sharingLessons() {
        return new AuraSpec(this.kinship, this.tutelageXp, true, this.apprenticeXp, this.chosen, this.rally);
    }

    public AuraSpec withApprenticeXp(float xp) {
        return new AuraSpec(this.kinship, this.tutelageXp, this.sharesLessons, xp, this.chosen, this.rally);
    }

    public AuraSpec choosing() {
        return new AuraSpec(this.kinship, this.tutelageXp, this.sharesLessons, this.apprenticeXp, true, this.rally);
    }

    public AuraSpec rallying() {
        return new AuraSpec(this.kinship, this.tutelageXp, this.sharesLessons, this.apprenticeXp, this.chosen, true);
    }
}
