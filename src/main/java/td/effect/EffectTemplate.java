package td.effect;

/**
 * An {@link Ability}'s authored description of an {@link Effect} to apply, before a
 * {@link DamageSink} is bound - unlike a live {@code Effect}, a template carries no sink of
 * its own, since which mob's {@code dealDamage} (or no-op, for a non-damaging kind like
 * {@link ShieldTemplate}/{@link InvisibleTemplate}) it credits through is only known at the
 * moment an ability actually fires, not when the ability itself is authored as data.
 */
public sealed interface EffectTemplate permits ShieldTemplate, InvisibleTemplate {

    Effect toEffect(DamageSink sink);
}
