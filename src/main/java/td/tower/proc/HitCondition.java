package td.tower.proc;

import td.effect.EffectKind;
import td.enemy.Rank;
import td.tower.TowerStat;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** When a hit bonus applies; each record decides it and says it in words. */
public sealed interface HitCondition {

    boolean holds(HitContext context);

    /** How the condition reads in a row label: "vs Elite and Boss", "first shot at a target", ... */
    String describe();

    /** Every hit. */
    record Always() implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return true;
        }

        @Override
        public String describe() {
            return "";
        }
    }

    /** The first attack at a new target. */
    record FreshTarget() implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.freshTarget();
        }

        @Override
        public String describe() {
            return "first shot at a target";
        }
    }

    /** Once the tower has landed {@code focus} attacks in a row on the target. */
    record FocusAtLeast(int focus) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.focus() >= this.focus;
        }

        @Override
        public String describe() {
            return this.focus == 1 ? "after the first shot at a target" : "after " + this.focus + " shots at a target";
        }
    }

    /** A target of {@code rank} or higher. */
    record TargetRankAtLeast(Rank rank) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.target().getRank().compareTo(this.rank) >= 0;
        }

        @Override
        public String describe() {
            List<String> ranks = Arrays.stream(Rank.values()).filter(r -> r.compareTo(this.rank) >= 0)
                    .map(r -> Words.capitalized(r.name().toLowerCase(Locale.ROOT))).toList();
            return "vs " + Words.listed(ranks, "and");
        }
    }

    /** A target under {@code share} of its health. */
    record TargetHealthBelow(float share) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.target().getHealthFraction() < this.share;
        }

        @Override
        public String describe() {
            return "vs under " + Math.round(this.share * 100) + "% health";
        }
    }

    /** A target that is stopped. */
    record TargetStopped() implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.target().isStopped();
        }

        @Override
        public String describe() {
            return "vs frozen or dazed";
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
        public String describe() {
            return Words.statesOf(this.kinds);
        }
    }

    /** A target past {@code share} of the tower's range. */
    record BeyondRangeShare(float share) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.rangeShare() > this.share;
        }

        @Override
        public String describe() {
            for (int denominator = 2; denominator <= 4; denominator++) {
                int numerator = Math.round(this.share * denominator);
                if (Math.abs(this.share - (float) numerator / denominator) < 1e-3f) {
                    return "past " + numerator + "/" + denominator + " of range";
                }
            }
            return "past " + Math.round(this.share * 100) + "% of range";
        }
    }

    /** A hit of one of {@code kinds}. */
    record HitKindIn(Set<HitKind> kinds) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return this.kinds.contains(context.kind());
        }

        @Override
        public String describe() {
            return Words.hitsOf(this.kinds);
        }
    }

    /** A target with at least as many stacks of {@code kind} as the attack's {@code stat}: at full Toll. */
    record TargetStacksAtLeastStat(EffectKind kind, TowerStat stat) implements HitCondition {
        @Override
        public boolean holds(HitContext context) {
            return context.target().effectStacks(this.kind) >= Math.round(context.shot().value(this.stat));
        }

        @Override
        public String describe() {
            return "at full " + Words.name(this.kind);
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
        public String describe() {
            return String.join(", ", this.conditions.stream().map(HitCondition::describe).filter(s -> !s.isEmpty()).toList());
        }
    }
}
