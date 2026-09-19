package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.InWedgeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.tower.upgrade.DamageDealtCondition;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;

import java.util.List;

/**
 * "Cinder tower" - a continuous flame cone with no cooldown, slowly reorienting toward the
 * nearest enemy in range and burning everything currently caught in its wedge, ghosts
 * included - the same "hits everyone in the shape" spirit as {@link PulseTower}, just confined
 * to a cone instead of the whole range circle.
 * <p>
 * Unlike {@link SonarTower}'s continuously rotating beam, this cone only slowly reorients and
 * is never "between" two headings in a way that would let it miss something, so it decides
 * hits against its <em>current</em> heading every tick rather than an arc swept since the
 * last one (see {@code InWedgeTargetQuery}).
 * <p>
 * Cinder never calls {@code dealDamage} directly - its entire attack is applying (and, while
 * an enemy stays in the cone, continually refreshing) a burn on it. Damage still flows through
 * this tower's own {@code dealDamage} once the burn ticks (see {@code Effect}'s sink), so its
 * damage/kill accounting stays accurate without a second, parallel damage path.
 */
public final class CinderTower extends AbstractTower {

    public static final int PRICE = 28;
    public static final int DAMAGE = 150;
    public static final float RANGE = 2.2f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.15;
    private static final double HALF_WIDTH_RADIANS_BASE = 0.35;
    private static final int BURN_DURATION_TICKS = 15;
    private static final double WIDE_NOZZLE_HALF_WIDTH_MULTIPLIER = 1.4;

    /**
     * More damage (and so more burn per tick, since burn's magnitude is this tower's own damageCurrent) - earned by proven output.
     */
    private static final UpgradePath WHITE_FLAME = new UpgradePath(
            "White Flame", 30, TowerBuff.damage(0.4f), new DamageDealtCondition(15000));
    /**
     * A wider cone and more range - a straightforward money-gated specialization needing no track record.
     */
    private static final UpgradePath WIDE_NOZZLE = new UpgradePath(
            "Wide Nozzle", 25, TowerBuff.range(0.3f), UpgradeCondition.always(), "+40% cone width");
    private static final List<UpgradePath> PATHS = List.of(WHITE_FLAME, WIDE_NOZZLE);
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    // Bought on the EDT (onUpgradePathChosen) and read every tick on the game-loop thread, so
    // it is published volatile - CLAUDE.md 3 rule 2. Each is an independent scalar with no
    // invariant tying it to another, which is what makes a volatile scalar the right mechanism
    // here rather than a TowerStats-style snapshot: reading last pulse's value for one tick
    // after an upgrade is correct, just briefly stale.
    private volatile double halfWidthRadians = HALF_WIDTH_RADIANS_BASE;

    public CinderTower(GameWorld context, int x, int y) {
        // No cooldown: it burns whatever is in its cone every tick - see rateLine.
        super(TowerFactory.Type.CINDER, PRICE, DAMAGE, RANGE, 0, context, x, y);
    }

    @Override
    public List<UpgradePath> availablePaths() {
        return PATHS;
    }

    /**
     * Wide Nozzle's wider cone isn't a {@link TowerBuff} axis, so it's applied here instead.
     */
    @Override
    protected void onUpgradePathChosen(UpgradePath path) {
        if (path == WIDE_NOZZLE) {
            this.halfWidthRadians *= WIDE_NOZZLE_HALF_WIDTH_MULTIPLIER;
        }
    }

    public void doTick(int gameTime) {
        List<EnemyMob> inRange = InRangeTargetQuery.anyType(this.centerX, this.centerY, this.rangeReal())
                .matching(this.context.enemies());
        // No target: hold the last heading rather than snapping back to a neutral angle - see
        // TurretAim's class doc on skipping tick() while idle.
        new NearestSelector(this.centerX, this.centerY).selectFrom(inRange)
                .ifPresent(nearest -> this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, nearest.getX(), nearest.getY())));

        List<EnemyMob> caught = new InWedgeTargetQuery(this.centerX, this.centerY, this.turretAim.currentRadians(), this.halfWidthRadians)
                .and(InRangeTargetQuery.anyType(this.centerX, this.centerY, this.rangeReal()))
                .matching(this.context.enemies());
        for (EnemyMob enemy : caught) {
            enemy.applyEffect(Effect.burn(Damage.magic(this.damageCurrent()), BURN_DURATION_TICKS, d -> this.dealDamage(enemy, d)));
        }
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public double getHalfWidthRadians() {
        return this.halfWidthRadians;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitCinderTower(this);
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
