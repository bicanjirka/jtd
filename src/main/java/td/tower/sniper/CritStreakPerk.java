package td.tower.sniper;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerBuffState;
import td.tower.TowerTerm;
import td.tower.upgrade.PerkText;
import td.util.ThreadConfined;

import java.util.Optional;

/** Tradecraft: a crit makes the next crit hit harder, two in a row harder still, and no further. */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class CritStreakPerk implements SniperPerk {

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
        return TermText.of("a crit makes the next crit deal +" + PerkText.percent(CRIT_DAMAGE_PER_STREAK) + " crit damage, two in a row +"
                + PerkText.percent(CRIT_DAMAGE_PER_STREAK * MAX_STREAK) + ", no further");
    }
}
