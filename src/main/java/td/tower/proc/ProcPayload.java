package td.tower.proc;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerStatus;
import td.util.TickRate;

import java.util.Optional;

/** What a proc does when it fires; each record does it and says it. */
public sealed interface ProcPayload {

    /** Runs on {@code target}, which is absent for a shot trigger. */
    void run(Optional<EnemyMob> target, ProcActions actions);

    /** The status this payload grants the tower, if it grants one. */
    default Optional<TowerStatus> grantedStatus() {
        return Optional.empty();
    }

    /** What it does, said after its trigger: "applies a stack of [Vulnerable]". */
    TermText describe();

    /** How it reads on a counted shot: a one-shot status is that shot. */
    default TermText describeForTheShot() {
        return this.describe();
    }

    /** {@code stacks} of the stacking debuff {@code kind}. */
    record ApplyStacks(EffectKind kind, int stacks) implements ProcPayload {
        @Override
        public void run(Optional<EnemyMob> target, ProcActions actions) {
            target.filter(enemy -> !enemy.isDead()).ifPresent(enemy -> actions.applyStacks(enemy, this.kind, this.stacks));
        }

        @Override
        public TermText describe() {
            return this.stacks == 1 ? TermText.of("applies a stack of ").plus(this.kind)
                    : TermText.of("applies " + this.stacks + " stacks of ").plus(this.kind);
        }
    }

    /** Charges the target for {@code ticks}. */
    record Charge(int ticks) implements ProcPayload {
        @Override
        public void run(Optional<EnemyMob> target, ProcActions actions) {
            target.filter(enemy -> !enemy.isDead()).ifPresent(enemy -> actions.charge(enemy, this.ticks));
        }

        @Override
        public TermText describe() {
            return TermText.of("applies ").plus(EffectKind.CHARGED)
                    .plus(" for " + TermNumbers.seconds(this.ticks / (float) TickRate.TICKS_PER_SECOND));
        }
    }

    /** Grants the tower {@code status}. */
    record GrantStatus(TowerStatus status) implements ProcPayload {
        @Override
        public Optional<TowerStatus> grantedStatus() {
            return Optional.of(this.status);
        }

        @Override
        public void run(Optional<EnemyMob> target, ProcActions actions) {
            actions.grantStatus(this.status);
        }

        @Override
        public TermText describe() {
            return TermText.of("grants ").plus(this.mention());
        }

        @Override
        public TermText describeForTheShot() {
            if (this.status.shots() != 1) {
                return this.describe();
            }
            return TermText.of("is " + ProcText.article(this.status.term()) + " ").plus(this.mention());
        }

        private TermText mention() {
            return TermText.of(this.status.term(), this.status.describe());
        }
    }

    /** Strips the target's shield and heal. */
    record Dispel() implements ProcPayload {
        @Override
        public void run(Optional<EnemyMob> target, ProcActions actions) {
            target.filter(enemy -> !enemy.isDead()).ifPresent(EnemyMob::dispelRestoratives);
        }

        @Override
        public TermText describe() {
            return TermText.of("removes ").plus(EffectKind.SHIELD).plus(" and ").plus(EffectKind.HEAL);
        }
    }

    /** Takes {@code share} of what the target's shield has left. */
    record BreakShield(float share) implements ProcPayload {
        @Override
        public void run(Optional<EnemyMob> target, ProcActions actions) {
            target.filter(enemy -> !enemy.isDead()).ifPresent(enemy -> enemy.breakShield(this.share));
        }

        @Override
        public TermText describe() {
            return TermText.of("takes " + TermNumbers.percent(this.share) + " of what ").plus(EffectKind.SHIELD)
                    .plus(" has left");
        }
    }
}
