package td.effect;

/**
 * An ability's authored {@link Effect}, before a {@link DamageSink} is bound: which sink applies is
 * only known when the ability fires.
 */
public sealed interface EffectTemplate permits ShieldTemplate, InvisibleTemplate, HealTemplate {

    Effect toEffect(DamageSink sink);

    /** The kind this template produces, without building an {@link Effect}. */
    EffectKind kind();
}
