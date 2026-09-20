# `td.effect` — the shared timed status-effect primitive

Read the root `CLAUDE.md` first; this file only covers what is specific to this package.

## Shape

`Effect` is the runtime value: six kinds (`SLOW`, `BURN`, `FREEZE`, `SHIELD`, `INVISIBLE`,
`HEAL`), one record, no per-kind subclassing - every field has a meaningful identity value for
every kind, so resolving a mob's active effects never branches on which kind it's holding.
`ActiveEffects` holds at most one `Effect` per `EffectKind` on one mob, keyed by kind so a
reapplication is a refresh, not a stack. Both are deliberately unaware of `td.enemy` -
`DamageSink` is the one seam a producer (a tower, or an enemy's own ability) binds at the moment
an effect is created, which is what lets this package sit below `td.enemy` in the dependency
graph rather than needing to depend on it.

`EffectTemplate` (sealed: `ShieldTemplate`, `InvisibleTemplate`, `HealTemplate`) is the
*authored* counterpart - what an `Ability` carries as data, before a `DamageSink` is bound.
`SLOW`/`BURN`/`FREEZE` have no template today: nothing yet authors them on an enemy's own
ability, only a tower applies them directly via `Effect.slow`/`.burn`/`.freeze`. Add one the same
shape as `ShieldTemplate`/`HealTemplate` if an ability ever needs to.

**`ActiveEffects` exposes only queries, never its internal map.** `activeKinds()` (a snapshot,
for the UI marker row), `speedMultiplier()`, `isInvisible()`, `applyShield(Damage)`,
`healPerTick()` and `apply(Effect)`/`tick()` are the whole surface. Nothing outside this package
reaches into which `Effect` is stored under which kind.

## Healing is a query, not a sink

Every damaging kind (`BURN`) deals its damage *through* `Effect.sink()` - a `DamageSink` bound at
creation time, so a damage-over-time tick is credited to whichever tower applied it exactly like
an instant hit is. `HEAL` deliberately does **not** work this way: `Damage`'s own compact
constructor clamps every amount at zero specifically so a "healing hit" can never exist, and a
heal credits nobody the way a damage-over-time tick credits its tower. `Effect.healPerTick`
is instead a plain `int` a mob reads directly via `ActiveEffects.healPerTick()` and applies to
its own health field itself - the effect-side counterpart to `applyShield(Damage)`, which is the
same shape: a query the mob calls, not a mutation this holder performs on the mob's behalf.
`Effect.heal(...)`'s constructor still takes a `DamageSink`, unused, purely so every factory on
this record has the same shape and `withRemainingTicks` needs no per-kind branching.

## Ordering, once per tick

**A heal must be read before `ActiveEffects.tick()` runs, not after** - the same reasoning
`AbstractEnemyMob.doTick` already applies to `speedMultiplier()`: an effect entering the last
tick of its duration must still act *this* tick, and `tick()` removes an expiring effect before
returning. Reading `healPerTick()` afterward would silently shorten a heal's last tick the same
way reading `speedMultiplier()` afterward would silently shorten a slow's.

**A heal is capped at the mob's own max health and skipped entirely once the mob is dead** -
`AbstractEnemyMob` enforces both: the cap by `Math.min(healthMax, health + healPerTick)`, and the
dead-skip for free, since the heal application only runs inside `doTick`'s live-mob branch,
which a dead mob never reaches (a dead mob takes the sibling branch that captures `deathTick`
instead). A heal must never resurrect a mob already fading.

## Adding a new effect kind

1. Add the constant to `EffectKind`.
2. Add its case to `Effect`'s static factory (a new named factory, following `heal`/`shield`'s
   shape) and to `ActiveEffects.magnitude` - the compiler forces the second, since that switch
   has no `default`.
3. If the kind is authorable by an ability rather than only applied directly by a tower, add an
   `EffectTemplate` implementation (its `kind()` names the `EffectKind` it produces).
4. Add its `Palette.STATUS_MARKER_*` role and the matching cases in
   `td.ui.EnemyFrameBuilder.markerPaletteFor` and `td.ui.Java2DFrameRenderer.colorFor` - both
   exhaustive with no `default`, so a new kind is a compile error until its marker art exists.
