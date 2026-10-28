import td.DevEffects;
import td.DevField;
import td.damage.AttackOrigin;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.util.TickRate;

import java.util.List;

/**
 * {@code effect <kind> [ticks]} for the Driver and PreviewBoard: puts the effect on every enemy as the dev panel
 * builds it, with the panel's starting values, so a new effect kind needs nothing here.
 */
final class EffectCommand {

    private static final int DEFAULT_TICKS = 200;

    private EffectCommand() {
    }

    /** Applies the effect {@code args} names to every enemy in {@code enemies}; returns the line to print. */
    static String apply(String args, EnemyMob[] enemies) {
        String[] parts = args.split("\\s+");
        EffectKind kind = EffectKind.valueOf(parts[0].toUpperCase());
        int ticks = parts.length > 1 ? Integer.parseInt(parts[1]) : DEFAULT_TICKS;
        var refusal = DevEffects.whyUnusable(kind);
        if (refusal.isPresent()) {
            return "ERROR effect " + kind + ": " + refusal.get();
        }
        List<Float> values = DevEffects.enemyFields(kind).stream()
                .map(field -> field.equals(DevField.seconds()) ? ticks / TickRate.TICKS_PER_SECOND : field.initial())
                .toList();
        for (EnemyMob mob : enemies) {
            for (Effect effect : DevEffects.enemyEffects(kind, values, DamageSink.none(), AttackOrigin.none())) {
                mob.applyEffect(effect);
            }
        }
        return "OK effect " + kind;
    }
}
