package td.tower.mortar;

import td.damage.DamageType;
import td.projectile.ShellLook;
import td.zone.ZoneKind;

import java.util.Optional;

/**
 * What one shell carries: how it looks in flight, the kind of damage its blast does, and the zone it
 * leaves where it lands, if any.
 */
public enum ShellType {
    PLAIN("plain", ShellLook.PLAIN, DamageType.PHYSICAL, Optional.empty()),
    /** The Mortar's one way to deal magic: its blast burns, and the ground where it lands too. */
    NAPALM("napalm", ShellLook.NAPALM, DamageType.MAGIC,
            Optional.of(new ShellZone(ZoneKind.BURNING_GROUND, 1f, 60, 0.04f))),
    TAR("tar", ShellLook.TAR, DamageType.PHYSICAL, Optional.of(new ShellZone(ZoneKind.TAR, 1.2f, 80, 0.03f))),
    CRYO("cryo", ShellLook.FROST, DamageType.PHYSICAL,
            Optional.of(new ShellZone(ZoneKind.FROST_GROUND, 1.2f, 60, 0f)));

    private final String label;
    private final ShellLook look;
    private final DamageType damageType;
    private final Optional<ShellZone> zone;

    ShellType(String label, ShellLook look, DamageType damageType, Optional<ShellZone> zone) {
        this.label = label;
        this.look = look;
        this.damageType = damageType;
        this.zone = zone;
    }

    public String label() {
        return this.label;
    }

    public ShellLook look() {
        return this.look;
    }

    public DamageType damageType() {
        return this.damageType;
    }

    public Optional<ShellZone> zone() {
        return this.zone;
    }

    /**
     * The ground a shell leaves where it lands.
     *
     * @param radiusCells   how wide the patch is
     * @param lifetimeTicks how long it lasts
     * @param damageShare   a burn's or poison's damage a tick, as a share of the Mortar's damage; none for frost
     */
    public record ShellZone(ZoneKind kind, float radiusCells, int lifetimeTicks, float damageShare) {
    }
}
