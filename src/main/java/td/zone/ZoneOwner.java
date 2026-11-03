package td.zone;

import td.damage.AttackOrigin;
import td.effect.DamageSink;
import td.effect.Effect;
import td.enemy.EnemyMob;

import java.util.function.Function;

/** The tower that made a zone: what a zone puts on an enemy is credited to it. */
public interface ZoneOwner {

    /**
     * The owner of a zone an enemy leaves: what it puts on an enemy is credited to no one, and comes from this owner
     * alone, so the heals of two zones add up while one zone's pulses only refresh its own.
     */
    static ZoneOwner nobody() {
        AttackOrigin source = AttackOrigin.fresh();
        return (target, effect) -> target.applyEffect(effect.apply(DamageSink.none()).from(source));
    }

    void applyEffect(EnemyMob target, Function<DamageSink, Effect> effect);
}
