package td.effect;

import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.List;

/**
 * One enemy's diminishing returns for one kind of hard crowd control: within a window of the last
 * fresh application, each fresh one lasts a smaller share of its duration, down the kind's own
 * ladder, until the last step keeps it out. Each kind climbs its own ladder; they never share one.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class DiminishingReturns {

    public static final int WINDOW_TICKS = Math.round(10 * TickRate.TICKS_PER_SECOND);
    private static final float[] FREEZE_STEPS = {1f, 0.5f, 0.25f, 0f};
    private static final float[] DAZED_STEPS = {1f, 0.8f, 0.6f, 0.4f, 0.2f, 0f};

    private final float[] steps;
    private int freshApplications;
    private int lastApplicationTick = -1;

    private DiminishingReturns(float[] steps) {
        this.steps = steps;
    }

    /** The ladder {@code kind} climbs; only hard crowd control has one. */
    public static DiminishingReturns of(EffectKind kind) {
        return switch (kind) {
            case FREEZE -> new DiminishingReturns(FREEZE_STEPS);
            case DAZED -> new DiminishingReturns(DAZED_STEPS);
            default -> throw new IllegalArgumentException(kind + " does not diminish");
        };
    }

    /** The ladder for {@code kind} as an unmodifiable list; empty for a kind that does not diminish. */
    static List<Float> stepsOf(EffectKind kind) {
        float[] steps = switch (kind) {
            case FREEZE -> FREEZE_STEPS;
            case DAZED -> DAZED_STEPS;
            default -> new float[0];
        };
        List<Float> list = new ArrayList<>();
        for (float step : steps) {
            list.add(step);
        }
        return List.copyOf(list);
    }

    /** The duration factor an application at {@code tick} gets, without recording it. */
    public float factorAt(int tick, boolean alreadyActive) {
        int step = this.stepAt(tick);
        if (alreadyActive && step > 0) {
            return this.steps[step - 1];
        }
        return this.steps[Math.min(step, this.steps.length - 1)];
    }

    /**
     * Records an application that landed at {@code tick}. Only a fresh one climbs a step and opens the window
     * again; one on an enemy already held changes neither.
     */
    public void recordAt(int tick, boolean alreadyActive) {
        if (alreadyActive) {
            return;
        }
        this.freshApplications = this.stepAt(tick) + 1;
        this.lastApplicationTick = tick;
    }

    /** How many fresh applications have landed in the current window. */
    public int stepAt(int tick) {
        if (this.lastApplicationTick < 0 || tick - this.lastApplicationTick >= WINDOW_TICKS) {
            return 0;
        }
        return this.freshApplications;
    }
}
