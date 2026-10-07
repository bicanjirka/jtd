package td.tower.sniper;

import td.enemy.Rank;
import td.tower.targeting.HighestRankSelector;

/** Headhunter: shots hit an elite or a boss harder. Aims at the highest rank. */
public final class HeadhunterPerk implements SniperPerk {

    private static final float DAMAGE_FACTOR = 1.4f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        boolean worthIt = context.target().getRank().compareTo(Rank.ELITE) >= 0;
        return worthIt ? shot.scaledBy(DAMAGE_FACTOR) : shot;
    }

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        return spec.aimingAt(new SniperAim(new HighestRankSelector(), "highest rank"));
    }
}
