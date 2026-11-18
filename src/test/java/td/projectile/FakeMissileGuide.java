package td.projectile;

import td.enemy.EnemyMob;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A guide that keeps every target but the one it was told to drop, turns to the one it was given, and records what happens. */
final class FakeMissileGuide implements MissileGuide {

    private final List<EnemyMob> landed = new ArrayList<>();
    private Optional<EnemyMob> retarget = Optional.empty();
    private EnemyMob dropped;
    private int homes;

    /** From now on it gives up {@code target}. */
    FakeMissileGuide dropping(EnemyMob target) {
        this.dropped = target;
        return this;
    }

    /** From now on a missile with nothing worth hunting turns to {@code target}. */
    FakeMissileGuide turningTo(EnemyMob target) {
        this.retarget = Optional.of(target);
        return this;
    }

    List<EnemyMob> landed() {
        return List.copyOf(this.landed);
    }

    int homes() {
        return this.homes;
    }

    @Override
    public boolean keeps(EnemyMob target) {
        return target != this.dropped;
    }

    @Override
    public Optional<EnemyMob> retarget(double x, double y) {
        return this.retarget;
    }

    @Override
    public void land(EnemyMob target, int gameTime) {
        this.landed.add(target);
    }

    @Override
    public void arriveHome() {
        this.homes++;
    }
}
