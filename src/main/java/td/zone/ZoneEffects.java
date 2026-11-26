package td.zone;

import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.util.TickRate;

/**
 * What one pulse of a zone puts on an enemy standing in it, so something that is not a zone (a piece
 * of shrapnel) can hand the same effect to an enemy it hits directly.
 */
public final class ZoneEffects {

    /** The ticks a pulse lasts before the next one: twice a second. */
    static final int PULSE_TICKS = ZoneRoster.PULSE_TICKS;

    /** A flare reveals an enemy this long after its last pulse. */
    private static final int FLARE_REVEAL_TICKS = Math.round(3 * TickRate.TICKS_PER_SECOND);
    /** Sickened stacks fallout adds each pulse: more than spirit wears off in half a second. */
    private static final int FALLOUT_SICKENED_STACKS = 4;
    private static final int FALLOUT_HOLD_TICKS = 2 * PULSE_TICKS;
    /** Healing and haste hold this long after a pulse, so the next one lands before they lapse. */
    private static final int BOON_TICKS = 2 * PULSE_TICKS;

    private ZoneEffects() {
    }

    /**
     * One {@code pulse} of {@code kind} on {@code enemy}, credited to {@code owner}: its strength as the kind takes it
     * (a pool's fuel, a direct hit of magic, a heal) and the slow it holds. A freeze is a matter of how long an enemy
     * stays, so it is the zone's own to add.
     */
    public static void touch(ZoneKind kind, ZonePulse pulse, ZoneOwner owner, EnemyMob enemy) {
        int strength = pulse.strength();
        if (strength > 0) {
            Damage magic = Damage.magic(strength);
            kind.fuels().ifPresent(pool -> owner.applyEffect(enemy, sink -> fuel(pool, magic, sink)));
            if (kind.hitsDirectly()) {
                owner.dealPeriodicDamage(enemy, magic);
            }
        }
        ZonePulse.Slow slow = pulse.slow();
        if (slow.slows()) {
            kind.slows().ifPresent(by -> owner.applyEffect(enemy, sink -> slowed(by, slow, sink)));
        }
        switch (kind) {
            case BURNING_GROUND, TAR, FROST_GROUND -> {
            }
            case HEALING_GROUND -> owner.applyEffect(enemy, sink -> Effect.heal(strength, BOON_TICKS, sink));
            case HASTE_GROUND -> owner.applyEffect(enemy, sink -> Effect.hastened(BOON_TICKS, sink));
            case WARD_MINE, BLINDING_FIELD -> {
            }
            case FALLOUT -> {
                owner.applyEffect(enemy, sink -> Effect.sickened(FALLOUT_SICKENED_STACKS));
                owner.applyEffect(enemy, sink -> Effect.deadZone(FALLOUT_HOLD_TICKS, sink));
            }
            case FLARE -> owner.applyEffect(enemy, sink -> Effect.revealed(FLARE_REVEAL_TICKS, sink));
        }
    }

    private static Effect fuel(EffectKind pool, Damage fuel, DamageSink sink) {
        return switch (pool) {
            case BURN -> Effect.burn(fuel, sink);
            case POISON -> Effect.poison(fuel, sink);
            default -> throw new IllegalArgumentException(pool + " is no pool a zone feeds");
        };
    }

    private static Effect slowed(EffectKind by, ZonePulse.Slow slow, DamageSink sink) {
        return switch (by) {
            case TARRED -> Effect.tarred(slow.strength(), slow.holdTicks(), sink);
            case CHILL -> Effect.chill(slow.strength(), slow.holdTicks(), sink);
            default -> throw new IllegalArgumentException(by + " is no slow a zone holds");
        };
    }
}
