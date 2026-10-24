package td.enemy;

import td.damage.AttackOrigin;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The enemies the dev panel observes, each writing a trace line for everything that happens to it. It knows the
 * tick being simulated, since a hit carries no game time, and names what caused an event: the tower behind an
 * attack, an enemy whose ability is running, or the dev panel's action.
 * <p>
 * Only the dev panel starts an observation; an observed enemy passes it on to what it spawns or hatches into.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class EnemyObservation {

    private static final String NO_CAUSE = "-";

    private final ObserveLog log;
    private final Function<AttackOrigin, Optional<String>> attackers;
    // Registered from the EDT while the panel is built, read on the game loop.
    private final Map<AttackOrigin, String> named = new ConcurrentHashMap<>();
    private final List<DefinedEnemyMob> observed = new ArrayList<>();

    private int tick;
    private String cause = NO_CAUSE;

    /** {@code attackers} names the tower behind an origin, as {@code Sniper@7,4}. */
    public EnemyObservation(ObserveLog log, Function<AttackOrigin, Optional<String>> attackers) {
        this.log = log;
        this.attackers = attackers;
    }

    /** Every line written from now on is stamped with {@code tick}. */
    public void beginTick(int tick) {
        this.tick = tick;
    }

    public int tick() {
        return this.tick;
    }

    /** Whether anything is observed; nothing else here costs an unobserved game anything. */
    public boolean anyObserved() {
        return !this.observed.isEmpty();
    }

    /**
     * Starts observing {@code mob}, unless it already is: a START line, then one line per event until it ends.
     *
     * @param via how observation began, as its START line says
     */
    public void observe(DefinedEnemyMob mob, String via) {
        this.observe(mob, new LoggedMobEvents(this.log, this::tick, mob.label()), via);
    }

    /** {@link #observe(DefinedEnemyMob, String)}, its events going to {@code sink} instead of the log. */
    void observe(DefinedEnemyMob mob, MobEventSink sink, String via) {
        if (mob.isObserved()) {
            return;
        }
        mob.startObservation(MobObserver.reportingTo(sink), via);
        this.observed.add(mob);
    }

    /** Ends {@code mob}'s trace with {@code how}; nothing if it isn't observed. */
    public void end(DefinedEnemyMob mob, String how) {
        mob.endObservation(how);
    }

    /** Ends every trace with {@code how}, such as the level ending under them. */
    public void endAll(String how) {
        for (DefinedEnemyMob mob : List.copyOf(this.observed)) {
            mob.endObservation(how);
        }
        this.observed.clear();
    }

    /** A full snapshot of every observed enemy, or one line saying there is none. */
    public void snapshotAll() {
        this.observed.removeIf(mob -> !mob.isObserved());
        if (this.observed.isEmpty()) {
            this.log.write(MobEventText.note(this.tick, "SNAPSHOT nothing observed"));
            return;
        }
        this.observed.forEach(DefinedEnemyMob::reportSnapshot);
    }

    /**
     * Ends the trace of every observed enemy that left {@code roster} without an end of its own, such as a roster
     * replaced at a wave's start, and forgets the ones that ended.
     */
    public void sweep(EnemyRegistry roster) {
        if (this.observed.isEmpty()) {
            return;
        }
        Set<EnemyMob> present = Collections.newSetFromMap(new IdentityHashMap<>());
        Collections.addAll(present, roster.getEnemies());
        for (DefinedEnemyMob mob : this.observed) {
            if (mob.isObserved() && !present.contains(mob)) {
                mob.endObservation("removed");
            }
        }
        this.observed.removeIf(mob -> !mob.isObserved());
    }

    /** Gives an origin that is no tower a name of its own, such as the dev panel's effects. */
    public void name(AttackOrigin origin, String name) {
        this.named.put(origin, name);
    }

    /**
     * Who {@code origin} is: a named origin, the tower behind it, or else whatever is running {@link #during} now.
     */
    public String nameOf(AttackOrigin origin) {
        String name = this.named.get(origin);
        if (name != null) {
            return name;
        }
        return this.attackers.apply(origin).orElse(this.cause);
    }

    /** What is running {@link #during} now, or {@code fallback} when nothing is. */
    String causeOr(String fallback) {
        return this.cause.equals(NO_CAUSE) ? fallback : this.cause;
    }

    /**
     * Runs {@code action} with {@code cause} as the name of whatever has no attacker of its own meanwhile: an
     * enemy's ability, or a dev panel action.
     */
    public void during(String cause, Runnable action) {
        String outer = this.cause;
        this.cause = cause;
        try {
            action.run();
        } finally {
            this.cause = outer;
        }
    }
}
