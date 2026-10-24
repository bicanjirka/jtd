package td.tower.splash;

import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectCategory;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.targeting.InRangeTargetQuery;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The hexes one caster has out, and what they pay: a Doom that ran out lands its share, a Reckoning carrier that died
 * releases the Dooms near it, a Contagion carrier that died passes its curses on, and once a second a Sympathy
 * carrier shares its debuffs. The caster records each hex it puts on and runs this at the start of its tick.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class HexKeeper {

    private static final int TICKS_PER_SECOND = Math.round(TickRate.TICKS_PER_SECOND);

    private final HexCaster caster;
    private final HexLedger ledger = new HexLedger();

    public HexKeeper(HexCaster caster) {
        this.caster = caster;
    }

    /** A freshly cast hex of {@code kind} just went on {@code enemy}. */
    public void record(EnemyMob enemy, EffectKind kind) {
        this.ledger.record(enemy, kind);
    }

    /** Whether no hex is out, so a tick has nothing to pay. */
    public boolean isEmpty() {
        return this.ledger.isEmpty();
    }

    /** Pays out what happened to the hexes since the last tick, as {@code spec} has them bite. */
    public void doTick(int gameTime, HexSpec spec) {
        if (this.ledger.isEmpty()) {
            return;
        }
        this.settle(spec);
        if (gameTime % TICKS_PER_SECOND == 0) {
            this.sympathize();
        }
    }

    private void settle(HexSpec spec) {
        for (HexEvent event : this.ledger.settle()) {
            switch (event) {
                case HexEvent.Ended ended -> {
                    if (ended.kind() == EffectKind.DOOM) {
                        this.payDoom(ended.enemy(), ended.stored(), spec);
                    }
                }
                case HexEvent.Died died -> {
                    if (spec.cursedCloud()) {
                        this.caster.leaveCloud(died.enemy());
                    }
                    if (died.carried(EffectKind.RECKONING).isPresent()) {
                        this.reckon(died.enemy(), spec);
                    }
                    died.carried(EffectKind.CONTAGION)
                            .filter(contagion -> contagion.generation() < ContagionHex.MAX_JUMPS)
                            .ifPresent(contagion -> this.spread(died, contagion.generation() + 1));
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
     * Reckoning: every Doom of this caster's within reach of the enemy that died pays out now, and
     * starts again at its full length.
     */
    private void reckon(EnemyMob dead, HexSpec spec) {
        Optional<Hex> doom = spec.hexOf(EffectKind.DOOM);
        float distance = this.caster.distance(HexSpec.SHARE_CELLS);
        for (EnemyMob enemy : this.ledger.carriers(EffectKind.DOOM)) {
            if (doom.isEmpty() || enemy == dead || !near(enemy, dead, distance)) {
                continue;
            }
            this.payDoom(enemy, this.ledger.restart(enemy, EffectKind.DOOM), spec);
            if (!enemy.isDead()) {
                int ticks = doom.get().ticksOn(enemy);
                this.caster.applyEffect(enemy, sink -> Effect.hex(EffectKind.DOOM, ticks, sink));
            }
        }
    }

    /**
     * Sympathy: each carrier tops up the other enemies that carry any of this caster's hexes, within
     * reach of it, to the debuffs it has itself.
     */
    private void sympathize() {
        List<EnemyMob> carriers = this.ledger.carriers(EffectKind.SYMPATHY);
        if (carriers.isEmpty()) {
            return;
        }
        float distance = this.caster.distance(HexSpec.SHARE_CELLS);
        List<EnemyMob> hexed = this.ledger.hexed();
        for (EnemyMob carrier : carriers) {
            for (EnemyMob other : hexed) {
                if (other != carrier && near(other, carrier, distance)) {
                    SympathyCopier.missingFrom(carrier, other).forEach(other::applyEffect);
                }
            }
        }
    }

    /**
     * Contagion: the dead enemy's hexes from this caster, and every debuff on it a curse carries,
     * jump to the nearest enemies that carry no hex, with the time they had left.
     */
    private void spread(HexEvent.Died died, int generation) {
        EnemyMob dead = died.enemy();
        float distance = this.caster.distance(ContagionHex.SPREAD_CELLS);
        List<Effect> carried = dead.activeEffects();
        List<EnemyMob> next = InRangeTargetQuery.everyone((int) dead.getX(), (int) dead.getY(), distance)
                .matching(this.caster.enemies()).stream()
                .filter(enemy -> enemy != dead && enemy.activeEffectKinds().stream()
                        .noneMatch(kind -> kind.category() == EffectCategory.HEX))
                .sorted(Comparator.comparingDouble(enemy -> Math.hypot(enemy.getX() - dead.getX(),
                        enemy.getY() - dead.getY())))
                .limit(ContagionHex.TARGETS)
                .toList();
        for (EnemyMob enemy : next) {
            for (Effect effect : carried) {
                if (effect.kind().spreadsWithCurse()) {
                    enemy.applyEffect(effect);
                } else if (died.carried(effect.kind()).isPresent()) {
                    int ticks = effect.remainingTicks();
                    this.caster.applyEffect(enemy, sink -> Effect.hex(effect.kind(), ticks, sink));
                    this.ledger.record(enemy, effect.kind(), generation);
                }
            }
        }
    }

    private static boolean near(EnemyMob a, EnemyMob b, float distance) {
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY()) <= distance;
    }
}
