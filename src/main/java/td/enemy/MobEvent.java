package td.enemy;

import td.damage.Damage;
import td.effect.EffectKind;
import td.zone.ZoneKind;

import java.util.List;
import java.util.Optional;

/**
 * One thing that happened to an observed enemy, as its trace reports it. Health and damage are in units; a
 * cause or attacker is already named ({@code Sniper@7,4}, {@code #12}, {@code dev:kill} or {@code -}).
 */
public sealed interface MobEvent {

    /** Observation began, {@code via} saying how: selected, from the spawn box, or from an observed parent. */
    record Started(String via, MobSnapshot snapshot) implements MobEvent {
    }

    /** A full look at the enemy, asked for while it is observed. */
    record Snapshot(MobSnapshot snapshot) implements MobEvent {
    }

    record Hit(HitCause cause, HitSteps steps, HitOutcome outcome) implements MobEvent {
    }

    record Dodged(String from, Damage arrived) implements MobEvent {
    }

    /** The hit that killed it, and how much of that hit health could not take. */
    record Killed(String by, int overkill) implements MobEvent {
    }

    /** An effect arriving, before resistance, diminishing returns or anything active decides what it does. */
    record EffectRequested(EffectKind kind, String from, int ticks) implements MobEvent {
    }

    /** Resistance and diminishing returns cut an effect to {@code ticks}. */
    record EffectShortened(EffectKind kind, float resisted, float diminished, int ticks) implements MobEvent {
    }

    record EffectBlocked(EffectKind kind, String why) implements MobEvent {
    }

    /** Inversion turned a heal or a shield into damage. */
    record EffectInverted(EffectKind kind, String into) implements MobEvent {
    }

    /** One effect changed in a way a trace reports: never a level that only fades. */
    record EffectChanged(Change change, EffectState before, EffectState after, String cause) implements MobEvent {

        public EffectKind kind() {
            return this.after.kind();
        }
    }

    record HealthChanged(String cause, int before, int after) implements MobEvent {
    }

    /** An ability put an effect on itself or on {@code targets} others within {@code radiusCells}. */
    record Cast(EffectKind kind, float radiusCells, int targets) implements MobEvent {
    }

    record ZoneLeft(ZoneKind kind, float radiusCells) implements MobEvent {
    }

    /** It spawned these enemies, or hatched into the one. */
    record Spawned(List<Long> ids, boolean hatched) implements MobEvent {

        public Spawned {
            ids = List.copyOf(ids);
        }
    }

    /** Always the last event: how it ended, how long it lived and what it took meanwhile. */
    record Ended(String how, int livedTicks, MobTotals totals, List<EffectState> effects) implements MobEvent {

        public Ended {
            effects = List.copyOf(effects);
        }
    }

    enum Change {
        GAINED, REFRESHED, STACKS, TOPPED_UP, WEAKENED, ENDED, UNCHANGED
    }

    /**
     * @param source   the effect a periodic hit came from, when one was named
     * @param markSpent whether the hit spent a mark, which made it a guaranteed crit
     */
    record HitCause(String from, Optional<EffectKind> source, boolean markSpent) {
    }

    /** What happened after the formula: the shield, the cap at health, and health either side. */
    record HitOutcome(int absorbed, Damage landed, int healthBefore, int healthAfter) {
    }
}
