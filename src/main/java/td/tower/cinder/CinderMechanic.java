package td.tower.cinder;

import td.tower.upgrade.Mechanic;


/**
 * What one owned node does to the Cinder. A mechanic reshapes the spec the tower reads whenever it aims
 * or fires; it does nothing by default, so a mechanic implements only its own.
 */
public interface CinderMechanic extends Mechanic {

    default CinderSpec refineSpec(CinderSpec spec) {
        return spec;
    }

}
