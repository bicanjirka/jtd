package td.effect;

/**
 * One tower's own share of a {@link EffectKind#BURN}'s fuel pool - {@code amount} decays
 * alongside every other contribution's, and {@code sink} is what {@link ActiveEffects#tickBurn}
 * credits that share's damage through, so a reapplication from a second tower never displaces
 * the first tower's own credit. See {@code ActiveEffects#applyBurn}/{@code #tickBurn} and
 * {@code td/effect/CLAUDE.md}.
 */
record BurnContribution(DamageSink sink, float amount) {

    BurnContribution decayedBy(float alpha) {
        return new BurnContribution(this.sink, this.amount * alpha);
    }
}
