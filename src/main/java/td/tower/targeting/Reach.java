package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.List;

/**
 * Whom a tower may hit: what its query matches, less a dead zone around it. Perks widen or narrow
 * the query in the order they were bought; the dead zone is applied last, so it holds however the
 * query was widened. It is a fixed distance: range bonuses don't move it.
 *
 * @param view     where the tower looks from
 * @param query    the enemies it may hit, before the dead zone
 * @param deadZone how close, in pixels, is too close; {@code 0} for none
 */
public record Reach(Viewpoint view, TargetQuery query, float deadZone) implements TargetQuery {

    /** The visible enemies in range, with no dead zone. */
    public static Reach visible(Viewpoint view) {
        return new Reach(view, InRangeTargetQuery.visible(view.x(), view.y(), view.range()), 0f);
    }

    /** Every enemy in range, hidden ones included, with no dead zone. */
    public static Reach everyone(Viewpoint view) {
        return new Reach(view, InRangeTargetQuery.everyone(view.x(), view.y(), view.range()), 0f);
    }

    public Reach withQuery(TargetQuery query) {
        return new Reach(this.view, query, this.deadZone);
    }

    /** Also the enemies {@code other} matches. */
    public Reach widenedBy(TargetQuery other) {
        return this.withQuery(this.query.or(other));
    }

    public Reach withDeadZone(float deadZone) {
        return new Reach(this.view, this.query, deadZone);
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemies) {
        if (this.deadZone <= 0f) {
            return this.query.matching(enemies);
        }
        return this.query.and(new BeyondRadiusTargetQuery(this.view.x(), this.view.y(), this.deadZone))
                .matching(enemies);
    }
}
