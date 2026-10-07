package td.zone;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;

/**
 * What one pulse of a zone puts on an enemy standing in it, so something that is not a zone (a piece
 * of shrapnel) can hand the same effect to an enemy it hits directly.
 */
public final class ZoneEffects {

    /** The ticks a pulse lasts before the next one: twice a second. */
    static final int PULSE_TICKS = ZoneRoster.PULSE_TICKS;

    private static final int BURN_TICKS = 60;
    private static final int POISON_TICKS = 60;
    /** Tar lets go of an enemy this long after its last pulse. */
    private static final int TARRED_TICKS = 2 * PULSE_TICKS;
    private static final float FROST_CHILL = 0.2f;
    private static final int FROST_CHILL_TICKS = 40;
    /** Sickened stacks fallout adds each pulse: more than spirit wears off in half a second. */
    private static final int FALLOUT_SICKENED_STACKS = 4;
    private static final int FALLOUT_HOLD_TICKS = 2 * PULSE_TICKS;

    private ZoneEffects() {
    }

    /**
     * One pulse of {@code kind} on {@code enemy}, credited to {@code owner}. A frost zone's freeze is
     * a matter of how long an enemy stays, so it is the zone's own to add.
     *
     * @param strength a burn's or poison's damage a tick, in damage units; unused by the kinds with none
     */
    public static void touch(ZoneKind kind, int strength, ZoneOwner owner, EnemyMob enemy) {
        switch (kind) {
            case BURNING_GROUND -> owner.applyEffect(enemy,
                    sink -> Effect.burn(Damage.magic(strength), BURN_TICKS, sink));
            case TAR -> {
                owner.applyEffect(enemy, sink -> Effect.tarred(TARRED_TICKS, sink));
                owner.applyEffect(enemy, sink -> Effect.poison(Damage.magic(strength), POISON_TICKS, sink));
            }
            case FROST_GROUND -> owner.applyEffect(enemy, sink -> Effect.chill(FROST_CHILL, FROST_CHILL_TICKS, sink));
            case MINE, CURSED_CLOUD -> {
            }
            case FALLOUT -> {
                owner.applyEffect(enemy, sink -> Effect.sickened(FALLOUT_SICKENED_STACKS));
                owner.applyEffect(enemy, sink -> Effect.deadZone(FALLOUT_HOLD_TICKS, sink));
            }
        }
    }
}
