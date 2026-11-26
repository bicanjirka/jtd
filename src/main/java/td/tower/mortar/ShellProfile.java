package td.tower.mortar;

import td.damage.Damage;
import td.effect.EffectKind;
import td.zone.ZoneKind;
import td.zone.ZonePulse;

import java.util.List;
import java.util.Optional;

/**
 * What one kind of shell does as the Mortar fires it now: the hit its core deals, how fast it flies, its blast, and
 * the ground it leaves.
 *
 * @param hit            what its core deals, before the enemy's defences
 * @param cellsPerSecond how fast it flies
 * @param ground         the ground it leaves; empty for a shell that leaves none
 * @param carry          the share of a payload's pulse its blast carries; empty for a shell that carries none
 */
public record ShellProfile(ShellKind kind, Damage hit, float cellsPerSecond, Blast blast, Optional<Ground> ground,
                           Optional<Carry> carry) {

    /** A shell that leaves no ground and carries no payload. */
    public static ShellProfile of(ShellKind kind, Damage hit, float cellsPerSecond, Blast blast) {
        return new ShellProfile(kind, hit, cellsPerSecond, blast, Optional.empty(), Optional.empty());
    }

    public ShellProfile withGround(Ground left) {
        return new ShellProfile(this.kind, this.hit, this.cellsPerSecond, this.blast, Optional.of(left), this.carry);
    }

    public ShellProfile withCarry(Carry carried) {
        return new ShellProfile(this.kind, this.hit, this.cellsPerSecond, this.blast, this.ground,
                Optional.of(carried));
    }

    /** The payload it carries a share of; empty for a shell that carries none. */
    public Optional<ShellKind> carried() {
        return this.carry.map(Carry::payload);
    }

    /**
     * The ground a shell leaves, as the Mortar would leave it now.
     *
     * @param lifetimeTicks how long it lasts; {@code Zone.UNTIL_WAVE_END} for ground that lasts the wave
     * @param pulse         what each pulse does to an enemy inside
     */
    public record Ground(ZoneKind kind, float radiusCells, int lifetimeTicks, ZonePulse pulse) {
    }

    /**
     * What a Normal right behind a payload puts on what its blast reaches, past its own hit.
     *
     * @param ground the kind of ground {@code payload} leaves, which says what {@code share} does
     * @param share  the share of one pulse of that ground
     */
    public record Carry(ShellKind payload, ZoneKind ground, ZonePulse share) {
    }

    /**
     * How far a shell's blast reaches and what it leaves on what it reaches.
     *
     * @param coreCells the full-damage zone
     * @param edgeCells the edge, as wide as the core when it has none
     * @param edgeShare what the edge deals, a share of the core's hit
     * @param applies   the effects it puts on enemies, its blast's and its ground's, in the effects' order
     */
    public record Blast(float coreCells, float edgeCells, float edgeShare, List<EffectKind> applies) {

        public Blast {
            applies = List.copyOf(applies);
        }

        /** Whether it hits anything past its core. */
        public boolean hasEdge() {
            return this.edgeCells > this.coreCells && this.edgeShare > 0f;
        }
    }
}
