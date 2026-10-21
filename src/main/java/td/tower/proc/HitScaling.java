package td.tower.proc;

import td.effect.EffectKind;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Optional;

/** How many times a hit bonus applies to one hit; each record works it out and says it. */
public sealed interface HitScaling {

    float factor(HitContext context);

    /** The counter this scaling reads, if it reads one. */
    default Optional<TowerTerm> focusCounter() {
        return Optional.empty();
    }

    /** {@code adjustment} said with how often it applies: "+10% damage per stack of [Toll]". */
    TermText around(TermText adjustment);

    /** Once. */
    record Once() implements HitScaling {
        @Override
        public float factor(HitContext context) {
            return 1f;
        }

        @Override
        public TermText around(TermText adjustment) {
            return adjustment;
        }
    }

    /** Once per step of the tower's focus, up to {@code max}; 0 for no max. */
    record PerFocus(TowerTerm counter, int max) implements HitScaling {
        @Override
        public Optional<TowerTerm> focusCounter() {
            return Optional.of(this.counter);
        }

        @Override
        public float factor(HitContext context) {
            return this.max == 0 ? context.focus() : Math.min(context.focus(), this.max);
        }

        @Override
        public TermText around(TermText adjustment) {
            return adjustment.plus(" per stack of ").plus(this.counter)
                    .plus(this.max == 0 ? "" : ", up to " + this.max);
        }
    }

    /** By how far out the target stands, in full at the tower's range. */
    record PerRangeShare() implements HitScaling {
        @Override
        public float factor(HitContext context) {
            return context.rangeShare();
        }

        @Override
        public TermText around(TermText adjustment) {
            return TermText.of("up to ").plus(adjustment).plus(", growing to full at max range");
        }
    }

    /** Once per stack of {@code kind} on the target. */
    record PerStackOf(EffectKind kind) implements HitScaling {
        @Override
        public float factor(HitContext context) {
            return context.target().effectStacks(this.kind);
        }

        @Override
        public TermText around(TermText adjustment) {
            return adjustment.plus(" per stack of ").plus(this.kind);
        }
    }
}
