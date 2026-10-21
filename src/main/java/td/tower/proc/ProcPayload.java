package td.tower.proc;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.TowerCapability;
import td.tower.TowerStat;
import td.tower.TowerStatus;
import td.util.TickRate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** What a proc does when it fires; each record does it and says it. */
public sealed interface ProcPayload {

    /** Runs on {@code target}, which is absent for a shot trigger. */
    void run(Optional<EnemyMob> target, ProcActions actions);

    /** The status this payload grants the tower, if it grants one. */
    default Optional<TowerStatus> grantedStatus() {
        return Optional.empty();
    }

    /** "Vulnerable +1", "Charged 3 s", "Dispel", "Shield -25%", or what a status does. */
    String describe();

    /** {@code stacks} of the stacking debuff {@code kind}. */
    record ApplyStacks(EffectKind kind, int stacks) implements ProcPayload {
        @Override
        public void run(Optional<EnemyMob> target, ProcActions actions) {
            target.filter(enemy -> !enemy.isDead()).ifPresent(enemy -> actions.applyStacks(enemy, this.kind, this.stacks));
        }

        @Override
        public String describe() {
            return Words.name(this.kind) + " +" + this.stacks;
        }
    }

    /** Charges the target for {@code ticks}. */
    record Charge(int ticks) implements ProcPayload {
        @Override
        public void run(Optional<EnemyMob> target, ProcActions actions) {
            target.filter(enemy -> !enemy.isDead()).ifPresent(enemy -> actions.charge(enemy, this.ticks));
        }

        @Override
        public String describe() {
            return "Charged " + seconds(this.ticks);
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
        public String describe() {
            String length = this.status.shots() == 1 ? "next shot"
                    : this.status.shots() > 0 ? "next " + this.status.shots() + " shots"
                    : seconds(this.status.durationTicks());
            List<String> parts = new ArrayList<>();
            for (TowerStat stat : this.status.modifiers().stats()) {
                parts.add(stat.label() + " " + stat.describe(this.status.modifiers().get(stat)));
            }
            for (TowerCapability capability : this.status.capabilities()) {
                parts.add(capability.label());
            }
            return length + ": " + String.join(", ", parts);
        }
    }

    /** Strips the target's shield and heal. */
    record Dispel() implements ProcPayload {
        @Override
        public void run(Optional<EnemyMob> target, ProcActions actions) {
            target.filter(enemy -> !enemy.isDead()).ifPresent(EnemyMob::dispelRestoratives);
        }

        @Override
        public String describe() {
            return "Dispel";
        }
    }

    /** Takes {@code share} of what the target's shield has left. */
    record BreakShield(float share) implements ProcPayload {
        @Override
        public void run(Optional<EnemyMob> target, ProcActions actions) {
            target.filter(enemy -> !enemy.isDead()).ifPresent(enemy -> enemy.breakShield(this.share));
        }

        @Override
        public String describe() {
            return "Shield -" + Math.round(this.share * 100) + "%";
        }
    }

    private static String seconds(int ticks) {
        return BigDecimal.valueOf(ticks / (double) TickRate.TICKS_PER_SECOND).stripTrailingZeros().toPlainString()
                .toLowerCase(Locale.ROOT) + " s";
    }
}
