package td.enemy;

import td.damage.Damage;

/**
 * Absorbs a fixed fraction of every incoming hit - the migrated Square's resistance. {@link
 * Damage}'s own zero-clamp turns a {@code fraction} of zero or below into full immunity, never a
 * healing hit. A stronger or weaker resistance is a different concrete instance authored at a
 * different {@link Rank}, not a formula scaled by anything live on the mob - see
 * {@code td/enemy/CLAUDE.md}'s note on why {@link TraitContext} no longer carries a level.
 */
public record PercentResistTrait(float fraction) implements Trait {

    @Override
    public Damage onHit(Damage incoming, TraitContext context) {
        return incoming.scaledBy(this.fraction);
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.PERCENT_RESIST;
    }
}
