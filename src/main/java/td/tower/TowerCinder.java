package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.InWedgeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.util.GameWorld;

import java.util.List;

/**
 * "Cinder tower" - a continuous flame cone with no cooldown, slowly reorienting toward the
 * nearest enemy in range and burning everything currently caught in its wedge, ghosts
 * included - the same "hits everyone in the shape" spirit as {@link TowerFour}, just confined
 * to a cone instead of the whole range circle.
 * <p>
 * Unlike {@link TowerThree}'s continuously rotating beam, this cone only slowly reorients and
 * is never "between" two headings in a way that would let it miss something, so it decides
 * hits against its <em>current</em> heading every tick rather than an arc swept since the
 * last one (see {@code InWedgeTargetQuery}).
 * <p>
 * Cinder never calls {@code dealDamage} directly - its entire attack is applying (and, while
 * an enemy stays in the cone, continually refreshing) a burn on it. Damage still flows through
 * this tower's own {@code dealDamage} once the burn ticks (see {@code Effect}'s sink), so its
 * damage/kill accounting stays accurate without a second, parallel damage path.
 */
public final class TowerCinder extends AbstractTower {

    public static final int price = 28;
    public static final int damage = 150;
    public static final float range = 2.2f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.15;
    /** Public so the renderer's cone effect matches exactly what {@link InWedgeTargetQuery} decides hits against. */
    public static final double HALF_WIDTH_RADIANS = 0.35;
    private static final int BURN_DURATION_TICKS = 15;

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);

    public TowerCinder(GameWorld context, int x, int y) {
        super(TowerFactory.type.cinder, price, damage, range);
        this.doInit(context, x, y);
    }

    public void doTick(int gameTime) {
        List<EnemyMob> inRange = InRangeTargetQuery.anyType(this.centerX, this.centerY, this.rangeReal)
                .matching(this.context.getEnemyRegistry());
        // No target: hold the last heading rather than snapping back to a neutral angle - see
        // TurretAim's class doc on skipping tick() while idle.
        new NearestSelector(this.centerX, this.centerY).selectFrom(inRange)
                .ifPresent(nearest -> this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, nearest.getX(), nearest.getY())));

        List<EnemyMob> caught = new InWedgeTargetQuery(this.centerX, this.centerY, this.turretAim.currentRadians(), HALF_WIDTH_RADIANS)
                .and(InRangeTargetQuery.anyType(this.centerX, this.centerY, this.rangeReal))
                .matching(this.context.getEnemyRegistry());
        for (EnemyMob enemy : caught) {
            enemy.applyEffect(Effect.burn(Damage.magic(this.damageCurrent), BURN_DURATION_TICKS, d -> this.dealDamage(enemy, d)));
        }
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerCinder(this);
    }

    public String getInfoString() {
        return "Cinder tower\n\n" +
                super.getInfoString() +
                "Burns everything in a cone\n" +
                "No cooldown";
    }

    public String getStatusString() {
        return "Cinder tower\n\n" +
                super.getStatusString() +
                "Burns everything in a cone\n" +
                "No cooldown";
    }
}
