package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.Rank;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Picks an Elite or a Boss first, then the candidate closest to leaking. */
public final class HuntedFirstSelector implements TargetSelector {

    private static final Rank HUNTED_FROM = Rank.ELITE;

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        Comparator<EnemyMob> hunted = Comparator.comparing(enemy -> enemy.getRank().compareTo(HUNTED_FROM) >= 0);
        return candidates.stream().max(hunted.thenComparingInt(EnemyMob::getProgression));
    }
}
