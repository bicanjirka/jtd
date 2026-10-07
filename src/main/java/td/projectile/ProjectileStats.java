package td.projectile;

/**
 * How a projectile flies, how big it is and what it looks like: a tower stat the info rows show and
 * the board draws.
 *
 * @param speed  pixels a tick
 * @param size   a multiple of the standard projectile's size; {@code 1} is standard
 * @param look   what it carries
 * @param streak whether it is fast enough to be drawn with a streak behind it
 */
public record ProjectileStats(float speed, float size, MissileLook look, boolean streak) {

    /** A standard-sized, standard-looking projectile flying {@code speed} pixels a tick. */
    public static ProjectileStats of(float speed) {
        return new ProjectileStats(speed, 1f, MissileLook.STANDARD, false);
    }

    public ProjectileStats withSize(float size) {
        return new ProjectileStats(this.speed, size, this.look, this.streak);
    }

    public ProjectileStats withLook(MissileLook look) {
        return new ProjectileStats(this.speed, this.size, look, this.streak);
    }

    public ProjectileStats withStreak(boolean streak) {
        return new ProjectileStats(this.speed, this.size, this.look, streak);
    }
}
