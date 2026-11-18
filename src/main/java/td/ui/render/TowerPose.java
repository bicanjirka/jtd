package td.ui.render;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * What a tower's moving part shows right now, beyond where it aims. {@link #idle()} is a tower at rest and ready;
 * every other pose is a moment that game state causes, built from it with the {@code with} copies.
 *
 * @param cues   what the tower holds, each on its own part
 * @param stacks a count the look shows (a lens brighter with every stack)
 * @param beat   which step of a running motion the look shows (a head shaken about), 0 when none runs
 * @param load   the shells this reload loads, the one fired first first, for a look that loads them; empty for none
 * @param stock  every kind of shell the tower can load, in the kinds' order, for a look that shows its stock
 * @param panes  the looks whose colours the panes take, in order round the lantern; empty for plain panes
 * @param recoil how far back the shot has thrown the part: 1 on the tick it fired, easing to 0
 * @param flash  whether the shot's flash still shows: the first moments after it fired
 * @param reload how far the reload has come: 0 just fired, 1 ready
 */
public record TowerPose(Set<TowerCue> cues, int stacks, int beat, List<LoadedShell> load, List<LoadedShell> stock,
                        List<TowerLook> panes, float recoil, boolean flash, float reload) {

    /** A shot's flash is gone once this much of the reload has run. */
    private static final float FLASH_SHARE = 0.12f;
    private static final TowerPose IDLE = new TowerPose(Set.of(), 0, 0, List.of(), List.of(), List.of(), 0f, false,
            1f);

    public TowerPose {
        cues = cues.isEmpty() ? Set.of() : Set.copyOf(cues);
        load = List.copyOf(load);
        stock = List.copyOf(stock);
        panes = List.copyOf(panes);
    }

    public static TowerPose idle() {
        return IDLE;
    }

    public boolean has(TowerCue cue) {
        return this.cues.contains(cue);
    }

    public TowerPose with(TowerCue cue) {
        if (this.cues.contains(cue)) {
            return this;
        }
        EnumSet<TowerCue> more = EnumSet.of(cue);
        more.addAll(this.cues);
        return new TowerPose(more, this.stacks, this.beat, this.load, this.stock, this.panes, this.recoil, this.flash, this.reload);
    }

    /** As {@link #with(TowerCue)} when {@code held}, unchanged otherwise, so a builder chains without branching. */
    public TowerPose with(TowerCue cue, boolean held) {
        return held ? this.with(cue) : this;
    }

    public TowerPose withStacks(int count) {
        return new TowerPose(this.cues, count, this.beat, this.load, this.stock, this.panes, this.recoil, this.flash,
                this.reload);
    }

    public TowerPose withBeat(int step) {
        return new TowerPose(this.cues, this.stacks, step, this.load, this.stock, this.panes, this.recoil, this.flash,
                this.reload);
    }

    public TowerPose withLoad(List<LoadedShell> loading) {
        return new TowerPose(this.cues, this.stacks, this.beat, loading, this.stock, this.panes, this.recoil,
                this.flash, this.reload);
    }

    public TowerPose withStock(List<LoadedShell> kinds) {
        return new TowerPose(this.cues, this.stacks, this.beat, this.load, kinds, this.panes, this.recoil, this.flash,
                this.reload);
    }

    public TowerPose withPanes(List<TowerLook> lit) {
        return new TowerPose(this.cues, this.stacks, this.beat, this.load, this.stock, lit, this.recoil, this.flash,
                this.reload);
    }

    /** The tick a shot leaves: thrown all the way back, flashing, nothing reloaded. */
    public TowerPose fired() {
        return new TowerPose(this.cues, this.stacks, this.beat, this.load, this.stock, this.panes, 1f, true, 0f);
    }

    /**
     * {@code reloaded} of the way through a reload after a shot (0 just fired, 1 ready): the part eases forward as the
     * reload runs, and the flash shows over its first moments.
     */
    public TowerPose reloading(float reloaded) {
        float done = Math.clamp(reloaded, 0f, 1f);
        float back = (1f - done) * (1f - done);
        return new TowerPose(this.cues, this.stacks, this.beat, this.load, this.stock, this.panes, back, done < FLASH_SHARE,
                done);
    }

    /** {@code reloaded} of the way through a reload with no throw and no flash: a part that waits to be refilled. */
    public TowerPose refilling(float reloaded) {
        return new TowerPose(this.cues, this.stacks, this.beat, this.load, this.stock, this.panes, 0f, false,
                Math.clamp(reloaded, 0f, 1f));
    }

    /**
     * This pose with its recoil and reload moved to the nearest of {@code steps} even steps, so a painter that keeps a
     * picture per pose keeps few.
     */
    public TowerPose snapped(int steps) {
        float back = Math.round(this.recoil * steps) / (float) steps;
        float done = Math.round(this.reload * steps) / (float) steps;
        if (back == this.recoil && done == this.reload) {
            return this;
        }
        return new TowerPose(this.cues, this.stacks, this.beat, this.load, this.stock, this.panes, back, this.flash, done);
    }
}
