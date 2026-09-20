package td.enemy;

/**
 * Names which glyph {@code td.ui.EnemyFrameBuilder} draws for a {@link Trait} in the
 * trait-marker row below a mob's body - a small, closed set, the same shape {@link
 * td.effect.EffectKind}'s own marker mapping uses for the timed status row above it. A new
 * {@link Trait} implementation names its own marker via {@link Trait#marker()}, which is
 * deliberately non-default so a new trait is a compile error until its glyph exists here and in
 * {@code td.ui.EnemyFrameBuilder}/{@code Java2DFrameRenderer}.
 */
public enum TraitMarker {
    PERCENT_RESIST,
    FLAT_RESIST,
    CRITICAL_IMMUNE,
    HURT_SPEED,
    BURN_IMMUNE,
    FREEZE_IMMUNE
}
