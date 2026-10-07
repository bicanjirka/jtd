package td.tower.seeker;

import td.enemy.EnemyMob;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The missiles a Seeker has banked, how long until the next may launch, and whom the salvo in
 * progress has fired at. A salvo ends once no missile has left for twice the launch gap.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SeekerNest {

    private static final int IDLE_CAP_TICKS = 1_000_000;

    private final List<EnemyMob> salvoTargets = new ArrayList<>();
    private int stored;
    private int gapLeft;
    private int ticksSinceLaunch = IDLE_CAP_TICKS;

    public int stored() {
        return this.stored;
    }

    public boolean hasRoom(NestSpec spec) {
        return this.stored < spec.capacity();
    }

    /** Banks one missile. */
    public void load() {
        this.stored++;
    }

    /** One tick closer to the next launch; a salvo that has gone quiet is over. */
    public void tick(NestSpec spec) {
        if (this.gapLeft > 0) {
            this.gapLeft--;
        }
        if (this.ticksSinceLaunch < IDLE_CAP_TICKS) {
            this.ticksSinceLaunch++;
        }
        if (this.ticksSinceLaunch > 2 * spec.launchGapTicks()) {
            this.salvoTargets.clear();
        }
    }

    public boolean readyToLaunch() {
        return this.stored > 0 && this.gapLeft == 0;
    }

    /** Takes one missile out to launch, and starts the wait before the next. */
    public void launch(NestSpec spec) {
        this.stored--;
        this.gapLeft = spec.launchGapTicks();
        this.ticksSinceLaunch = 0;
    }

    /** Remembers that the salvo has fired at {@code target}. */
    public void recordTarget(EnemyMob target) {
        this.salvoTargets.add(target);
    }

    /** Whom the salvo in progress has fired at. */
    public Set<EnemyMob> salvoTargets() {
        return Set.copyOf(this.salvoTargets);
    }
}
