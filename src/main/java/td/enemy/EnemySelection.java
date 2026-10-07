package td.enemy;

import td.util.ThreadConfined;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The enemy the player is inspecting. The EDT only posts a request ({@link #requestAt},
 * {@link #requestClear}); the game-loop thread resolves it at the start of each frame build
 * ({@link #resolve}), so a live mob is only ever read by its owner.
 * <p>
 * A selection outlives the mob's death, leak or hatch, keeping its last snapshot with that fate
 * until the next request. A mob that leaves the roster still alive is dropped.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class EnemySelection {

    /** A click this close to a body's edge, relative to its radius, still picks it. */
    private static final float CLICK_RADIUS_FACTOR = 1.5f;
    private static final float MIN_CLICK_RADIUS = 12f;
    private static final EnemyMobVisitor<DefinedEnemyMob> AS_DEFINED = mob -> mob;
    private static final Request NONE = new Request.None();

    private final AtomicReference<Request> pending = new AtomicReference<>(NONE);
    private DefinedEnemyMob selected;

    /** From any thread: select the enemy nearest to this board pixel, or clear if none is near. */
    public void requestAt(int boardX, int boardY) {
        this.pending.set(new Request.At(boardX, boardY));
    }

    /** From any thread: drop the selection. */
    public void requestClear() {
        this.pending.set(new Request.Clear());
    }

    /**
     * Applies any pending request, then snapshots the selection. Empty when nothing is selected.
     */
    public Optional<EnemyInspection> resolve(EnemyRegistry enemies) {
        Request request = this.pending.getAndSet(NONE);
        EnemyMob[] roster = enemies.getEnemies();
        switch (request) {
            case Request.None ignored -> {
            }
            case Request.Clear ignored -> this.selected = null;
            case Request.At at -> this.selectNearest(roster, at.x(), at.y());
        }
        if (this.selected == null) {
            return Optional.empty();
        }
        if (this.selected.fate() == EnemyInspection.Fate.ALIVE && !contains(roster, this.selected)) {
            this.selected = null;
            return Optional.empty();
        }
        return Optional.of(this.selected.inspect());
    }

    /** The selected mob while it is still alive, for the selection ring. */
    public Optional<EnemyMob> selectedAlive() {
        if (this.selected == null || this.selected.isDead() || this.selected.fate() != EnemyInspection.Fate.ALIVE) {
            return Optional.empty();
        }
        return Optional.of(this.selected);
    }

    private void selectNearest(EnemyMob[] roster, int x, int y) {
        DefinedEnemyMob nearest = null;
        double nearestDistance2 = Double.MAX_VALUE;
        for (EnemyMob enemy : roster) {
            DefinedEnemyMob mob = enemy.accept(AS_DEFINED);
            if (mob.isDead() || mob.isInactive() || mob.isHidden()) {
                continue;
            }
            double dx = mob.getX() - x;
            double dy = mob.getY() - y;
            double distance2 = dx * dx + dy * dy;
            float reach = Math.max(CLICK_RADIUS_FACTOR * mob.getBodyScale(), MIN_CLICK_RADIUS);
            if (distance2 <= reach * reach && distance2 < nearestDistance2) {
                nearest = mob;
                nearestDistance2 = distance2;
            }
        }
        this.selected = nearest;
    }

    private static boolean contains(EnemyMob[] roster, EnemyMob mob) {
        for (EnemyMob enemy : roster) {
            if (enemy == mob) {
                return true;
            }
        }
        return false;
    }

    private sealed interface Request {

        record None() implements Request {
        }

        record Clear() implements Request {
        }

        record At(int x, int y) implements Request {
        }
    }
}
