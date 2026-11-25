package td.tower.sniper;

import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerStat;

/** Headhunter: a shot at an Elite or a Boss is likelier to crit. */
public final class HeadhunterMechanic implements SniperMechanic {

    private static final float BONUS_CRIT_CHANCE = 0.50f;
    private static final Rank HUNTED_FROM = Rank.ELITE;

    @Override
    public SniperShot shape(SniperShot shot, EnemyMob target) {
        return this.hunts(target)
                ? shot.withAttack(attack -> attack.withCritChance(Math.min(1f, attack.critChance() + BONUS_CRIT_CHANCE)))
                : shot;
    }

    @Override
    public boolean hunts(EnemyMob target) {
        return target.getRank().compareTo(HUNTED_FROM) >= 0;
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerStat.CRIT_CHANCE).plus(" " + TermNumbers.signedPercent(BONUS_CRIT_CHANCE)
                + " against Elites and Bosses");
    }
}
