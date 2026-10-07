package td.tower.splash;

import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What a Sympathy carrier shares: the effects of {@code from} that {@code to} lacks. Each copy
 * tops {@code to} up to what {@code from} has, never past it, with {@code from}'s own time left, so
 * two enemies sharing back and forth settle at the same debuffs instead of climbing.
 */
public final class SympathyCopier {

    private SympathyCopier() {
    }

    /** The effects that bring {@code to} level with the shared debuffs {@code from} carries. */
    public static List<Effect> missingFrom(EnemyMob from, EnemyMob to) {
        List<Effect> copies = new ArrayList<>();
        for (Effect effect : from.activeEffects()) {
            if (effect.kind().sharedBySympathy()) {
                topUp(effect, to).ifPresent(copies::add);
            }
        }
        return copies;
    }

    private static Optional<Effect> topUp(Effect source, EnemyMob to) {
        return switch (source.kind()) {
            case VULNERABLE -> missingStacks(source, to)
                    .map(stacks -> Effect.vulnerable(stacks, source.remainingTicks(), source.sink()));
            case SUNDERED -> missingStacks(source, to)
                    .map(stacks -> Effect.sundered(stacks, source.remainingTicks(), source.sink()));
            case EXPOSED -> to.hasEffect(EffectKind.EXPOSED) ? Optional.empty()
                    : Optional.of(Effect.exposed(source.remainingTicks(), source.sink()));
            case CHILL -> missingChill(source, to);
            default -> Optional.empty();
        };
    }

    private static Optional<Integer> missingStacks(Effect source, EnemyMob to) {
        int missing = source.stacks() - to.effectStacks(source.kind());
        return missing > 0 ? Optional.of(missing) : Optional.empty();
    }

    private static Optional<Effect> missingChill(Effect source, EnemyMob to) {
        float held = (float) to.activeEffects().stream()
                .filter(effect -> effect.kind() == EffectKind.CHILL)
                .mapToDouble(Effect::fuelLevel)
                .sum();
        float missing = source.fuelLevel() - held;
        return missing > 0f && source.ticksToFade() > 0
                ? Optional.of(Effect.chill(missing, source.ticksToFade(), source.sink())) : Optional.empty();
    }
}
