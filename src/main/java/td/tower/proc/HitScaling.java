package td.tower.proc;

import td.effect.EffectKind;

/** How many times a hit bonus applies to one hit; each record works it out and says it. */
public sealed interface HitScaling {

    float factor(HitContext context);

    String describe();

    /** Once. */
    record Once() implements HitScaling {
        @Override
        public float factor(HitContext context) {
            return 1f;
        }

        @Override
        public String describe() {
            return "";
        }
    }

    /** Once per step of the tower's focus, up to {@code max}; 0 for no max. */
    record PerFocus(int max) implements HitScaling {
        @Override
        public float factor(HitContext context) {
            return this.max == 0 ? context.focus() : Math.min(context.focus(), this.max);
        }

        @Override
        public String describe() {
            return this.max == 0 ? "per stack" : "per stack, up to " + this.max;
        }
    }

    /** By how far out the target stands, in full at the tower's range. */
    record PerRangeShare() implements HitScaling {
        @Override
        public float factor(HitContext context) {
            return context.rangeShare();
        }

        @Override
        public String describe() {
            return "up to, at max range";
        }
    }

    /** Once per stack of {@code kind} on the target. */
    record PerStackOf(EffectKind kind) implements HitScaling {
        @Override
        public float factor(HitContext context) {
            return context.target().effectStacks(this.kind);
        }

        @Override
        public String describe() {
            return "per " + Words.name(this.kind) + " stack";
        }
    }
}
