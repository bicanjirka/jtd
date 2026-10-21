package td.tower.proc;

import td.effect.EffectKind;
import td.enemy.Rank;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.List;
import java.util.Set;

/** When a hit bonus applies; each record decides it and says it in words. */
public sealed interface HitCondition {

    boolean holds(HitContext context);

    /** When it holds, after what it adds: "against Elite and Boss enemies". Empty for every hit. */
    TermText describe();

    /** Every hit. */
    record Always() implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return true;
        }

        @Override
        public TermText describe() {
            return TermText.empty();
        }
    }

    /** The first attack at a new target. */
    record FreshTarget() implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.freshTarget();
        }

        @Override
        public TermText describe() {
            return TermText.of("on the first shot at a target");
        }
    }

    /** Once the tower's focus has built to {@code focus}. */
    record FocusAtLeast(TowerTerm counter, int focus) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.focus() >= this.focus;
        }

        @Override
        public TermText describe() {
            return this.focus == 1 ? TermText.of("while it has ").plus(this.counter)
                    : TermText.of("with " + this.focus + " or more stacks of ").plus(this.counter);
        }
    }

    /** A target of {@code rank} or higher. */
    record TargetRankAtLeast(Rank rank) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.target().getRank().compareTo(this.rank) >= 0;
        }

        @Override
        public TermText describe() {
            return TermText.of("against " + ProcText.ranksFrom(this.rank) + " enemies");
        }
    }

    /** A target under {@code share} of its health. */
    record TargetHealthBelow(float share) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.target().getHealthFraction() < this.share;
        }

        @Override
        public TermText describe() {
            return TermText.of("under " + TermNumbers.percent(this.share) + " health");
        }
    }

    /** A target that is stopped. */
    record TargetStopped() implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.target().isStopped();
        }

        @Override
        public TermText describe() {
            return TermText.of("against an enemy with ").plus(ProcText.kinds(EffectKind.stopping(), "or"));
        }
    }

    /** A target under any of {@code kinds}. */
    record TargetHasAnyOf(Set<EffectKind> kinds) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            for (EffectKind kind : this.kinds) {
                if (context.target().hasEffect(kind)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public TermText describe() {
            return TermText.of("against an enemy with ").plus(ProcText.kinds(this.kinds, "or"));
        }
    }

    /** A target past {@code share} of the tower's range. */
    record BeyondRangeShare(float share) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.rangeShare() > this.share;
        }

        @Override
        public TermText describe() {
            for (int denominator = 2; denominator <= 4; denominator++) {
                int numerator = Math.round(this.share * denominator);
                if (Math.abs(this.share - (float) numerator / denominator) < 1e-3f) {
                    return TermText.of("past " + numerator + "/" + denominator + " of range");
                }
            }
            return TermText.of("past " + TermNumbers.percent(this.share) + " of range");
        }
    }

    /** A hit of one of {@code kinds}. */
    record HitKindIn(Set<HitKind> kinds) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return this.kinds.contains(context.kind());
        }

        @Override
        public TermText describe() {
            return TermText.of("on ").plus(ProcText.hits(this.kinds, "and", ProcText::plural));
        }
    }

    /** Every one of {@code conditions}. */
    record All(List<HitCondition> conditions) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            for (HitCondition condition : this.conditions) {
                if (!condition.holds(context)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public TermText describe() {
            return TermText.join(" ", this.conditions.stream().map(HitCondition::describe).toList());
        }
    }
}
