package td.tower.splash;

import td.damage.Damage;
import td.effect.EffectCategory;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.tower.targeting.TargetSelector;
import td.util.ThreadConfined;

import java.util.List;
import java.util.Optional;

/**
 * The hexes one caster has out, and what they pay: a Doom that ran out lands its share and one whose carrier was
 * killed jumps on with what it recorded, and with Contagion any other hex whose carrier was killed jumps on too, as it
 * was. The caster records each hex it puts on and runs this at the start of
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

    /** A hex of {@code kind} jumped onto {@code enemy} carrying {@code stored}; it merges with one already there. */
    public void recordJump(EnemyMob enemy, EffectKind kind, int castTicks, long stored) {
        this.ledger.recordJump(enemy, kind, castTicks, stored);
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
                    if (gone.enemy().wasKilled()) {
                        gone.hexes().stream()
                                .filter(hex -> hex.kind() == EffectKind.DOOM || spec.contagion())
                                .forEach(hex -> this.spread(gone.enemy(), hex, spec));
                    }
                }
            }
        }
    }

    /** Doom's payout: its share of the {@code stored} damage, as one shadow hit that never crits. */
    private void payDoom(EnemyMob enemy, long stored, HexSpec spec) {
        int payout = Math.round(stored * spec.doomShare());
        if (payout > 0) {
            this.caster.dealShadowHit(enemy, Damage.magic(payout));
        }
    }

    /**
     * The hex on the dead enemy jumps on with the time it had left as it was cast, the slow it had grown to and what
     * it recorded. A Doom takes the nearest enemy without a Doom, else the nearest with one, and with Contagion the
     * healthiest, starting over for its whole time; any other hex takes the nearest enemy without a hex.
     */
    private void spread(EnemyMob dead, HexEvent.CarriedHex carried, HexSpec spec) {
        dead.activeEffect(carried.kind()).ifPresent(effect -> {
            boolean isDoom = carried.kind() == EffectKind.DOOM;
            int ticks = isDoom && spec.contagion() ? carried.castTicks()
                    : Math.max(1, Math.round((float) effect.remainingTicks() * carried.castTicks()
                            / Math.max(1, effect.authoredDurationTicks())));
            List<EnemyMob> around = InRangeTargetQuery.around(dead, this.caster.distance(HexSpec.SPREAD_CELLS))
                    .matching(this.caster.enemies()).stream()
                    .filter(enemy -> !enemy.isDead())
                    .toList();
            TargetSelector aim = isDoom && spec.contagion() ? new HighestHealthSelector()
                    : new NearestSelector(dead.getX(), dead.getY());
            Optional<EnemyMob> next = aim.selectFrom(around.stream()
                    .filter(enemy -> !this.hasHex(enemy, carried.kind(), isDoom))
                    .toList());
            if (next.isEmpty() && isDoom) {
                next = aim.selectFrom(around);
            }
            next.ifPresent(to -> this.caster.jump(new HexJump(
                    new HexEvent.CarriedHex(carried.kind(), ticks, carried.stored()), dead.position(), to,
                    effect.grownSlow())));
        });
    }

    /** Whether {@code enemy} blocks a jump of {@code kind}: a Doom is blocked by a Doom, any other hex by any hex. */
    private boolean hasHex(EnemyMob enemy, EffectKind kind, boolean isDoom) {
        return isDoom ? enemy.hasEffect(kind)
                : enemy.activeEffectKinds().stream().anyMatch(other -> other.category() == EffectCategory.HEX);
    }
}
