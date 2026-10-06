package td.tower.targeting;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

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

    /** A tower that picks one target picks the enemy carrying a priority while it is in range. */
    public static PreferringSelector priority(TargetSelector inner) {
        return new PreferringSelector(enemy -> enemy.hasEffect(EffectKind.PRIORITY), inner);
    }

    /** Aims at a frozen enemy first. */
    public static PreferringSelector frozenFirst(TargetSelector inner) {
        return new PreferringSelector(enemy -> enemy.hasEffect(EffectKind.FREEZE), inner);
    }

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        List<EnemyMob> favoured = candidates.stream().filter(this.preferred).toList();
        return this.inner.selectFrom(favoured.isEmpty() ? candidates : favoured);
    }
}
