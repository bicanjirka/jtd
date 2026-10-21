package td.tower;

import td.stat.StatModifiers;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** The statuses one tower holds now, each with what is left of it: an end tick or a count of shots. */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
final class ActiveTowerStatuses {

    private final List<Held> held = new ArrayList<>();

    /**
     * Grants {@code status} at {@code nowTick}: a running copy is left alone if the status keeps it,
     * otherwise replaced. A timed status lasts its duration times {@code buffLength}; a shot status
     * its shots, times {@code buffLength} when it is lengthened.
     */
    void grant(TowerStatus status, int nowTick, float buffLength) {
        int running = this.indexOf(status.term());
        if (running >= 0 && status.keepsRunning()) {
            return;
        }
        Held next = status.shots() > 0
                ? new Held(status, 0, status.lengthened() ? Math.round(status.shots() * buffLength) : status.shots(), 0)
                : new Held(status, nowTick + Math.round(status.durationTicks() * buffLength), 0,
                        Math.round(status.durationTicks() * buffLength));
        if (running >= 0) {
            this.held.set(running, next);
        } else {
            this.held.add(next);
        }
    }

    /** Drops the timed statuses past their end; whether anything changed. */
    boolean expire(int nowTick) {
        return this.held.removeIf(h -> h.status().shots() == 0 && nowTick >= h.endsAt());
    }

    /** Spends one shot of every shot status, dropping those with none left; whether anything changed. */
    boolean spendShot() {
        boolean changed = false;
        for (int i = this.held.size() - 1; i >= 0; i--) {
            Held h = this.held.get(i);
            if (h.status().shots() == 0) {
                continue;
            }
            changed = true;
            if (h.shotsLeft() <= 1) {
                this.held.remove(i);
            } else {
                this.held.set(i, new Held(h.status(), 0, h.shotsLeft() - 1, 0));
            }
        }
        return changed;
    }

    /** What every held status does together. */
    StatModifiers<TowerStat> modifiers() {
        StatModifiers<TowerStat> total = StatModifiers.none();
        for (Held h : this.held) {
            total = total.plus(h.status().modifiers());
        }
        return total;
    }

    /** What every held status lets the tower do together. */
    Set<TowerCapability> capabilities() {
        Set<TowerCapability> all = EnumSet.noneOf(TowerCapability.class);
        for (Held h : this.held) {
            all.addAll(h.status().capabilities());
        }
        return all;
    }

    private int indexOf(TowerTerm term) {
        for (int i = 0; i < this.held.size(); i++) {
            if (this.held.get(i).status().term() == term) {
                return i;
            }
        }
        return -1;
    }

    /** Each held status as its pane shows it: a shot status by its shots left, a timed one by its clock. */
    List<TowerBuffState> states(int nowTick) {
        List<TowerBuffState> states = new ArrayList<>();
        for (Held h : this.held) {
            TowerBuffState state = TowerBuffState.of(h.status().term(), h.status().describe());
            if (h.status().shots() > 0) {
                states.add(h.shotsLeft() > 1 ? state.withStacks(h.shotsLeft()) : state);
            } else {
                states.add(state.withClock(Math.max(0, h.endsAt() - nowTick), h.clockTicks()));
            }
        }
        return states;
    }

    private record Held(TowerStatus status, int endsAt, int shotsLeft, int clockTicks) {
    }
}
