package td.tower.seeker;

import td.util.ThreadConfined;

/** The missiles a Seeker has banked, and how long until the next may launch. */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SeekerNest {

    private int stored;
    private int gapLeft;

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

    /** One tick closer to the next launch. */
    public void tick() {
        if (this.gapLeft > 0) {
            this.gapLeft--;
        }
    }

    public boolean readyToLaunch() {
        return this.stored > 0 && this.gapLeft == 0;
    }

    /** Takes one missile out to launch, and starts the wait before the next. */
    public void launch(NestSpec spec) {
        this.stored--;
        this.gapLeft = spec.launchGapTicks();
    }
}
