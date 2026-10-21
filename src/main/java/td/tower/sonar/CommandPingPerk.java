package td.tower.sonar;

import td.effect.Effect;
import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.term.Term;
import td.term.TermText;
import td.tower.BehaviourLine;
import td.tower.BehaviourMarker;
import td.tower.upgrade.PerkText;

import java.util.Optional;
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
    public Optional<BehaviourLine> behaviour() {
        return Optional.of(new BehaviourLine(BehaviourMarker.TARGETING, "Priority", "the pinged enemy"));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.PRIORITY);
    }

    @Override
    public TermText describe() {
        return TermText.of("the pinged enemy is the Priority until the next pass: +"
                + PerkText.percent(EffectKind.PRIORITY.modifiersAt(1).flat(EnemyStat.PHYSICAL_DAMAGE_TAKEN))
                + " damage taken, and towers that pick one target pick it");
    }
}
