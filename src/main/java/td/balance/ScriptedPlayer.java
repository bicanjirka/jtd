package td.balance;

import td.util.ThreadConfined;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Plays back a list of timed actions: each is taken at its moment, or at the first turn after it that it goes
 * through, so a script recorded on one version of the game still plays on a changed one where rounds end at
 * other ticks or an action is not yet affordable. Against the same game and seed it repeats the recorded play
 * exactly.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class ScriptedPlayer implements Player {

    private final Deque<PlayRecord.TimedAction> pending;

    public ScriptedPlayer(List<PlayRecord.TimedAction> script) {
        this.pending = new ArrayDeque<>(script);
    }

    /** A player that plays {@code script} back, named {@code name}. */
    public static PlayerSpec spec(String name, List<PlayRecord.TimedAction> script) {
        return new PlayerSpec(name, (survey, seed) -> new ScriptedPlayer(script));
    }

    @Override
    public void act(Turn turn) {
        while (!this.pending.isEmpty() && this.pending.peekFirst().dueBy(turn.round(), turn.offset())) {
            if (!turn.perform(this.pending.peekFirst().action())) {
                return;
            }
            this.pending.removeFirst();
        }
    }
}
