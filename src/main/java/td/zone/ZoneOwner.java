package td.zone;

import td.effect.DamageSink;
import td.effect.Effect;
import td.enemy.EnemyMob;

import java.util.function.Function;

/** The tower that made a zone: what a zone puts on an enemy is credited to it. */
public interface ZoneOwner {

    /** The owner of a zone an enemy leaves: what it puts on an enemy is credited to no one. */
    static ZoneOwner nobody() {
        return (target, effect) -> target.applyEffect(effect.apply(DamageSink.none()));
    }

    void applyEffect(EnemyMob target, Function<DamageSink, Effect> effect);
}
