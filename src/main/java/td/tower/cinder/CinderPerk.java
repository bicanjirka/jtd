package td.tower.cinder;

/**
 * What one owned node does to the Cinder. A perk reshapes the spec the tower reads whenever it aims
 * or fires; it does nothing by default, so a perk implements only its own.
 */
public interface CinderPerk {

    default CinderSpec refineSpec(CinderSpec spec) {
        return spec;
    }
}
