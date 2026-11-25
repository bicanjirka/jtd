package td.tower.splash;

import td.enemy.EnemyMob;
import td.wave.Vec2;

import java.util.Optional;

/**
 * A bolt that struck where {@code to} was.
 *
 * @param shocked  the enemy it hit there; empty when that enemy dodged it
 * @param critical whether it hit as a crit
 */
public record Bolt(Vec2 from, Vec2 to, Optional<EnemyMob> shocked, int tick, BoltKind kind, boolean critical) {

    /** A jump of the Arc from {@code from} that hit {@code struck} where it stands. */
    public static Bolt jump(Vec2 from, EnemyMob struck, int tick, boolean critical) {
        return new Bolt(from, struck.position(), Optional.of(struck), tick, BoltKind.ARC, critical);
    }

    /** A cast's bolt of {@code kind} from {@code from} that hit {@code struck} where it stands; a cast never crits. */
    public static Bolt cast(Vec2 from, EnemyMob struck, int tick, BoltKind kind) {
        return new Bolt(from, struck.position(), Optional.of(struck), tick, kind, false);
    }

    /** This bolt, its enemy having dodged it. */
    public Bolt missed() {
        return new Bolt(this.from, this.to, Optional.empty(), this.tick, this.kind, this.critical);
    }
}
