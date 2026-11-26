package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.TowerStats;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;

/** The freezes the Aerie landed that still hold, each watched for the tick it ends. */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class FrozenLedger {

    private final List<Freeze> holding = new ArrayList<>();

    /** {@code enemy} was just frozen by a missile of {@code kind} fired with {@code stats}, at {@code tick}. */
    public void record(EnemyMob enemy, MissileKind kind, TowerStats stats, int tick) {
        this.holding.removeIf(freeze -> freeze.enemy() == enemy);
        this.holding.add(new Freeze(enemy, kind, stats, tick));
    }

    /**
     * {@code enemy}'s freeze, held already, is now of {@code kind}'s ice, fired with {@code stats}: it ends as it would
     * have, but what it does then is that kind's. From {@code tick} when the ledger held no freeze on it.
     */
    public void retake(EnemyMob enemy, MissileKind kind, TowerStats stats, int tick) {
        int since = this.holding.stream().filter(freeze -> freeze.enemy() == enemy).findFirst()
                .map(Freeze::frozenAtTick).orElse(tick);
        this.holding.removeIf(freeze -> freeze.enemy() == enemy);
        this.holding.add(new Freeze(enemy, kind, stats, since));
    }

    /** The enemies whose freeze it landed still holds them, alive. */
    public List<EnemyMob> holding() {
        return this.holding.stream().map(Freeze::enemy).filter(enemy -> !enemy.isDead()).toList();
    }

    /** The freezes that ended since the last call, by a thaw or by death, with how long each held up to {@code tick}. */
    public List<Thaw> settle(int tick) {
        List<Thaw> ended = new ArrayList<>();
        this.holding.removeIf(freeze -> {
            if (freeze.enemy().isDead() || !freeze.enemy().hasEffect(EffectKind.FREEZE)) {
                ended.add(new Thaw(freeze, tick - freeze.frozenAtTick()));
                return true;
            }
            return false;
        });
        return ended;
    }

    /** A freeze it landed: on whom, by which kind of missile, fired with which stats, from which tick. */
    public record Freeze(EnemyMob enemy, MissileKind kind, TowerStats stats, int frozenAtTick) {
    }

    /** A freeze that ended, and how many ticks it held. */
    public record Thaw(Freeze freeze, int heldTicks) {
    }
}
