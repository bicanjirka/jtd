package td.effect;

/** {@code stacks} stacks of magic resist, held and faded like every shared count. */
public record SpellproofTemplate(int stacks) implements EffectTemplate {

    @Override
    public Effect toEffect(DamageSink sink) {
        return Effect.spellproof(this.stacks);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.SPELLPROOF;
    }
}
