package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.projectile.MissileProjectile;
import td.tower.buff.TowerBuff;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.List;

/**
 * "Seeker tower" - fires a homing missile at whichever visible enemy is furthest along the
 * path, the same target choice as {@link SniperTower}. Unlike {@link MortarTower}'s shell, the
 * missile re-aims each tick at its target's live position and retargets to the nearest
 * remaining enemy if that target dies or leaks before it arrives (see
 * {@code MissileProjectile}). On impact it deals magic damage and freezes whichever mob it
 * actually reached - which may not be the one it was originally fired at.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)  // cooldown and current target, advanced by doTick
public final class SeekerTower extends AbstractTower {

    public static final int PRICE = 35;
    // Buffed from 1800/60 (30 dmg/tick) - see TODO.md's "New tower numbers are unbalanced
    // placeholders" formation-test entry: at the old numbers this was the lowest-DPS attack
    // tower in the game by a wide margin (first's 4000/39 is ~103/tick) with no splash/sweep/
    // continuous-AoE multiplier to make up the gap, and it registered zero kills against even
    // a small enemy column in that test. 2600/45 (~58 dmg/tick) roughly doubles its throughput
    // without matching or exceeding first's, keeping guaranteed-hit reliability and freeze CC
    // as the reason to pick this over a cheaper single-target tower rather than raw DPS alone.
    public static final int DAMAGE = 2600;
    public static final float RANGE = 4.5f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final float PROJECTILE_SPEED = 35f;
    private static final int FREEZE_DURATION_TICKS_BASE = 30;
    private static final float DEEP_FREEZE_DURATION_MULTIPLIER = 1.5f;

    /**
     * Faster reloading - a straightforward money-gated specialization needing no track record.
     */
    private static final UpgradePath TWIN_WARHEAD = new UpgradePath(
            "Twin Warhead", 30, TowerBuff.fireRate(0.35f), UpgradeCondition.always());
    /**
     * More damage and a longer freeze - earned by this tower having racked up proven kills.
     */
    private static final UpgradePath DEEP_FREEZE = new UpgradePath(
            "Deep Freeze", 35, TowerBuff.damage(0.3f), new KillCountCondition(10), "+50% freeze duration");
    private static final List<UpgradePath> PATHS = List.of(TWIN_WARHEAD, DEEP_FREEZE);

    /**
     * Ticks between shots before any fire-rate buff - paired with this tower's damage.
     */
    private static final int COOLDOWN_MAX = 45;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    // Bought on the EDT (onUpgradePathChosen) and read every tick on the game-loop thread, so
    // it is published volatile - CLAUDE.md 3 rule 2. Each is an independent scalar with no
    // invariant tying it to another, which is what makes a volatile scalar the right mechanism
    // here rather than a TowerStats-style snapshot: reading last pulse's value for one tick
    // after an upgrade is correct, just briefly stale.
    private volatile int freezeDurationTicks = FREEZE_DURATION_TICKS_BASE;
    private int coolDown = 0;
    private EnemyMob currentTarget;

    public SeekerTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SEEKER, PRICE, DAMAGE, RANGE, COOLDOWN_MAX, 0f, context, x, y);
    }

    @Override
    public List<UpgradePath> availablePaths() {
        return PATHS;
    }

    /**
     * Deep Freeze's longer duration isn't a {@link TowerBuff} axis, so it's applied here instead.
     */
    @Override
    protected void onUpgradePathChosen(UpgradePath path) {
        if (path == DEEP_FREEZE) {
            this.freezeDurationTicks = Math.round(this.freezeDurationTicks * DEEP_FREEZE_DURATION_MULTIPLIER);
        }
    }

    private EnemyMob findTarget() {
        List<EnemyMob> inRange = InRangeTargetQuery.ofType(this.centerX, this.centerY, this.rangeReal(), EnemyMob.Type.NORMAL)
                .matching(this.context.enemies());
        return new FurthestAlongPathSelector().selectFrom(inRange).orElse(null);
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            this.currentTarget = this.findTarget();
            if (this.currentTarget != null) {
                this.fireAt(this.currentTarget);
                this.coolDown = this.coolDownCurrent();
            }
        }
        // No target: hold the last heading rather than snapping back to a neutral angle - see
        // TurretAim's class doc on skipping tick() while idle.
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    private void fireAt(EnemyMob target) {
        this.context.projectiles().add(new MissileProjectile(this.centerX, this.centerY, target,
                this.context.enemies(), PROJECTILE_SPEED, this::onImpact));
    }

    private void onImpact(EnemyMob target) {
        this.dealDamage(target, Damage.magic(this.damageCurrent()));
        target.applyEffect(Effect.freeze(this.freezeDurationTicks, d -> this.dealDamage(target, d)));
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    int getFreezeDurationTicks() {
        return this.freezeDurationTicks;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSeekerTower(this);
    }

    public String getInfoString() {
        return "Seeker tower\n\n" +
                super.getInfoString() +
                "Fires a homing missile\n" +
                "Freezes what it hits";
    }

    public String getStatusString() {
        return "Seeker tower\n\n" +
                super.getStatusString() +
                "Fires a homing missile\n" +
                "Freezes what it hits";
    }
}
