package td.tower.proc;

/** What sets a proc off; each record says when it fires and how it reads. */
public sealed interface ProcTrigger {

    default boolean firesOn(AttackOutcome outcome) {
        return false;
    }

    default boolean firesOnShot(int shotNumber) {
        return false;
    }

    /** Whether it fires when the target dodges an attack, which no other trigger sees. */
    default boolean firesOnDodge() {
        return false;
    }

    /** The shots between two firings of a shot trigger; 0 for every other trigger. */
    default int shotInterval() {
        return 0;
    }

    /** "On hit", "On crit", "On kill", "On dodge", "Every 3rd shot". */
    String describe();

    /** Every landed attack. */
    record OnHit() implements ProcTrigger {
        @Override
        public boolean firesOn(AttackOutcome outcome) {
            return true;
        }

        @Override
        public String describe() {
            return "On hit";
        }
    }

    /** A critical attack. */
    record OnCrit() implements ProcTrigger {
        @Override
        public boolean firesOn(AttackOutcome outcome) {
            return outcome.critical();
        }

        @Override
        public String describe() {
            return "On crit";
        }
    }

    /** An attack that kills. */
    record OnKill() implements ProcTrigger {
        @Override
        public boolean firesOn(AttackOutcome outcome) {
            return outcome.killed();
        }

        @Override
        public String describe() {
            return "On kill";
        }
    }

    /** An aimed attack the target dodged. */
    record OnDodge() implements ProcTrigger {
        @Override
        public boolean firesOnDodge() {
            return true;
        }

        @Override
        public String describe() {
            return "On dodge";
        }
    }

    /** Every {@code n}th attack the tower begins. */
    record EveryNthShot(int n) implements ProcTrigger {
        @Override
        public int shotInterval() {
            return this.n;
        }

        @Override
        public boolean firesOnShot(int shotNumber) {
            return this.n > 0 && shotNumber % this.n == 0;
        }

        @Override
        public String describe() {
            return "Every " + Words.ordinal(this.n) + " shot";
        }
    }
}
