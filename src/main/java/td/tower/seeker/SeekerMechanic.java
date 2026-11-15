package td.tower.seeker;

import td.tower.upgrade.Mechanic;

/**
 * What one owned node does to the Aerie. A mechanic reshapes the spec the tower reads whenever it loads, launches or
 * lands a missile; it does nothing by default, so a mechanic implements only its own.
 */
public interface SeekerMechanic extends Mechanic {

    default SeekerSpec refineSpec(SeekerSpec spec) {
        return spec;
    }
}
