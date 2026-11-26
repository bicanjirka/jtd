package td.zone;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.enemy.EnemyMob;

import java.util.function.Function;

/** A {@link ZoneOwner} that puts each effect on its target with a sink that credits nobody, and hits it unaided. */
final class FakeZoneOwner implements ZoneOwner {

    @Override
    public void applyEffect(EnemyMob target, Function<DamageSink, Effect> effect) {
        target.applyEffect(effect.apply(DamageSink.none()));
    }

    @Override
    public void dealPeriodicDamage(EnemyMob target, Damage damage) {
        target.takeHit(damage, AttackProfile.none().asPeriodic());
    }
}
