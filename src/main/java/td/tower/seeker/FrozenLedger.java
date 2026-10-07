package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;

/** The enemies a Seeker froze that are still frozen, watched for the moment each one thaws. */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class FrozenLedger {

    private final List<EnemyMob> frozen = new ArrayList<>();

    /** {@code enemy} was just frozen by this Seeker. */
    public void record(EnemyMob enemy) {
        if (!this.frozen.contains(enemy)) {
            this.frozen.add(enemy);
        }
    }

    /** The enemies whose freeze has ended since the last call, still alive; the dead are dropped quietly. */
    public List<EnemyMob> settle() {
        List<EnemyMob> thawed = new ArrayList<>();
        this.frozen.removeIf(enemy -> {
            if (enemy.isDead()) {
                return true;
            }
            if (!enemy.hasEffect(EffectKind.FREEZE)) {
                thawed.add(enemy);
                return true;
            }
            return false;
        });
        return thawed;
    }
}
