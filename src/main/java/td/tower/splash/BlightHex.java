package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.targeting.SoonestOutSelector;
import td.util.TickRate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Hex of Blight: poison, and a slow that grows over its time. Picks the enemy that would leave soonest, one without
 * Blight first, so it spreads before it deepens.
 */
public final class BlightHex implements Hex {

    private static final float SECONDS = 4f;
    /** The slow it has grown to when it ends, as it was cast. */
    private static final float SLOW_AT_END = 0.3f;
    /** The poison's fuel, in points of magic damage. */
    private static final float POISON_POINTS = 7f;
    /** How much its slow grows a tick, as cast. */
    public static final float SLOW_PER_TICK = SLOW_AT_END / (SECONDS * TickRate.TICKS_PER_SECOND);

    @Override
    public EffectKind kind() {
        return EffectKind.BLIGHT;
    }

    @Override
    public float seconds() {
        return SECONDS;
    }

    @Override
    public Optional<EnemyMob> target(List<EnemyMob> candidates) {
        SoonestOutSelector soonest = new SoonestOutSelector();
        Optional<EnemyMob> fresh = soonest.selectFrom(candidates.stream()
                .filter(enemy -> !enemy.hasEffect(EffectKind.BLIGHT))
                .toList());
        return fresh.or(() -> soonest.selectFrom(candidates));
    }

    @Override
    public void lay(EnemyMob target, int ticks, float grownSlow, HexHand hand) {
        hand.blight(target, ticks, SLOW_PER_TICK, grownSlow);
        hand.poison(target, POISON_POINTS);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.BLIGHT, EffectKind.POISON);
    }

    @Override
    public TermText describe() {
        return TermText.of(EffectKind.BLIGHT, TermText.of("for " + TermNumbers.seconds(SECONDS) + ": ")
                .plus(EffectKind.POISON).plus(" of " + TermNumbers.number(POISON_POINTS)
                        + " magic damage, and a slow that grows steadily to "
                        + TermNumbers.percent(SLOW_AT_END) + " at its end; cast again, it starts over and never slows less"));
    }
}
