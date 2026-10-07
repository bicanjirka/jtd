package td.tower.sonar;

/**
 * What one owned node does to the Sonar. A perk may change whom the beam hits, how it moves and
 * whom it pings, reshape each hit before it lands, and react to a hit or to a finished revolution.
 * Every hook does nothing by default, so a perk implements only its own.
 */
public interface SonarPerk {

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
