package td.tower.mortar;

import td.damage.Damage;
import td.effect.EffectKind;

import java.util.List;

/**
 * What one kind of shell does as the Mortar fires it now: the hit its core deals, how fast it flies, and its blast.
 *
 * @param hit            what its core deals, before the enemy's defences
 * @param cellsPerSecond how fast it flies
 */
public record ShellProfile(ShellKind kind, Damage hit, float cellsPerSecond, Blast blast) {

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
