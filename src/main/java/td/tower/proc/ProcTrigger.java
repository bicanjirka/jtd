package td.tower.proc;

import td.term.TermNumbers;
import td.term.TermText;

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

    /** When it fires, said of {@code noun}, the hit its proc's conditions name, or empty for any hit. */
    TermText describe(TermText noun);

    /** Whether it counts the tower's shots, so a one-shot status it grants is that shot. */
    default boolean countsShots() {
        return false;
    }

    /** Every landed attack. */
    record OnHit() implements ProcTrigger {
        @Override
        public boolean firesOn(AttackOutcome outcome) {
            return true;
        }

        @Override
        public TermText describe(TermText noun) {
            return noun.isEmpty() ? TermText.of("each hit") : TermText.of("each ").plus(noun);
        }
    }

    /** A critical attack. */
    record OnCrit() implements ProcTrigger {
        @Override
        public boolean firesOn(AttackOutcome outcome) {
            return outcome.critical();
        }

        @Override
        public TermText describe(TermText noun) {
            return noun.isEmpty() ? TermText.of("a crit") : TermText.of("a crit ").plus(noun);
        }
    }

    /** An attack that kills. */
    record OnKill() implements ProcTrigger {
        @Override
        public boolean firesOn(AttackOutcome outcome) {
            return outcome.killed();
        }

        @Override
        public TermText describe(TermText noun) {
            return noun.isEmpty() ? TermText.of("a kill") : TermText.of("a ").plus(noun).plus(" that kills");
        }
    }

    /** An aimed attack the target dodged. */
    record OnDodge() implements ProcTrigger {
        @Override
        public boolean firesOnDodge() {
            return true;
        }

        @Override
        public TermText describe(TermText noun) {
            return TermText.of("a dodged ").plus(noun.isEmpty() ? TermText.of("shot") : noun);
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
        public TermText describe(TermText noun) {
            return TermText.of("every " + TermNumbers.ordinal(this.n) + " shot");
        }

        @Override
        public boolean countsShots() {
            return true;
        }
    }
}
