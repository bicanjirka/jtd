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
            Optional.of(ShellZone.fixed(ZoneKind.BURNING_GROUND, 1f, 60, 0.04f))),
    TAR("tar", ShellLook.TAR, DamageType.PHYSICAL, Optional.of(ShellZone.fixed(ZoneKind.TAR, 1.2f, 80, 0.03f))),
    CRYO("cryo", ShellLook.FROST, DamageType.PHYSICAL,
            Optional.of(ShellZone.fixed(ZoneKind.FROST_GROUND, 1.2f, 60, 0f))),
    /** The Tactical Nuke: it never carries a special, and its fallout covers most of its blast. */
    NUKE("nuke", ShellLook.NUKE, DamageType.PHYSICAL,
            Optional.of(ShellZone.ofBlast(ZoneKind.FALLOUT, 0.8f, 80)));

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
     * The ground a shell leaves where it lands: {@code radiusCells} wide, or {@code blastShare} of
     * the blast's own radius when that is above zero.
     *
     * @param lifetimeTicks how long it lasts
     * @param damageShare   a burn's or poison's damage a tick, as a share of the Mortar's damage
     */
    public record ShellZone(ZoneKind kind, float radiusCells, float blastShare, int lifetimeTicks,
                            float damageShare) {

        static ShellZone fixed(ZoneKind kind, float radiusCells, int lifetimeTicks, float damageShare) {
            return new ShellZone(kind, radiusCells, 0f, lifetimeTicks, damageShare);
        }

        static ShellZone ofBlast(ZoneKind kind, float blastShare, int lifetimeTicks) {
            return new ShellZone(kind, 0f, blastShare, lifetimeTicks, 0f);
        }

        /** Its radius in pixels for a blast of {@code blastRadius}. */
        public float radius(int cellSize, float blastRadius) {
            return this.blastShare > 0f ? this.blastShare * blastRadius : this.radiusCells * cellSize;
        }
    }
}
