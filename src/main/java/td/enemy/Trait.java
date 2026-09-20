package td.enemy;

import td.damage.Damage;
import td.effect.EffectKind;

/**
 * A passive, always-on modifier of an enemy's properties or of incoming interactions -
 * generalizes what used to be a hand-written override on one of the five now-retired leaf
 * enemy classes (see {@link PercentResistTrait}/{@link HurtSpeedTrait}, which reproduce
 * Square's old resistance and Triangle's old hurt-speed curve). {@link #onHit}/
 * {@link #isValidTarget}/{@link #speedFactor} each have a no-op default, so a definition with no
 * traits at all (Circle's case) needs no special-cased "no traits" branch anywhere that reads
 * one - an empty {@code List<Trait>} already behaves exactly like the old absence of any
 * override. {@link #marker()} is the one exception, deliberately non-default: it names the
 * glyph {@code td.ui.EnemyFrameBuilder} draws for this trait, so a new implementation is a
 * compile error until its visual exists, the same discipline `EffectKind`'s marker mapping
 * already enforces.
 * <p>
 * A single {@link Trait} instance is shared by every mob built from the same
 * {@link EnemyDefinition}, regardless of which wave's level spawned it - {@link TraitContext}
 * is what lets {@link PercentResistTrait}/{@link HurtSpeedTrait} (Square's resistance,
 * Triangle's hurt curve) stay level-scaled without needing a fresh instance per mob.
 * <p>
 * Note: invisibility does <b>not</b> route through {@link #isValidTarget} at all, for the Ghost
 * or any other enemy - see {@code AbstractEnemyMob.effectiveType()} and {@code td/enemy/CLAUDE.md}
 * for the actual (ability/effect-driven) mechanism. {@link #isValidTarget} stays available for a
 * future trait that makes a mob untargetable through some other means entirely.
 */
public interface Trait {

    /**
     * Resists (or otherwise modifies) an incoming hit - generalizes {@code absorb}.
     */
    default Damage onHit(Damage incoming, TraitContext context) {
        return incoming;
    }

    /**
     * Whether this trait alone makes the mob permanently untargetable, independent of {@link EnemyMob.Type}.
     */
    default boolean isValidTarget(TraitContext context) {
        return true;
    }

    /**
     * A multiplier on intrinsic speed - generalizes Triangle's hurt curve.
     */
    default float speedFactor(TraitContext context) {
        return 1f;
    }

    /**
     * Whether this trait blocks an incoming {@link EffectKind} outright, before it is ever
     * applied - see {@link BurnImmunityTrait}/{@link FreezeImmunityTrait}. Unlike {@link #onHit},
     * which only ever sees an instant hit, this is consulted by {@link DefinedEnemyMob#applyEffect}
     * for a status effect a tower or ability tries to apply directly.
     */
    default boolean blocksEffect(EffectKind kind) {
        return false;
    }

    /**
     * The glyph naming this trait in the second, trait-marker row {@code td.ui.EnemyFrameBuilder}
     * draws below a mob's body - see {@link TraitMarker}.
     */
    TraitMarker marker();
}
