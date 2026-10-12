package td.tower.proc;

import td.effect.EffectKind;

import java.util.Set;

/** What a landed attack must be for a proc to fire; each record decides it and says it. */
public sealed interface ProcCondition {

    boolean holds(AttackOutcome outcome);

    /** "arcs", "vs shielded". */
    String describe();

    /** An attack of one of {@code kinds}. */
    record HitKindIn(Set<HitKind> kinds) implements ProcCondition {
        @Override
        public boolean holds(AttackOutcome outcome) {
            return this.kinds.contains(outcome.kind());
        }

        @Override
        public String describe() {
            return Words.hitsOf(this.kinds);
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
        public String describe() {
            return Words.statesOf(this.kinds);
        }
    }
}
