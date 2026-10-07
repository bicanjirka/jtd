package td.enemy;

import td.damage.Damage;

/** Told on the game-loop thread when a shield takes part of a hit, with the part it took. */
public interface ShieldListener {

    void shieldTook(EnemyMob enemy, Damage absorbed);
}
