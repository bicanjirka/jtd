package td.tower.seeker;

import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The missiles the Aerie holds ready, each of its kind, in the order they leave; how long until the next may
 * launch; and the missiles a Hatching still has to hatch.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SeekerNest {

    private final List<MissileKind> missiles = new ArrayList<>();
    private int gapLeft;
    private int hatchLeft;
    private int hatchWait;

    /** The missiles it holds, the next to leave first. */
    public List<MissileKind> missiles() {
        return List.copyOf(this.missiles);
    }

    public int stored() {
        return this.missiles.size();
    }

    public boolean hasRoom(int capacity) {
        return this.missiles.size() < capacity;
    }

    /** Takes in one missile of {@code kind}, at the back. */
    public void load(MissileKind kind) {
        this.missiles.add(kind);
    }

    /** Lets go every missile of a kind {@code owned} no longer holds. */
    public void keepOnly(Predicate<MissileKind> owned) {
        this.missiles.removeIf(owned.negate());
    }

    /** One tick closer to the next launch. */
    public void tick() {
        if (this.gapLeft > 0) {
            this.gapLeft--;
        }
    }

    public boolean readyToLaunch() {
        return !this.missiles.isEmpty() && this.gapLeft == 0;
    }

    /** Takes the front missile out to launch, and starts a wait of {@code gapTicks} before the next. */
    public MissileKind launch(int gapTicks) {
        this.gapLeft = gapTicks;
        return this.missiles.removeFirst();
    }

    /** {@code count} more missiles to hatch, the first at once. */
    public void hatch(int count) {
        this.hatchLeft += count;
    }

    /** Whether a missile hatches this tick, one every {@code gapTicks} while any are left to hatch. */
    public boolean hatchesNow(int gapTicks) {
        if (this.hatchLeft == 0) {
            return false;
        }
        if (this.hatchWait > 0) {
            this.hatchWait--;
            return false;
        }
        this.hatchLeft--;
        this.hatchWait = gapTicks - 1;
        return true;
    }
}
