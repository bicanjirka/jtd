# `td.enemy` — the enemy mob hierarchy

Read the root `CLAUDE.md` first; this file only covers what is specific to this package.

## Shape

`EnemyMob` is the interface every consumer (towers, targeting queries, renderers) sees.
`AbstractEnemyMob` holds everything shared: position, movement along the path, health,
death and the fade animation's timing. Two thin subclasses add only a facing angle:

| Class | Facing comes from |
|---|---|
| `AbstractEnemyMobDirectional` | the path's own geometry at this mob's position |
| `AbstractEnemyMobRotor` | a constant spin, accumulated per tick |

The five leaf classes are `final`: `EnemyMobCircle`, `EnemyMobSquare`, `EnemyMobTriangle`,
`EnemyMobGhost`, `EnemyMobEmpty`. `EnemyFactory.Enemy` is the enum that maps a wave-script
letter to a leaf class and constructs it.

## Invariants worth knowing before you change anything here

**`doInit` must be called from the leaf constructor, and `super.doInit` first.** A leaf's
own `doInit` override reads `this.context` and `this.level`, both of which
`AbstractEnemyMob.doInit` sets. `EnemyMobGhost` additionally rewrites the `health` argument
on the way *down* (`super.doInit(context, delay, health / 5, price, level)`), so its
override runs before the base ever sees the value.

**Facing is never derived from a per-tick pixel delta.** Enemies move at sub-pixel speeds
(`speedBase` is 1.28 px/tick), so an `atan2` over one tick's movement intermittently
collapses to zero. `AbstractEnemyMobDirectional` reads the path's exact geometric facing
instead. Don't "simplify" it back.

**Movement is real arc-length distance, not a per-segment tick budget.** `doTick` advances
a `distanceIntoLap` accumulator by `speed` pixels and resolves it through the shared
`td.wave.ArcLengthPath`. That is why a curved or diagonal path moves enemies at the same
real-world pace as a straight one.

**Reaching the path's end is a wrap, not a despawn.** `distanceIntoLap` wraps back to zero,
the player is charged an `EconomyDelta.leak`, and `prevX`/`prevY` are resynced to the new
position — without that resync the renderer interpolates across the whole board for one
frame and draws a streak.

**`doDamage` returns the damage that actually landed, not what was passed in.** `absorb`
lets a mob resist part of a hit; a mob that is not a valid target takes none of it at all;
and the result is capped at the health the mob had left, so a killing blow reports only what
it actually removed rather than its overkill. That cap is also why a dead mob sits at exactly
zero health instead of going negative. Anything reporting damage figures —
`AbstractTower.dealDamage` is the only such caller today — must use the return value. An
override must propagate it (see `EnemyMobTriangle`).

**The health cap is applied to `absorb`'s result, via `Damage.cappedAt`, never by re-wrapping
the incoming hit.** `absorb` is free to change a hit's `DamageType` as well as its amount (a
future magic-resistant trait would do exactly this); capping from the original `damage`
argument instead would silently discard whatever type `absorb` chose.

**Death timing is captured in `doTick`, not lazily at paint time.** `doDamage` sets
`dead`; the next `doTick` records `deathTick`. The fade therefore advances with the
simulation clock, so it runs at the same rate while fast-forwarding as the rest of the game.
`ticksSinceDeath` can legitimately return `-1` for an already-dead mob whose `doTick` has
not run yet — `fadeAlpha` clamps for exactly this reason.

**A degenerate path is a supported state, not a bug.** A `GameWorld` before any level loads
has a path with fewer than two points; `ArcLengthPath.of` returns empty and the mob holds
still at the path's first point (or the origin). Keep that branch.

## Adding a new enemy type

1. Add the leaf class (make it `final`), extending `AbstractEnemyMob` or one of the two
   facing subclasses.
2. Add a constant to `EnemyFactory.Enemy` with its wave-script letter, and its `create`
   branch. The `switch` there has no `default`, so this step cannot be forgotten silently.
3. Add a `visit…` method to `EnemyMobVisitor` — the compiler will then point you at
   `td.ui.EnemyFrameBuilder`, which is where the new mob's art gets described.
4. Add a `Palette` constant and its shape/colour cases in `td.ui.Java2DFrameRenderer`
   (`enemyShape`, `colorFor`, and the fade switch in `paintEnemyFade`).
5. Document the new letter in the root `CLAUDE.md`'s wave mini-language table and in
   `README.md`'s enemy table.

**Never branch on a mob's concrete type with `instanceof` or a `switch`.** Use
`EnemyMobVisitor`. The whole point of the visitor is that step 3 above turns "I forgot to
draw the new enemy" into a compile error.

## Roster

`EnemyRoster` owns the live per-wave array and the alive count; `EnemyRegistry` is the
read-only slice of it that targeting queries and renderers depend on — depend on the
narrow interface, not the roster, unless you actually need to mutate.

`clear()` (level teardown) deliberately does **not** call `GameHost.enemyDied`, while
`remove()` (an actual kill) does. Renaming or merging those two would make returning to the
menu spuriously trigger the "you won" overlay.
