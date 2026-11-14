package td.tower.proc;

import java.util.List;
import java.util.Optional;

/** Runs procs for an outcome or a shot; pure apart from what it asks of {@code actions}. */
public final class ProcRunner {

    private ProcRunner() {
    }

    public static void onOutcome(List<Proc> procs, AttackOutcome outcome, ProcActions actions) {
        for (Proc proc : procs) {
            if (proc.trigger().firesOn(outcome) && proc.holds(outcome)) {
                proc.payload().run(Optional.of(outcome.target()), actions);
            }
        }
    }

    public static void onShot(List<Proc> procs, int shotNumber, ProcActions actions) {
        for (Proc proc : procs) {
            if (proc.trigger().firesOnShot(shotNumber)) {
                proc.payload().run(Optional.empty(), actions);
            }
        }
    }
}
