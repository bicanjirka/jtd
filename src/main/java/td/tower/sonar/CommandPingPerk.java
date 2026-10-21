package td.tower.sonar;

import td.effect.Effect;
import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Command Ping: the enemy a ping picks first becomes the Priority until the next pass. */
public final class CommandPingPerk implements SonarPerk {

    @Override
    public void onRevolution(Revolution revolution, SonarActions actions) {
        if (!revolution.pinged().isEmpty()) {
            actions.apply(revolution.pinged().getFirst(),
                    sink -> Effect.priority(revolution.spec().untilNextPass(), sink));
        }
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.PRIORITY);
    }

    @Override
    public TermText describe() {
        return TermText.of("the enemy ").plus(TowerTerm.PING).plus(" picks gains ").plus(EffectKind.PRIORITY)
                .plus(" until the next pass");
    }
}
