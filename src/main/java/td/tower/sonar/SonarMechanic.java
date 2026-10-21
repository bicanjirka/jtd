package td.tower.sonar;

import td.tower.upgrade.Mechanic;


/**
 * What one owned node does to the Sonar. A mechanic may change whom the beam hits, how it moves and
 * whom it pings, reshape each hit before it lands, and react to a hit or to a finished revolution.
 * Every hook does nothing by default, so a mechanic implements only its own.
 */
public interface SonarMechanic extends Mechanic {

    default SonarSpec refineSpec(SonarSpec spec) {
        return spec;
    }

    default SonarStrike shape(SonarStrike strike, StrikeContext context) {
        return strike;
    }

    default void react(StrikeResult result, SonarActions actions) {
    }

    default void onRevolution(Revolution revolution, SonarActions actions) {
    }

}
