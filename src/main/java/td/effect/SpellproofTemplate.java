package td.effect;

/** {@code stacks} stacks of magic resist for {@code durationTicks}. */
public record SpellproofTemplate(int stacks, int durationTicks) implements EffectTemplate {

    @Override
    public Effect toEffect(DamageSink sink) {
        return Effect.spellproof(this.stacks, this.durationTicks, sink);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.SPELLPROOF;
    }
}
