package td.tower.sniper;

import td.tower.TowerTerm;
import td.wave.Vec2;

import java.util.List;

/**
 * A shot that hopped on from one enemy to the next.
 *
 * @param from   where it hopped from, in pixels
 * @param bullet the hop itself, aimed at the next enemy
 * @param rounds the rounds of the shot it hopped from, which the hop itself no longer carries
 */
public record Hop(Vec2 from, Bullet bullet, List<TowerTerm> rounds) {

    public Hop {
        rounds = List.copyOf(rounds);
    }
}
