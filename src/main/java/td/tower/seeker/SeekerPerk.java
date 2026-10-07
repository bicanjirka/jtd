package td.tower.seeker;

/**
 * What one owned node does to the Seeker. A perk may change whom it may fire at, whom it picks and
 * what its nest holds. Every hook does nothing by default, so a perk implements only its own.
 */
public interface SeekerPerk {

    default SeekerSpec refineSpec(SeekerSpec spec) {
        return spec;
    }
}
