package td.projectile;

/**
 * How a projectile flies and how big it is: a tower stat the info rows show and the board draws.
 *
 * @param speed pixels a tick
 * @param size  a multiple of the standard projectile's size; {@code 1} is standard
 */
public record ProjectileStats(float speed, float size) {

    /** A standard-sized projectile flying {@code speed} pixels a tick. */
    public static ProjectileStats of(float speed) {
        return new ProjectileStats(speed, 1f);
    }

    public ProjectileStats withSize(float size) {
        return new ProjectileStats(this.speed, size);
    }
}
