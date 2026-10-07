package td.tower.sonar;

import td.effect.Effect;

/** Command Ping: the enemy a ping picks first becomes the Priority until the next pass. */
public final class CommandPingPerk implements SonarPerk {

    @Override
    public void onRevolution(Revolution revolution, SonarActions actions) {
        if (!revolution.pinged().isEmpty()) {
            actions.apply(revolution.pinged().getFirst(),
                    sink -> Effect.priority(revolution.spec().untilNextPass(), sink));
        }
    }
}
