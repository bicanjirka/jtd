package td.effect;

/**
 * An {@link td.enemy.Ability}'s authored description of an {@link Effect} to apply, before a
 * {@link DamageSink} is bound - unlike a live {@code Effect}, a template carries no sink of
 * its own, since which mob's {@code dealDamage} (or no-op, for a non-damaging kind like
 * {@link ShieldTemplate}/{@link InvisibleTemplate}) it credits through is only known at the
 * moment an ability actually fires, not when the ability itself is authored as data.
 */
public sealed interface EffectTemplate permits ShieldTemplate, InvisibleTemplate, HealTemplate {

    Effect toEffect(DamageSink sink);

    /**
     * Which {@link EffectKind} this template produces, without building an {@link Effect} - lets
     * a consumer (e.g. {@code EnemyDefinition.supportAura}) ask what an authored ability projects
     * without switching on this sealed set itself.
     */
    EffectKind kind();
}
