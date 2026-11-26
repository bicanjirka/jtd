package td.tower.mortar;

import java.util.Optional;

/**
 * A shell as the Mortar holds and fires it: its kind, and the payload it carries a share of with Coated Shells, the one
 * right in front of it when it is a Normal.
 *
 * @param carried the payload right in front of it, for a Normal; empty otherwise
 */
public record HeldShell(ShellKind kind, Optional<ShellKind> carried) {

    /** {@code kind} with {@code inFront} right in front of it: a Normal carries it when it is a payload. */
    public static HeldShell behind(ShellKind kind, Optional<ShellKind> inFront) {
        return new HeldShell(kind, kind == ShellKind.NORMAL ? inFront.filter(ShellKind::isPayload) : Optional.empty());
    }

    /** The same shell carrying nothing, for a Mortar without Coated Shells. */
    public HeldShell plain() {
        return new HeldShell(this.kind, Optional.empty());
    }
}
