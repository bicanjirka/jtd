package td.tower.pulse;

import java.util.EnumSet;
import java.util.Set;

/**
 * The rules that hold while an enemy is inside the field.
 *
 * @param active which of them hold
 */
public record ModeSpec(Set<FieldMode> active) {

    public ModeSpec {
        active = Set.copyOf(active);
    }

    /** No rules beyond the damage. */
    public static ModeSpec none() {
        return new ModeSpec(Set.of());
    }

    /** These rules, and {@code mode} too. */
    public ModeSpec with(FieldMode mode) {
        Set<FieldMode> grown = EnumSet.noneOf(FieldMode.class);
        grown.addAll(this.active);
        grown.add(mode);
        return new ModeSpec(grown);
    }

    public boolean has(FieldMode mode) {
        return this.active.contains(mode);
    }
}
