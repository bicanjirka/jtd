package td.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntConsumer;

/**
 * Playtest work the engine runs every tick, after the zones and before the towers: what the dev panel keeps going on
 * the board by itself. Nothing in normal play adds one.
 */
public final class PlaytestHooks {

    private final List<IntConsumer> hooks = new CopyOnWriteArrayList<>();

    /** {@code hook} runs every tick from now on, given the tick's time. */
    public void add(IntConsumer hook) {
        this.hooks.add(hook);
    }

    public void doTick(int time) {
        for (IntConsumer hook : this.hooks) {
            hook.accept(time);
        }
    }
}
