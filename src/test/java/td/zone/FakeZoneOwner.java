package td.zone;

import td.effect.DamageSink;
import td.effect.Effect;
import td.enemy.EnemyMob;

import java.util.function.Function;

/** A {@link ZoneOwner} that puts each effect on its target with a sink that credits nobody. */
final class FakeZoneOwner implements ZoneOwner {

    @Override
    public void applyEffect(EnemyMob target, Function<DamageSink, Effect> effect) {
        target.applyEffect(effect.apply(damage -> {
        }));
    }
}
