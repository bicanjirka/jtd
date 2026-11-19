package td.tower.splash;

import td.enemy.EnemyMob;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** The enemies one Burst's passive has tagged, each once, in the order it tagged them. */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class TagLedger {

    private final List<EnemyMob> tagged = new ArrayList<>();
    /** Membership only: the list keeps the order. */
    private final Set<EnemyMob> members = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Tags {@code enemy}, unless it already carries this tower's stack. */
    public void tag(EnemyMob enemy) {
        if (this.members.add(enemy)) {
            this.tagged.add(enemy);
        }
    }

    public boolean isEmpty() {
        return this.tagged.isEmpty();
    }

    /** Static: untags every enemy dead or badly hurt, and {@code discharge}s each of them still alive, in tag order. */
    public void settleStatic(Consumer<EnemyMob> discharge) {
        for (EnemyMob enemy : this.take(enemy -> enemy.isDead()
                || enemy.getHealthFraction() <= StaticMechanic.DISCHARGE_AT)) {
            if (!enemy.isDead()) {
                discharge.accept(enemy);
            }
        }
    }

    /** Soulbrand: untags every dead enemy, and {@code burst}s each of them that was killed, in tag order. */
    public void settleSoulbrand(Consumer<EnemyMob> burst) {
        for (EnemyMob enemy : this.take(EnemyMob::isDead)) {
            if (enemy.wasKilled()) {
                burst.accept(enemy);
            }
        }
    }

    /** Untags every enemy. */
    public void clear() {
        this.tagged.clear();
        this.members.clear();
    }

    /** Untags every enemy that matches {@code which} and hands them back, in the order they were tagged. */
    private List<EnemyMob> take(Predicate<EnemyMob> which) {
        List<EnemyMob> taken = new ArrayList<>();
        this.tagged.removeIf(enemy -> {
            if (which.test(enemy)) {
                taken.add(enemy);
                this.members.remove(enemy);
                return true;
            }
            return false;
        });
        return taken;
    }
}
