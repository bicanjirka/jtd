package td.tower.sniper;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerBuffState;
import td.tower.TowerTerm;
import td.util.ThreadConfined;

import java.util.Optional;

/** Tradecraft: a crit makes the next crit hit harder, two in a row harder still, and no further. */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class CritStreakMechanic implements SniperMechanic {

    private static final float CRIT_DAMAGE_PER_STREAK = 0.25f;
    private static final int MAX_STREAK = 2;

    private int streak;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return shot.withAttack(attack -> attack.withCritDamageBonus(CRIT_DAMAGE_PER_STREAK * this.streak));
    }

    @Override
    public void react(ShotResult result, ShotActions actions) {
        this.streak = result.critical() ? Math.min(MAX_STREAK, this.streak + 1) : 0;
    }

    @Override
    public Optional<TowerBuffState> condition() {
        if (this.streak == 0) {
            return Optional.empty();
        }
        return Optional.of(TowerBuffState.of(TowerTerm.CRIT_STREAK, TermText.of("the next crit deals "
                + TermNumbers.signedPercent(CRIT_DAMAGE_PER_STREAK * this.streak) + " crit damage"))
                .withStacks(this.streak));
    }

    @Override
    public TermText describe() {
        return TermText.of("each crit adds a stack of ")
                .plus(TermText.of(TowerTerm.CRIT_STREAK, TermText.of("each stack gives the next crit "
                        + TermNumbers.signedPercent(CRIT_DAMAGE_PER_STREAK) + " crit damage")))
                .plus(", up to " + MAX_STREAK + "; a shot that doesn't crit clears it");
    }
}
