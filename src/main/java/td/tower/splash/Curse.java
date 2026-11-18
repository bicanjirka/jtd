package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.ShotFlight;
import td.wave.Vec2;

/** A hex on its way to the enemy it curses. */
public record Curse(EffectKind kind, Vec2 from, EnemyMob target, ShotFlight flight) {
}
