package td.enemy;

import td.effect.EffectKind;
import td.effect.PeriodicDamageListener;
import td.util.ThreadConfined;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What one mob's trace needs between its events: the effects it last reported, the effect dealing the periodic
 * damage now landing, and its totals. An unobserved mob holds {@link #off()}, which the mob asks before it builds
 * any event, so a mob nobody watches builds none.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
final class MobObserver implements PeriodicDamageListener {

    /** The cause a shield's drain under a hit reports under. */
    static final String ABSORBED = "absorbed";

    private static final MobObserver OFF = new MobObserver(MobEventSink.none(), false);

    private final MobEventSink sink;
    private final boolean on;

    private Map<EffectKind, EffectState> effects = Map.of();
    private Optional<EffectKind> periodicSource = Optional.empty();
    private MobTotals totals = MobTotals.none();

    private MobObserver(MobEventSink sink, boolean on) {
        this.sink = sink;
        this.on = on;
    }

    /** Shared by every unobserved mob, so it never changes. */
    static MobObserver off() {
        return OFF;
    }

    static MobObserver reportingTo(MobEventSink sink) {
        return new MobObserver(sink, true);
    }

    boolean isOn() {
        return this.on;
    }

    void report(MobEvent event) {
        this.totals = this.totals.plus(event);
        this.sink.report(event);
    }

    /** What its effects are when observation begins: the baseline the first change is measured from. */
    void baseline(Map<EffectKind, EffectState> now) {
        this.effects = now;
    }

    /** Reports every change since the last look, under {@code cause}. */
    void effectsNow(Map<EffectKind, EffectState> now, String cause) {
        EffectDiff.between(this.effects, now, cause).forEach(this::report);
        this.effects = now;
    }

    /**
     * Reports what applying {@code kind} changed; if it changed nothing of its own kind, says so and why.
     *
     * @param keptOutBy the active kind that keeps it out, if one does
     */
    void effectApplied(Map<EffectKind, EffectState> now, EffectKind kind, Optional<EffectKind> keptOutBy) {
        List<MobEvent.EffectChanged> changes = EffectDiff.between(this.effects, now, "applied:" + kind.title());
        changes.forEach(this::report);
        if (changes.stream().noneMatch(change -> change.kind() == kind)) {
            EffectState state = Optional.ofNullable(now.get(kind)).orElseGet(() -> EffectStates.absent(kind));
            this.report(new MobEvent.EffectChanged(MobEvent.Change.UNCHANGED, state, state,
                    keptOutBy.map(by -> "kept-out-by:" + by.title()).orElse("no-change")));
        }
        this.effects = now;
    }

    MobTotals totals() {
        return this.totals;
    }

    /** The effect behind the periodic damage landing now, if an effect tick named one. */
    Optional<EffectKind> periodicSource() {
        return this.periodicSource;
    }

    @Override
    public void dealing(EffectKind kind) {
        if (this.on) {
            this.periodicSource = Optional.of(kind);
        }
    }

    /** The effects are done dealing: periodic damage from now on comes from elsewhere until named again. */
    void dealt() {
        if (this.on) {
            this.periodicSource = Optional.empty();
        }
    }
}
