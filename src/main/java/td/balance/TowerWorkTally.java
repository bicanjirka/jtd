package td.balance;

import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerHitListener;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Adds up what each tower type did to the enemies of one play: every hit as it lands, and who stood in reach. */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class TowerWorkTally implements TowerHitListener {

    private final Map<TowerFactory.Type, Work> work = new EnumMap<>(TowerFactory.Type.class);

    /** One type's running totals. */
    @ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
    private static final class Work {
        private long credited;
        private long absorbed;
        private long overkill;
        private long ticksInReach;
        private final Map<String, Long> damageByKind = new HashMap<>();
    }

    private Work workOf(TowerFactory.Type type) {
        return this.work.computeIfAbsent(type, ignored -> new Work());
    }

    @Override
    public void landed(Tower tower, EnemyMob enemy, HitResult result) {
        Work towerWork = this.workOf(tower.getType());
        towerWork.credited += result.credited();
        towerWork.absorbed += result.absorbed();
        towerWork.overkill += result.overkill();
        if (result.credited() > 0) {
            towerWork.damageByKind.merge(enemy.accept(defined -> defined.definition().id()), (long) result.credited(),
                    Long::sum);
        }
    }

    /** Call once a tick: counts every living enemy within the current range of every tower. */
    void countReach(EnemyMob[] enemies, List<Tower> towers) {
        List<EnemyMob> walking = new ArrayList<>();
        for (EnemyMob mob : enemies) {
            if (!mob.isDead() && !mob.accept(DefinedEnemyMob::isInactive)) {
                walking.add(mob);
            }
        }
        for (Tower tower : towers) {
            double range = tower.getRangeReal();
            double range2 = range * range;
            int inReach = 0;
            for (EnemyMob mob : walking) {
                double dx = mob.getX() - tower.getX();
                double dy = mob.getY() - tower.getY();
                if (dx * dx + dy * dy <= range2) {
                    inReach++;
                }
            }
            if (inReach > 0) {
                this.workOf(tower.getType()).ticksInReach += inReach;
            }
        }
    }

    Map<TowerFactory.Type, PlayRecord.TowerWork> totals() {
        Map<TowerFactory.Type, PlayRecord.TowerWork> totals = new EnumMap<>(TowerFactory.Type.class);
        this.work.forEach((type, done) -> totals.put(type, new PlayRecord.TowerWork(
                new PlayRecord.HitTotals(done.credited, done.absorbed, done.overkill), done.ticksInReach,
                done.damageByKind)));
        return totals;
    }
}
