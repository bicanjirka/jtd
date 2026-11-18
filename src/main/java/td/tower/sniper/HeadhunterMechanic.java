package td.tower.sniper;

import td.damage.DamageUnits;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerStat;

/** Headhunter: a shot at an Elite or a Boss deals more damage. */
public final class HeadhunterMechanic implements SniperMechanic {

    private static final float BONUS_POINTS = 14f;
    private static final Rank HUNTED_FROM = Rank.ELITE;

    @Override
    public SniperShot shape(SniperShot shot, EnemyMob target) {
        return this.hunts(target) ? shot.withBonusDamage(DamageUnits.ofPoints(BONUS_POINTS)) : shot;
    }

    @Override
    public boolean hunts(EnemyMob target) {
        return target.getRank().compareTo(HUNTED_FROM) >= 0;
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerStat.DAMAGE).plus(" " + TermNumbers.signed(Math.round(BONUS_POINTS))
                + " against Elites and Bosses");
    }
}
