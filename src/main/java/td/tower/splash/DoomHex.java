package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.targeting.HighestRankSelector;

import java.util.List;
import java.util.Optional;

/**
 * Hex of Doom: when it ends, a share of all the damage the enemy took meanwhile hits it again, through shields and
 * plating. Picks the highest rank, the closest to leaking among equals. When its carrier is killed it jumps on with
 * what it recorded.
 */
public final class DoomHex implements Hex {

    private static final float SECONDS = 5f;

    @Override
    public EffectKind kind() {
        return EffectKind.DOOM;
    }

    @Override
    public float seconds() {
        return SECONDS;
    }

    @Override
    public Optional<EnemyMob> target(List<EnemyMob> candidates) {
        return new HighestRankSelector().selectFrom(candidates.stream()
                .filter(enemy -> !enemy.hasEffect(EffectKind.DOOM))
                .toList());
    }

    @Override
    public void lay(EnemyMob target, int ticks, float grownSlow, HexHand hand) {
        hand.curse(target, EffectKind.DOOM, ticks);
    }

    @Override
    public TermText describe() {
        return TermText.of(EffectKind.DOOM, TermText.of("for " + TermNumbers.seconds(SECONDS) + "; when it ends, "
                + TermNumbers.percent(HexSpec.DOOM_SHARE) + " of all the damage the enemy took meanwhile hits it again"
                + " as shadow damage; when its enemy is killed, it jumps on, recorded damage and all"));
    }
}
