package td.tower.mortar;

import td.damage.Damage;
import td.effect.EffectKind;

import java.util.List;
import java.util.Optional;

/**
 * What one kind of shell does as the Mortar fires it now: the hit its core deals, how fast it flies, its blast, and
 * what its ground deals.
 *
 * @param hit            what its core deals, before the enemy's defences
 * @param cellsPerSecond how fast it flies
 * @param groundPulse    the fuel each pulse of its ground adds to a burn or a poison; empty for ground that deals none
 */
public record ShellProfile(ShellKind kind, Damage hit, float cellsPerSecond, Blast blast, Optional<Damage> groundPulse) {

    /** A shell whose ground, if it leaves one, deals nothing. */
    public static ShellProfile of(ShellKind kind, Damage hit, float cellsPerSecond, Blast blast) {
        return new ShellProfile(kind, hit, cellsPerSecond, blast, Optional.empty());
    }

    public ShellProfile withGroundPulse(Damage fuel) {
        return new ShellProfile(this.kind, this.hit, this.cellsPerSecond, this.blast, Optional.of(fuel));
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
