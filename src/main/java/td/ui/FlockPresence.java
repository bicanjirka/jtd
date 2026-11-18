package td.ui;

/**
 * Where an Aerie's flock is, on the tick clock: {@code arrived} is how far it has come in (0 none, 1 every bird
 * circling) and {@code leaving} how far it has scattered since the tower began to fight (0 while it is quiet).
 */
record FlockPresence(double arrived, double leaving) {

    /** A tower must be quiet this long before its first bird comes; the flock takes this long to come in. */
    static final double DELAY_SECONDS = 1.5;
    static final double ARRIVE_SECONDS = 3.5;
    /** Once the tower fights, its birds scatter over this long. */
    static final double LEAVE_SECONDS = 1.4;
    /** A bird's own pace of leaving varies this far either side of the flock's. */
    private static final double LEAVE_SPREAD = 0.2;

    /**
     * A quiet tower: {@code quietSeconds} since its last fight ended. A fighting one: {@code quietSeconds} is how long
     * the quiet before this fight lasted, {@code fightingSeconds} how long it has fought.
     */
    static FlockPresence of(double quietSeconds, boolean fighting, double fightingSeconds) {
        double arrived = Math.clamp((quietSeconds - DELAY_SECONDS) / ARRIVE_SECONDS, 0, 1);
        return new FlockPresence(arrived, fighting ? Math.max(1e-6, fightingSeconds / LEAVE_SECONDS) : 0);
    }

    /** Whether no bird shows: none came yet, or every one has gone. */
    boolean empty() {
        return this.arrived <= 0 || this.leaving >= 1 / (1 - LEAVE_SPREAD);
    }

    /** How far bird {@code i} of {@code count} is in: they come one after another. */
    double in(int i, int count) {
        double gap = 0.5 / count;
        return Math.clamp((this.arrived - i * gap) / (1 - (count - 1) * gap), 0, 1);
    }

    /** How far a bird has fled, at its own pace from {@code roll} in 0..1. */
    double out(double roll) {
        return this.leaving <= 0 ? 0 : Math.clamp(this.leaving * (1 - LEAVE_SPREAD + 2 * LEAVE_SPREAD * roll), 0, 1);
    }
}
