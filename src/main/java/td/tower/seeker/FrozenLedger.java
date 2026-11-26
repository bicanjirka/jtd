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

    /** {@code enemy} was just frozen by a missile of {@code kind} fired with {@code stats}. */
    public void record(EnemyMob enemy, MissileKind kind, TowerStats stats) {
        this.holding.removeIf(freeze -> freeze.enemy() == enemy);
        this.holding.add(new Freeze(enemy, kind, stats));
    }

    /**
     * {@code enemy}'s freeze, held already, is now of {@code kind}'s ice, fired with {@code stats}: it ends as it would
     * have, but what it does then is that kind's.
     */
    public void retake(EnemyMob enemy, MissileKind kind, TowerStats stats) {
        this.record(enemy, kind, stats);
    }

    /** The enemies whose freeze it landed still holds them, alive. */
    public List<EnemyMob> holding() {
        return this.holding.stream().map(Freeze::enemy).filter(enemy -> !enemy.isDead()).toList();
    }

    /** The freezes that ended since the last call, by a thaw or by death. */
    public List<Freeze> settle() {
        List<Freeze> ended = new ArrayList<>();
        this.holding.removeIf(freeze -> {
            if (freeze.enemy().isDead() || !freeze.enemy().hasEffect(EffectKind.FREEZE)) {
                ended.add(freeze);
                return true;
            }
            return false;
        });
        return ended;
    }

    /** A freeze it landed: on whom, by which kind of missile, fired with which stats. */
    public record Freeze(EnemyMob enemy, MissileKind kind, TowerStats stats) {
    }
}
