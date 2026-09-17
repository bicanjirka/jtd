package td.enemy;

import td.damage.Damage;
import td.economy.EconomyDelta;
import td.effect.ActiveEffects;
import td.effect.Effect;
import td.effect.EffectKind;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.ArcLengthPath;
import td.wave.PathPose;
import td.wave.Vec2;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Everything every enemy shares: spawn delay, movement along the level's path, health and
 * damage, and the death-fade animation's timing. {@link DefinedEnemyMob} adds a body scale,
 * a facing angle, and whatever behavioural twist its {@link Trait}s give it (see
 * {@link #absorb} and {@link HurtSpeedTrait}'s speed-up on damage).
 * <p>
 * Movement is real arc-length distance: {@link #doTick} advances {@code distanceIntoLap} by
 * {@code speed} pixels and resolves it through the shared {@link ArcLengthPath}, so a curved
 * or diagonal path moves a mob at the same real-world pace a straight one does. Reaching the
 * end wraps back to the start and charges the player a {@link EconomyDelta#leak} - a mob is
 * never removed from the roster by walking, only by dying.
 * <p>
 * <strong>Everything a mob is born with is set by this constructor and is {@code final}.</strong>
 * A mob used to be built in two phases - an empty constructor, then a {@code doInit} a leaf had
 * to remember to call last - which left the whole object mutable and visible half-built. The
 * compiler enforces the ordering now, the same way it does for {@code td.tower.AbstractTower}.
 * <p>
 * Everything that remains mutable is per-tick simulation state, and it is {@code private}: a
 * mob is owned by the {@code game-loop} thread and published to that thread by the
 * {@code CopyOnWriteArrayList} it is added to. Fourteen {@code protected} mutable fields meant
 * no invariant here could survive a subclass; a leaf now reaches state through the accessors
 * below. See CLAUDE.md 3 and 5.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
// per-tick simulation state; the frame build that reads it runs on the loop thread too
public abstract class AbstractEnemyMob implements EnemyMob {

    /**
     * Pixels per tick for a mob whose definition names no speed of its own. Rescaled from the
     * old fixed-point model (speed=40 meant "40/1000 of the current segment per tick", which -
     * since every segment was exactly one 32px cell - worked out to 40/1000*32 = 1.28 px/tick),
     * so every enemy's actual speed is unchanged; only the unit it is expressed in is.
     */
    public static final float DEFAULT_SPEED = 1.28f;
    /**
     * Health is stored in hundredths, matching the scale {@code td.tower} expresses damage in
     * ({@code SniperTower.DAMAGE} of {@code 4000} is 40 points a shot). Storing the fine-grained
     * unit is what lets a percentage resistance or a damage-over-time tick subtract a fraction
     * of a point without rounding to nothing.
     */
    private static final int HEALTH_UNITS_PER_POINT = 100;
    /**
     * How many ticks of spawn delay one slot of wave ordering is worth, at DEFAULT_SPEED.
     */
    private static final float DELAY_TICKS_PER_SLOT = 22.4f;

    // Injected collaborators and the constants a mob is born with. All final, all set below.
    protected final GameWorld gameWorld;
    protected final int level;
    private final Type type;
    private final int price;
    private final int healthMax;
    private final ActiveEffects activeEffects = new ActiveEffects();
    /**
     * The path this mob measures its progress along, empty for a degenerate path - fewer than
     * two points, as a world has before any level is installed. An empty one has nothing to
     * measure distance along, so the mob holds at {@link #stationaryPosition} instead of moving.
     */
    private final Optional<ArcLengthPath> arcLengthPath;
    private final Vec2 stationaryPosition;

    // Per-tick simulation state, owned by the game-loop thread. Private: a leaf that needs one
    // of these goes through an accessor, so this class can hold an invariant over them.
    private int health;
    private boolean inactive = true;
    private boolean validTarget = false;
    private boolean dead = false;
    private float speed;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private int delay;
    private int deathTick = -1;
    private double distanceIntoLap = 0;
    private double lastFacingRadians = 0;

    /**
     * Binds this mob to a world, its type and speed, and a starting position on its path.
     * <p>
     * A leaf passes its own constants straight through - there is no second initialization
     * step to remember, and no window in which a half-built mob is reachable. Anything a leaf
     * derives from the board scale or from {@code level} (a body scale, a speed curve) it
     * computes after this returns, by which point every field here is set.
     *
     * @param delay  this mob's slot index within its wave, converted here into a tick countdown
     *               before it becomes active and targetable
     * @param health in whole points; stored internally in hundredths
     */
    protected AbstractEnemyMob(GameWorld gameWorld, Type type, float speed,
                               int delay, int health, int price, int level) {
        this.gameWorld = gameWorld;
        this.type = type;
        this.speed = speed;
        this.price = price;
        this.level = level;
        this.health = health * HEALTH_UNITS_PER_POINT;
        this.healthMax = health * HEALTH_UNITS_PER_POINT;
        this.arcLengthPath = ArcLengthPath.of(gameWorld.getPath());
        List<Vec2> pathPoints = gameWorld.getPath().points();
        this.stationaryPosition = pathPoints.isEmpty() ? new Vec2(0, 0) : pathPoints.getFirst();
        // Rescaled the same way speed was (700f -> 700f*0.032 = 22.4f) so spawn timing is
        // unchanged now that speed is a direct px/tick value rather than a 0-999-per-segment
        // fixed-point unit.
        this.delay = Math.round(DELAY_TICKS_PER_SLOT * delay / speed);
        if (delay == 0) {
            this.inactive = false;
            this.validTarget = true;
        }
        this.updatePosition();
        // No prior tick to interpolate from at spawn - start with prev == current.
        this.prevX = this.x;
        this.prevY = this.y;
    }

    public int getHealth() {
        return this.health;
    }

    public int getBounty() {
        return this.price;
    }

    public float getHealthFraction() {
        return (float) this.health / this.healthMax;
    }

    public boolean isInactive() {
        return this.inactive;
    }

    /**
     * Applies a hit, paying the player its bounty and removing it from the roster's alive
     * count if this kills it. A hit on an already-dead mob is a no-op - several towers can
     * fire into the same mob within one tick, and only the first may count as the kill (see
     * {@code AbstractTower.dealDamage}, which relies on that).
     *
     * @return the damage that actually landed: {@link #absorb}'s result capped at the health
     * this mob had left, and {@link Damage#none()} for a mob that is already dead or not
     * currently targetable. The cap means a killing blow reports only the health it actually
     * removed, so overkill is not credited to whoever fired it.
     */
    public Damage doDamage(Damage damage) {
        if (this.dead) {
            return Damage.none();
        }
        Damage landed = Damage.none();
        if (this.validTarget()) {
            landed = this.absorb(damage).cappedAt(this.health);
            this.health -= landed.amount();
        }
        if (this.health <= 0) {
            this.validTarget = false;
            this.dead = true;
            this.gameWorld.economy().apply(EconomyDelta.kill(this.price));
            this.gameWorld.enemies().remove();
        }
        return landed;
    }

    /**
     * Hook for a mob that resists part of an incoming hit (see {@link DefinedEnemyMob#absorb},
     * which folds every {@link Trait#onHit}). The default is no resistance - the damage lands
     * unchanged.
     */
    protected Damage absorb(Damage incoming) {
        return incoming;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    /**
     * This mob's x/y as of the tick before last, i.e. the interpolation source for a render
     * landing between two ticks. Equal to getX()/getY() at spawn and immediately after a
     * path-end wrap, where there is nothing meaningful to interpolate from.
     */
    public double getPrevX() {
        return this.prevX;
    }

    public double getPrevY() {
        return this.prevY;
    }

    /**
     * This mob's speed, folding in every currently active speed-affecting {@link Effect}
     * (see {@link #getSpeed()}). {@code speed} itself stays the intrinsic value - the one
     * {@link DefinedEnemyMob#doDamage} recomputes from its traits' {@code speedFactor} and the
     * constructor reads for the spawn delay - so a slow or freeze never gets permanently baked
     * into it, and never gets wiped out the next time a trait recomputes it.
     */
    private float effectiveSpeed() {
        return this.speed * this.activeEffects.speedMultiplier();
    }

    public float getSpeed() {
        return this.effectiveSpeed();
    }

    /**
     * Replaces this mob's intrinsic speed - what a {@link Trait}'s {@code speedFactor}
     * recomputes when the mob is hurt (see {@link DefinedEnemyMob#doDamage}). Deliberately the
     * intrinsic value and not the effective one: a slow or a freeze multiplies this at read
     * time (see {@link #effectiveSpeed()}), so recomputing here never bakes a status effect in
     * permanently and never wipes one out.
     */
    protected void setSpeed(float speed) {
        this.speed = speed;
    }

    public void applyEffect(Effect effect) {
        this.activeEffects.apply(effect);
    }

    public Set<EffectKind> activeEffectKinds() {
        return this.activeEffects.activeKinds();
    }

    /**
     * A coarse "how far into this lap of the path" ranking value, used only to compare two
     * enemies on the same path (see FurthestAlongPathSelector) - sub-pixel precision has no
     * practical effect on that ranking, so this stays an int rather than widening to match
     * the double-precision position/distance it's derived from.
     */
    public int getProgression() {
        return (int) this.distanceIntoLap;
    }

    /**
     * The exact, sub-pixel distance into the path's current lap - see {@link #jumpToDistance}.
     */
    protected double getDistanceIntoLap() {
        return this.distanceIntoLap;
    }

    /**
     * Places this mob at an arbitrary point along the path instead of the start every mob
     * otherwise spawns at - what an ability-driven spawn (the Warden's egg, a reinforcement)
     * uses to appear where the spawning mob actually was, via
     * {@code DefinedEnemyMob.spawnAtSamePositionAs}.
     */
    protected void jumpToDistance(double distanceIntoLap) {
        this.distanceIntoLap = distanceIntoLap;
        this.updatePosition();
        this.prevX = this.x;
        this.prevY = this.y;
    }

    /**
     * Spawned, on the board, and still alive - the precondition every targeting query applies.
     */
    public boolean validTarget() {
        return ((!this.inactive) && this.validTarget && (!this.dead));
    }

    public boolean validTarget(Type type) {
        return (this.validTarget() && (type.equals(this.type)));
    }

    public boolean validTarget(Type type1, Type type2) {
        return (this.validTarget(type1) || this.validTarget(type2));
    }

    public boolean isDead() {
        return this.dead;
    }

    /**
     * Ticks elapsed since this mob died, or -1 while still alive. Death timing is
     * captured here (the first doTick call after doDamage() sets dead=true) rather
     * than lazily inside paint(), so the death-fade animation advances with the
     * simulation clock instead of with however often the board happens to repaint.
     */
    public int ticksSinceDeath(int gameTime) {
        return this.deathTick < 0 ? -1 : gameTime - this.deathTick;
    }

    public int fadeDurationTicks() {
        return 3 * this.level + 6;
    }

    public boolean isFadeComplete(int gameTime) {
        int age = this.ticksSinceDeath(gameTime);
        return age > this.fadeDurationTicks();
    }

    /**
     * ticksSinceDeath can be -1 (death recorded by doDamage() mid-tick, but this
     * mob's own doTick() hasn't run yet to capture deathTick) - clamp to a valid
     * Color alpha range rather than let that produce a value above 255.
     */
    public int fadeAlpha(int ticksSinceDeath) {
        int alpha = 255 - (ticksSinceDeath * (255 / (this.fadeDurationTicks() + 1)));
        return Math.min(255, Math.max(alpha, 0));
    }

    /**
     * Recomputes x/y (and, while moving, the precise path-facing angle) from the current
     * arcLengthPath/distanceIntoLap state. A degenerate path has nothing to measure distance
     * along, so it just holds at its one available point.
     */
    private void updatePosition() {
        if (this.arcLengthPath.isPresent()) {
            PathPose pose = this.arcLengthPath.get().poseAt(this.distanceIntoLap);
            this.x = pose.position().x();
            this.y = pose.position().y();
            this.lastFacingRadians = pose.facingRadians();
        } else {
            this.x = this.stationaryPosition.x();
            this.y = this.stationaryPosition.y();
        }
    }

    /**
     * The path's own facing direction at this mob's current position - exact, geometry-based,
     * not derived from a pixel delta over one tick (see {@link PathDirectionalMovement}).
     */
    protected double getPathFacingRadians() {
        return this.lastFacingRadians;
    }

    /**
     * Counts down the spawn delay, or - for a live mob - resolves this tick's active status
     * effects and then advances it along the path, or - once dead - records the tick death
     * happened on so the fade can be timed against the simulation clock rather than the
     * repaint rate. A damage-over-time effect that kills the mob this tick skips movement
     * entirely for the rest of this call - there is nothing left to move.
     */
    public void doTick(int gameTime) {
        if (this.inactive) {
            if (this.delay > 0) {
                this.delay--;
                if (this.delay == 0) {
                    this.inactive = false;
                    this.validTarget = true;
                    this.doTick(gameTime);
                }
            }
        } else if (this.dead) {
            if (this.deathTick < 0) {
                this.deathTick = gameTime;
            }
        } else {
            // Read this tick's speed multiplier before ticking durations down, so an effect
            // entering the last tick of its duration still suppresses this tick's movement -
            // ActiveEffects.tick() removes it before returning, and querying afterward would
            // silently shorten its effect on movement by one tick relative to its effect on
            // damage-over-time (which it applies before removing itself either way).
            float speedMultiplier = this.activeEffects.speedMultiplier();
            this.activeEffects.tick();
            if (this.dead) {
                // A damage-over-time tick just killed this mob - doDamage() already set
                // dead=true synchronously (its sink calls straight back into doDamage()).
                // Movement must not run this tick: there is nothing left to move.
                return;
            }
            this.prevX = this.x;
            this.prevY = this.y;
            boolean wrappedToPathStart = false;
            if (this.arcLengthPath.isPresent()) {
                this.distanceIntoLap += this.speed * speedMultiplier;
                double totalLength = this.arcLengthPath.get().totalLength();
                if (this.distanceIntoLap >= totalLength) {
                    this.distanceIntoLap -= totalLength;
                    wrappedToPathStart = true;
                    this.gameWorld.economy().apply(EconomyDelta.leak(this.price == 0 ? 10 : this.price));
                }
            }
            this.updatePosition();
            this.validTarget = this.x >= 0 && this.x <= this.gameWorld.getBoard().maxX() && this.y >= 0 && this.y <= this.gameWorld.getBoard().maxY();
            if (wrappedToPathStart) {
                // Reappearing at the path's start is a genuine teleport, not motion along
                // it - interpolating from the old (near path-end) position would draw a
                // streak clear across the board for one frame.
                this.prevX = this.x;
                this.prevY = this.y;
            }
        }
    }
}
