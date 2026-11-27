package td.tower.splash;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.tower.ability.Ability;

import java.util.Set;

/**
 * What a spell hits or hexes is left Vulnerable: for as long as its hex lasts, or a while after its lightning. A spell
 * is any ability that costs the Burst's currencies, so one added later is a spell without naming it.
 */
public final class SpellMarkMechanic implements SplashMechanic {

    /** How long a spell's lightning leaves its enemy Vulnerable, in seconds. */
    public static final float JOLT_SECONDS = 5f;

    private static final Set<Term> SPELL_CURRENCIES = Set.of(TowerTerm.SPARK, TowerTerm.SOUL);

    /** Whether {@code ability} is a spell: it costs Sparks or Souls. */
    public static boolean isSpell(Ability ability) {
        return SPELL_CURRENCIES.contains(ability.currency());
    }

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withSpellsMarking();
    }

    @Override
    public TermText describe() {
        return TermText.of("every enemy a spell (an ability that costs ").plus(TowerTerm.SPARK).plus("s or ")
                .plus(TowerTerm.SOUL).plus("s) hits or hexes is ").plus(EffectKind.VULNERABLE)
                .plus(": while its ").plus(TowerTerm.HEX).plus(" lasts, or for "
                        + TermNumbers.seconds(JOLT_SECONDS) + " after its lightning");
    }
}
