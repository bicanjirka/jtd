package td.enemy;

import java.util.List;

/**
 * A data-driven enemy type: a name/id (the wave-script token), base stats, a graphical
 * representation, and the composable {@link Trait}s and {@link Ability}s that give it its
 * behavior - replacing what today is a hardcoded method override on a {@code final} leaf
 * class. Built once, at {@link EnemyCatalog} registration time, not per-spawn.
 *
 * @param id          the wave-script token this definition spawns under - built-ins use a
 *                    single letter (matching today's {@code c}/{@code s}/{@code t}/{@code g}/
 *                    {@code e}); a per-level custom or cloned definition uses an ordinary,
 *                    longer id. There is no separate syntax for the two - the wave mini-language
 *                    treats every token as a plain id lookup against whichever
 *                    {@link EnemyCatalog} is in scope.
 * @param baseSpeed   pixels per tick before any level scaling or active effect - {@code 0}
 *                    means the mob never advances along the path at all (the boss egg), which
 *                    needs no separate "stationary" flag: {@code distanceIntoLap} simply never
 *                    accumulates.
 * @param traits      always-on, no per-mob state of their own beyond what the mob itself tracks
 * @param abilities   triggered behaviors - see {@link Ability}
 */
public record EnemyDefinition(
        String id,
        String displayName,
        int baseHealth,
        float baseSpeed,
        int price,
        BodyArchetype archetype,
        MovementBehavior movement,
        List<Trait> traits,
        List<Ability> abilities) {

    public EnemyDefinition {
        traits = List.copyOf(traits);
        abilities = List.copyOf(abilities);
    }
}
