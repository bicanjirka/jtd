package td.tower.splash;

import td.damage.Damage;
import td.effect.EffectCategory;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.util.ThreadConfined;

import java.util.ArrayList;
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
     * it recorded. A Doom takes the nearest enemy without a Doom, else the nearest with one; any other hex takes the
     * nearest enemy without a hex. With Contagion the hex splits in two, each half to its own enemy, and a Doom's
     * record is shared between them.
     */
    private void spread(EnemyMob dead, HexEvent.CarriedHex carried, HexSpec spec) {
        dead.activeEffect(carried.kind()).ifPresent(effect -> {
            boolean isDoom = carried.kind() == EffectKind.DOOM;
            int ticks = Math.max(1, Math.round((float) effect.remainingTicks() * carried.castTicks()
                    / Math.max(1, effect.authoredDurationTicks())));
            List<EnemyMob> around = new ArrayList<>(InRangeTargetQuery.around(dead, this.caster.distance(HexSpec.SPREAD_CELLS))
                    .matching(this.caster.enemies()).stream()
                    .filter(enemy -> !enemy.isDead())
                    .toList());
            NearestSelector aim = new NearestSelector(dead.getX(), dead.getY());
            List<EnemyMob> landing = new ArrayList<>();
            for (int i = 0; i < (spec.contagion() ? 2 : 1); i++) {
                Optional<EnemyMob> next = aim.selectFrom(around.stream()
                        .filter(enemy -> !this.hasHex(enemy, carried.kind(), isDoom))
                        .toList());
                if (next.isEmpty() && isDoom) {
                    next = aim.selectFrom(around);
                }
                next.ifPresent(landing::add);
                next.ifPresent(around::remove);
            }
            long each = carried.stored() / Math.max(1, landing.size());
            for (int i = 0; i < landing.size(); i++) {
                long stored = i == 0 ? carried.stored() - each * (landing.size() - 1) : each;
                this.caster.jump(new HexJump(new HexEvent.CarriedHex(carried.kind(), ticks, stored), dead.position(),
                        landing.get(i), effect.grownSlow()));
            }
        });
    }

    /** Whether {@code enemy} blocks a jump of {@code kind}: a Doom is blocked by a Doom, any other hex by any hex. */
    private boolean hasHex(EnemyMob enemy, EffectKind kind, boolean isDoom) {
        return isDoom ? enemy.hasEffect(kind)
                : enemy.activeEffectKinds().stream().anyMatch(other -> other.category() == EffectCategory.HEX);
    }
}
