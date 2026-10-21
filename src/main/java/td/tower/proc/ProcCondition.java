package td.tower.proc;

import td.effect.EffectKind;
import td.term.TermText;

import java.util.Set;

/** What a landed attack must be for a proc to fire; each record decides it and says it. */
public sealed interface ProcCondition {

    boolean holds(AttackOutcome outcome);

    /** What the target must carry or be, after the trigger; empty when the trigger's noun says it all. */
    TermText describe();

    /** The kinds of hit it limits the proc to; none when it limits nothing by kind. */
    Set<HitKind> hitKinds();

    /** An attack of one of {@code kinds}. */
    record HitKindIn(Set<HitKind> kinds) implements ProcCondition {
        @Override
        public boolean holds(AttackOutcome outcome) {
            return this.kinds.contains(outcome.kind());
        }

        @Override
        public TermText describe() {
            return TermText.empty();
        }

        @Override
        public Set<HitKind> hitKinds() {
            return this.kinds;
        }
    }

    /** A target under any of {@code kinds}. */
    record TargetHasAnyOf(Set<EffectKind> kinds) implements ProcCondition {
        @Override
        public boolean holds(AttackOutcome outcome) {
            for (EffectKind kind : this.kinds) {
                if (outcome.target().hasEffect(kind)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public TermText describe() {
            return TermText.of("on an enemy with ").plus(ProcText.kinds(this.kinds, "or"));
        }

        @Override
        public Set<HitKind> hitKinds() {
            return Set.of();
        }
    }
}
