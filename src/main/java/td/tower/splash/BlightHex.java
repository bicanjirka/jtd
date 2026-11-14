package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.targeting.MostNeighboursSelector;
import td.util.TickRate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Hex of Blight: poison, and a slow that grows over its time. Picks the most crowded enemy, one without Blight first,
 * so it spreads before it deepens.
 */
public final class BlightHex implements Hex {

    private static final float SECONDS = 4f;
    /** The slow it has grown to when it ends, as it was cast. */
    private static final float SLOW_AT_END = 0.3f;
    private static final float POISON_SHARE = 0.03f;
    private static final float SLOW_PER_TICK = SLOW_AT_END / (SECONDS * TickRate.TICKS_PER_SECOND);

    @Override
    public EffectKind kind() {
        return EffectKind.BLIGHT;
    }

    @Override
    public float seconds() {
        return SECONDS;
    }

    @Override
    public Optional<EnemyMob> target(List<EnemyMob> candidates, float cellPixels) {
        MostNeighboursSelector crowded = new MostNeighboursSelector(HexSpec.CLOUD_CELLS * cellPixels);
        Optional<EnemyMob> fresh = crowded.selectFrom(candidates.stream()
                .filter(enemy -> !enemy.hasEffect(EffectKind.BLIGHT))
                .toList());
        return fresh.isPresent() ? fresh : crowded.selectFrom(candidates);
    }

    @Override
    public void lay(EnemyMob target, int ticks, float grownSlow, HexHand hand) {
        hand.blight(target, ticks, SLOW_PER_TICK, grownSlow);
        hand.poison(target, POISON_SHARE, ticks);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.BLIGHT, EffectKind.POISON);
    }

    @Override
    public TermText describe() {
        return TermText.of(EffectKind.BLIGHT, TermText.of("for " + TermNumbers.seconds(SECONDS) + ": ")
                .plus(EffectKind.POISON).plus(" of " + TermNumbers.percent(POISON_SHARE)
                        + " of base damage a tick, and a slow that grows steadily to "
                        + TermNumbers.percent(SLOW_AT_END) + " at its end; cast again, it starts over and never slows less"));
    }
}
