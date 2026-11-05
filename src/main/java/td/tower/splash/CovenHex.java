package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.targeting.HighestHealthSelector;

import java.util.Optional;
import java.util.Set;

/**
 * Either side of the coven's exclusive choice: both curse with Malaise, and each adds one thing as it is cast. Picks
 * the healthiest enemy without it.
 */
public final class CovenHex implements Hex {

    private static final float RIME_CHILL = 0.3f;
    private static final float ASH_BURN_SHARE = 0.1f;

    private final EffectKind addedOnCast;

    private CovenHex(EffectKind addedOnCast) {
        this.addedOnCast = addedOnCast;
    }

    /** Rime Coven's: it chills as it is cast. */
    public static CovenHex rime() {
        return new CovenHex(EffectKind.CHILL);
    }

    /** Ash Coven's: it burns as it is cast. */
    public static CovenHex ash() {
        return new CovenHex(EffectKind.BURN);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.MALAISE;
    }

    @Override
    public Optional<EnemyMob> target(HexScene scene) {
        return new HighestHealthSelector().selectFrom(scene.candidates().stream()
                .filter(enemy -> !enemy.hasEffect(EffectKind.MALAISE))
                .toList());
    }

    @Override
    public void cast(EnemyMob target, HexSpec spec, HexActions actions) {
        int ticks = this.ticksOn(target);
        actions.curse(target, EffectKind.MALAISE, ticks);
        if (this.addedOnCast == EffectKind.CHILL) {
            actions.chill(target, RIME_CHILL, ticks);
        } else {
            actions.burn(target, ASH_BURN_SHARE, ticks);
        }
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.MALAISE, this.addedOnCast);
    }

    @Override
    public TermText describe() {
        TermText onCast = this.addedOnCast == EffectKind.CHILL
                ? TermText.of("applies " + TermNumbers.percent(RIME_CHILL) + " ").plus(EffectKind.CHILL)
                : TermText.of("applies ").plus(EffectKind.BURN).plus(" from " + TermNumbers.percent(ASH_BURN_SHARE)
                        + " of the damage");
        return TermText.of(EffectKind.MALAISE, onCast.plus(" when cast; ").plus(EffectKind.MALAISE.describe()));
    }
}
