package td.tower.splash;

import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectCategory;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.util.ThreadConfined;

import java.util.Optional;

/**
 * The hexes one caster has out, and what they pay: a Doom that ran out lands its share, and with Contagion a hex
 * whose carrier was killed jumps on, as it was. The caster records each hex it puts on and runs this at the start of
 * its tick.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class HexKeeper {

    private final HexCaster caster;
    private final HexLedger ledger = new HexLedger();

    public HexKeeper(HexCaster caster) {
        this.caster = caster;
    }

    /** A hex of {@code kind}, cast for {@code castTicks}, just went on {@code enemy}. */
    public void record(EnemyMob enemy, EffectKind kind, int castTicks) {
        this.ledger.record(enemy, kind, castTicks);
    }

    /** Pays out what happened to the hexes since the last tick, as {@code spec} has them bite. */
    public void doTick(HexSpec spec) {
        if (this.ledger.isEmpty()) {
            return;
        }
        for (HexEvent event : this.ledger.settle()) {
            switch (event) {
                case HexEvent.Ended ended -> {
                    if (ended.kind() == EffectKind.DOOM) {
                        this.payDoom(ended.enemy(), ended.stored(), spec);
                    }
                }
                case HexEvent.Gone gone -> {
                    if (spec.contagion() && gone.enemy().wasKilled()) {
                        gone.hexes().forEach(hex -> this.spread(gone.enemy(), hex));
                    }
                }
            }
        }
    }

    /** Doom's payout: its share of the {@code stored} damage, as one magic hit that never crits. */
    private void payDoom(EnemyMob enemy, long stored, HexSpec spec) {
        int payout = Math.round(stored * spec.doomShare());
        if (payout > 0) {
            this.caster.dealPeriodicDamage(enemy, Damage.magic(payout));
        }
    }

    /**
     * Contagion: the hex on the dead enemy jumps to the nearest enemy without a hex, with the time it had left as it
     * was cast and the slow it had grown to.
     */
    private void spread(EnemyMob dead, HexEvent.CarriedHex carried) {
        dead.activeEffect(carried.kind()).ifPresent(left -> this.spread(dead, carried, left));
    }

    private void spread(EnemyMob dead, HexEvent.CarriedHex carried, Effect effect) {
        int ticks = Math.max(1, Math.round((float) effect.remainingTicks() * carried.castTicks()
                / Math.max(1, effect.authoredDurationTicks())));
        Optional<EnemyMob> next = new NearestSelector(dead.getX(), dead.getY()).selectFrom(
                InRangeTargetQuery.around(dead, this.caster.distance(HexSpec.SPREAD_CELLS))
                        .matching(this.caster.enemies()).stream()
                        .filter(enemy -> !enemy.isDead()
                                && enemy.activeEffectKinds().stream()
                                .noneMatch(kind -> kind.category() == EffectCategory.HEX))
                        .toList());
        next.ifPresent(to -> this.caster.jump(new HexJump(new HexEvent.CarriedHex(carried.kind(), ticks),
                dead.position(), to, effect.grownSlow())));
    }
}
