package td.enemy;

import td.zone.Zone;
import td.zone.ZoneKind;

/**
 * Leaves a zone where the caster stands. {@code power} is what the kind needs: the points a healing ground heals each
 * tick, the points a ward mine's shield holds, the chance a blinding field makes a tower miss; a haste ground has none.
 *
 * @param radiusCells   how far the zone reaches
 * @param lifetimeTicks how long it lasts, or {@link Zone#UNTIL_WAVE_END}
 */
public record LeaveZoneAction(ZoneKind kind, float radiusCells, int lifetimeTicks, float power)
        implements AbilityAction {

    public static LeaveZoneAction healingGround(float radiusCells, int lifetimeTicks, float pointsPerTick) {
        return new LeaveZoneAction(ZoneKind.HEALING_GROUND, radiusCells, lifetimeTicks, pointsPerTick);
    }

    public static LeaveZoneAction hasteGround(float radiusCells, int lifetimeTicks) {
        return new LeaveZoneAction(ZoneKind.HASTE_GROUND, radiusCells, lifetimeTicks, 0f);
    }

    public static LeaveZoneAction wardMine(float radiusCells, int lifetimeTicks, float shieldPoints) {
        return new LeaveZoneAction(ZoneKind.WARD_MINE, radiusCells, lifetimeTicks, shieldPoints);
    }

    /** Lasts until the wave is over. */
    public static LeaveZoneAction blindingField(float radiusCells, float missChance) {
        return new LeaveZoneAction(ZoneKind.BLINDING_FIELD, radiusCells, Zone.UNTIL_WAVE_END, missChance);
    }

    @Override
    public boolean appliesEffects() {
        return this.kind != ZoneKind.BLINDING_FIELD;
    }
}
