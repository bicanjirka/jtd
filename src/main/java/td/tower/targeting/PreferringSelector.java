package td.tower.targeting;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.wave.FinishLine;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Narrows the candidates to the preferred ones when any exist, then lets another selector pick
 * among them. Nothing preferred in range means it behaves as the other selector alone.
 */
public final class PreferringSelector implements TargetSelector {

    private final Predicate<EnemyMob> preferred;
    private final TargetSelector inner;

    private PreferringSelector(Predicate<EnemyMob> preferred, TargetSelector inner) {
        this.preferred = preferred;
        this.inner = inner;
    }

    /**
     * A tower that picks one target picks the enemy carrying a priority while it is in range, else one past the
     * {@link FinishLine}, about to leak, else one that taunts, else what {@code inner} picks: the player's priority
     * beats a leak, and a leak beats a taunt.
     */
    public static PreferringSelector priority(TargetSelector inner) {
        return new PreferringSelector(enemy -> enemy.hasEffect(EffectKind.PRIORITY),
                new PreferringSelector(enemy -> FinishLine.isPassedAt(enemy.pathShare()),
                        new PreferringSelector(EnemyMob::taunts, inner)));
    }

    /** Picks among the enemies that give their allies an aura first. */
    public static PreferringSelector leadersFirst(TargetSelector inner) {
        return new PreferringSelector(EnemyMob::leadsAllies, inner);
    }

    /** Aims at a frozen enemy first. */
    public static PreferringSelector stoppedFirst(TargetSelector inner) {
        return new PreferringSelector(EnemyMob::isStopped, inner);
    }

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        List<EnemyMob> favoured = candidates.stream().filter(this.preferred).toList();
        return this.inner.selectFrom(favoured.isEmpty() ? candidates : favoured);
    }
}
