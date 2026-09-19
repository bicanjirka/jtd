package td.enemy;

import java.util.List;

/**
 * A data-driven enemy type: a name/id (the wave-script token), base stats, a graphical
 * representation, and the composable {@link Trait}s and {@link Ability}s that give it its
 * behavior - replacing what today is a hardcoded method override on a {@code final} leaf
 * class. Built once, at {@link EnemyCatalog} registration time, not per-spawn; {@link Trait}s
 * and {@link Ability}s are shared across every mob spawned from this definition regardless of
 * level (see {@link TraitContext}).
 * <p>
 * Carries its own {@code baseHealth}/{@code price}, but a v1 wave-spawned enemy ignores
 * them: its health and price are supplied per-wave, uniformly across whatever mix of types
 * that wave spawns (matching today's {@code WaveDefinition.hp()}/{@code price()} - see
 * {@code EnemyCatalog.spawn}'s explicit parameters, which override these). Only an
 * *ability*-spawned enemy (a {@code SpawnEnemiesAction}, resolved through
 * {@code DefinedEnemyMob}'s own ability execution) reads {@link #baseHealth()}/{@link #price()}
 * directly, since it has no wave slot of its own to inherit stats from - the Warden's boss egg
 * chain is what actually needs this.
 *
 * @param id            the wave-script token this definition spawns under - built-ins use a
 *                      single letter (matching today's {@code c}/{@code s}/{@code t}/
 *                      {@code g}/{@code e}); a per-level custom or cloned definition uses an
 *                      ordinary, longer id. There is no separate syntax for the two - the wave
 *                      mini-language treats every token as a plain id lookup against whichever
 *                      {@link EnemyCatalog} is in scope.
 * @param displayName   shown as the first line of the in-game info text.
 * @param description   shown as the second line of the in-game info text.
 * @param baseHealth    only consulted for an ability-spawned instance of this definition -
 *                      ignored for a wave-spawned one (see above).
 * @param price         only consulted for an ability-spawned instance of this definition -
 *                      ignored for a wave-spawned one (see above).
 * @param baseSpeed     pixels per tick before any level scaling or active effect - {@code 0}
 *                      means the mob never advances along the path at all (the boss egg),
 *                      which needs no separate "stationary" flag: {@code distanceIntoLap}
 *                      simply never accumulates.
 * @param healthDivisor the wave-supplied base health is divided by this before any other
 *                      scaling - generalizes Ghost's flat {@code /5}. {@code 1} for every
 *                      definition that doesn't need one.
 * @param mobType       which {@link EnemyMob.Type} this definition spawns as - what
 *                      type-filtering targeting queries (see {@code td.tower.targeting}) see,
 *                      independent of anything a {@link Trait} does.
 * @param traits        always-on, no per-mob state of their own beyond what {@link TraitContext} supplies
 * @param abilities     triggered behaviors - see {@link Ability}
 */
public record EnemyDefinition(
        String id,
        String displayName,
        String description,
        int baseHealth,
        int price,
        float baseSpeed,
        float healthDivisor,
        EnemyMob.Type mobType,
        BodyArchetype archetype,
        MovementBehavior movement,
        List<Trait> traits,
        List<Ability> abilities) {

    public EnemyDefinition {
        traits = List.copyOf(traits);
        abilities = List.copyOf(abilities);
    }

    /**
     * The required shape every enemy has: no description, {@link EnemyMob.Type#NORMAL}, a
     * {@link FixedMovement} pace, no health divisor, no traits, no abilities. A definition that
     * needs any of those calls the matching {@code withX} copy below instead of this factory
     * growing another parameter - the same "with"-copy shape {@link td.wave.PathDefinition} and
     * {@code td.util.LoadedLevel} already use.
     */
    public static EnemyDefinition of(String id, String displayName, int baseHealth, int price, float baseSpeed,
            BodyArchetype archetype) {
        return new EnemyDefinition(id, displayName, "", baseHealth, price, baseSpeed, 1f,
                EnemyMob.Type.NORMAL, archetype, new FixedMovement(), List.of(), List.of());
    }

    public EnemyDefinition withDescription(String description) {
        return new EnemyDefinition(this.id, this.displayName, description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.mobType, this.archetype, this.movement, this.traits,
                this.abilities);
    }

    public EnemyDefinition withHealthDivisor(float healthDivisor) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, healthDivisor, this.mobType, this.archetype, this.movement, this.traits,
                this.abilities);
    }

    public EnemyDefinition withMobType(EnemyMob.Type mobType) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, mobType, this.archetype, this.movement, this.traits,
                this.abilities);
    }

    public EnemyDefinition withMovement(MovementBehavior movement) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.mobType, this.archetype, movement, this.traits,
                this.abilities);
    }

    public EnemyDefinition withTraits(List<Trait> traits) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.mobType, this.archetype, this.movement, traits,
                this.abilities);
    }

    public EnemyDefinition withAbilities(List<Ability> abilities) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.mobType, this.archetype, this.movement, this.traits,
                abilities);
    }
}
