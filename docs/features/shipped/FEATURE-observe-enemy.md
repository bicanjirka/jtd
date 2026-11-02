# Feature Request: Observe Enemy

**Status: implemented.** Everything below shipped as decided. What building it settled:

- Trace lines go to the session log file only, not the console, through a queue of their own that waits
  instead of dropping. They can land a few lines out of place among the other loggers' lines; each
  carries its tick, and one enemy's lines keep their order.
- A dev panel action runs between ticks, so its effects carry the tick before it; an `END` after a
  kill from the panel is stamped on the next tick, when the death is settled.
- Cost (`td.PerformanceHarness`, last built-in level, every cell a tower): with nothing observed, tick
  p99 is unchanged at about 0.21 ms; one enemy observed, about 0.27 ms; every enemy, about 0.46 ms; the
  ground-zone loadout with every enemy observed, about 1.0 ms, at the budget. `observe=one|all` repeats
  the measurement.

## Summary

A dev-panel switch that makes enemies report, tick by tick, everything that happens to them -
every hit and how it was reduced, every effect, heal, shield, ability and how they ended - to the
game's log. Tests prove each rule alone; this shows how the rules combine on a real enemy in a
real run, so a surprise seen in play ("why did that survive?", "did the freeze land or
diminish?") can be read off instead of guessed, then pinned down by a test. Its main reader is
Claude driving the game through `run-jtd`; the developer reads it too.

## Current state (what exists today)

- `DefinedEnemyMob` is the only concrete `EnemyMob`. `EnemyMobVisitor` reaches it from rendering
  (`EnemyFrameBuilder`), the Info pane (`EnemyInfoText`), `EnemySelection` and `DevControls`.
- Much of what happens to an enemy never crosses its interface; it happens inside the mob:
  - regeneration, in `doTick` through `MobHealth.regenerate`;
  - effect ticks, stack decay and expiry, in `ActiveEffects.tick`, seen per tick by
    `EffectTransitions.observe`;
  - resistance, diminishing returns and the "shorter than a tick, blocked" rule, in
    `DefinedEnemyMob.applyResisted`; Inversion turning a heal or shield into damage, in
    `applyEffect`;
  - the hit sequence in `takeHit`/`land`: mark spent, dodge, `HitResolution` (crit, mitigation),
    shield absorption, the cap at health, a charge discharging, death and its payout.
- `HitResolution.resolve` returns only the final `Damage`; the steps between (crit, armor or magic
  resist after penetration, plating or insulation, damage taken) are not kept.
- Periodic damage reaches the mob through `takeHit` like a hit, with a periodic `AttackProfile`
  (`AbstractTower.dealPeriodicDamage`), so it never crits or spends a mark. A fuel pool several
  towers feed splits each tick's damage into one sink call per contributor
  (`ActiveEffects`).
- An `AttackProfile` carries an `AttackOrigin`, which is identity only: no name, no position.
  `td.damage` may not depend on `td.tower`. Dev-panel effects and ability effects carry
  `AttackOrigin.none()`.
- A hit carries no game time: `doTick` stamps pending moments on the next tick
  (`MobMoments.capturePending`), because towers hit after the mob's own tick ran.
- An enemy killed by a hit runs its death-tick abilities (an on-death spawn or burst) on its next
  `doTick`, once its death is captured.
- The only per-mob number is `entryOrdinal`, which is `0` until the spawn delay ends.
- A hatch replaces the mob with a new one (`EnemySpawner.replace`); ability spawns are built by
  `AbilitySpawnFactory` at the caster's position and rank.
- The wave preview and tooltips build `DefinedEnemyMob`s that never enter the board.
- The dev panel (`PanelDev`, ctrl+shift+d) has a "Selected" section; with an enemy selected it
  offers Apply effect, Clear, Kill, Heal, Remove. The Spawn row holds the script field, rank,
  path, the Spawn button and the script preview. Its actions go through `DevControls`, which
  queues them to run on the game-loop thread (`onSelectedEnemy`). Every dev control has an
  `alt+<letter>` shortcut, listed by `alt+h`; the `run-jtd` Driver drives the panel through them
  because component indices shift.
- Logging is SLF4J + Logback (`src/main/resources/logback.xml`): root at INFO, to the console and
  to `logs/jTD-<timestamp>.log` through an async appender that never blocks and drops events once
  its 2048-entry queue is full.
- The `run-jtd` Driver has no command that reads the log.
- Performance budget: tick p99 ≤ 1 ms with every buildable cell a tower (`td.PerformanceHarness`).

## What this feature adds

### Choosing what is observed

- An **Observe** toggle on the Selected section's enemy card observes the selected enemy from that
  moment until it is toggled off.
- An **Observe** toggle on the Spawn row: while it is on, every enemy the Spawn button creates is
  observed from its first tick, its spawn delay included. Enemies a wave spawns can be observed
  only by selecting them.
- Any number of enemies may be observed at once.
- Whatever an observed enemy spawns, or hatches into, is observed too.
- Only the dev panel can make an enemy observed: no wave script, level or saved game can.
- The Selected card shows the id of every selected enemy, observed or not, and the toggle shows
  whether it is observed. There is no marker on the board.

### The lines

- One line per event, written as it happens, so lines keep the order things happened in:
  `OBSERVE t=<tick> #<id> <Name> <Rank> <EVENT> key=value ...`. The tick is the one the event
  happened in.
- Health and damage in points, as the Info pane shows them; effect, tower and enemy names as the
  game titles them.
- `from=` names the cause: `<Tower>@<x>,<y>` (a tower by name and board cell), `#<id>` (an
  enemy's cast), `dev:<action>` (`dev:kill`, `dev:kill-all`, `dev:effect`, `dev:heal`, ...) or `-`
  (nothing in particular). A dev action never passes for game behaviour.
- A line belongs to the enemy it happened to. A caster logs that it cast (what, radius, how many
  targets); each target that is itself observed logs what the effect did to it, `from=#<id>`.
  What an enemy does to towers shows only in its cast line.
- Every line goes to the session log at INFO, and none is dropped: while anything is observed
  the game may run slower rather than lose a line. With nothing observed, nothing changes and the
  performance budget holds.

### Events

- **START**: a full snapshot - health (current and max), every base and resolved stat, traits,
  active effects - and how observation began: `selected`, `spawn box`, `spawned by #<id>` or
  `hatched from #<id>`.
- **HIT**: the attacker, type, and every step from what arrived to what landed: crit (or a spent
  mark), each reduction with the value it was resolved against (armor or magic resist after
  penetration, plating or insulation, damage taken), what the shield absorbed, the cap at health;
  health before and after; overkill. **DODGE** for a dodged hit.
- **Periodic damage**: every tick, one line per contributing tower, with the same steps minus crit
  and mark, and the effect it came from.
- **Effects**, discrete changes only: gained (strength and length), refreshed (old and new
  length), stack count changed (decay included), shortened by resistance or diminishing returns,
  blocked, kept out by another effect, turned into damage by Inversion; ended, with why: expired,
  dispelled, consumed, stacks lost, cleared from the dev panel. A continuous level (Chill's
  strength, fullness) appears in these lines and in END, never as a line of its own each tick.
- **Health and shields**: regeneration or a heal that changed health; a shield gained or broken.
- **Abilities**: what it cast or spawned.
- **KILLED** at the killing hit, by whom, with overkill.
- **END**, always the last line for its id, written after any death-tick abilities: how it ended
  (killed, leaked, hatched into `#<id>`, removed, stopped, level ended), how long it lived, and its
  totals: damage taken per source and type, absorbed by shields, dodges, overkill. Restarting,
  going back to the menu or loading a game ends every observed enemy with `level ended`.
- A **Snapshot** button on the Spawn row writes a START-style full snapshot of every observed
  enemy on demand; with nothing observed it writes one line saying so. It is always enabled.

### Controls and the Driver

- Both Observe toggles and Snapshot get `alt+<letter>` dev shortcuts, listed by `alt+h`.
- A `run-jtd` Driver command, `trace [#id]`, prints the `OBSERVE` lines of the game the Driver
  launched, all of them or one enemy's.
- The `run-jtd` skill's description (the part loaded in every session) says it can trace what
  happens to an enemy tick by tick; its command table documents `trace` and the shortcuts. This
  ships with the Driver change.

### Out of scope for v1

Showing the trace on screen, filtering lines by event, a board marker over observed enemies,
recording traces in headless balance runs, keeping observation through a save and load, and
observing towers.

## Interconnections

- `TODO.md`'s "What the balance tools cannot see yet" lists measures the recorder doesn't take
  (damage wasted on overkill and shields). END's totals show them for one enemy live, but don't
  close that gap: the balance tools need them in aggregate, headless.

## Constraints and open risks

- The engine is headless and the mob is owned by the game-loop thread; the dev panel lives on the
  EDT. Turning observation on or off, and Snapshot, cross that boundary.
- Hits arrive without a game time, but each line must carry the tick it happened in. Stamping a
  hit on the mob's next tick would be off by one.
- Naming an attacker by tower and cell needs a way from an `AttackOrigin` to a tower that keeps
  `td.damage` free of `td.tower`.
- The id must exist from the moment a mob is built; `entryOrdinal` doesn't.
- Showing every reduction step needs `HitResolution`'s intermediate values, on a per-hit path that
  must cost nothing measurable while nothing is observed.
- The session log is written by an async appender that drops; lines that may never be dropped
  must share that file without corrupting it or arriving out of order.
- Enemies built for the wave preview and tooltips must never be observed.
- The Spawn row is already full; two more controls must fit without squeezing the script preview.
- Content rules still hold: tests prove that each event is reported with the numbers its input
  gives, never what a built-in enemy logs.

## Decisions made

- Name: **Observe enemy**. Dev panel only; never in a level's waves, never saved.
- Output goes to the existing session log, not a new file or window.
- The choices above under "What this feature adds" are the developer's, settled in review:
  selection plus a spawn-box toggle, any number at once, spawns and hatches inherit, no board
  marker, ids on every selected enemy, fixed `key=value` lines in points and game names, towers by
  name and cell, dev causes named, lines owned by the enemy they happened to, a full START
  snapshot, every reduction step on hits, every periodic tick, discrete effect changes only,
  KILLED then END after death-tick abilities, a Snapshot button, never drop a line, shortcuts for
  every control, and a Driver `trace` command with the skill updated alongside.
- Tests: one per event source, on a fixture enemy with a recording fake, numbers from the test's
  input; one formatter test per event kind for the exact line, since `trace` and grep depend on
  it; one that an unobserved enemy reports nothing; and one `GameEngineTest` following a
  spawn-box enemy through a hatch to its child's `hatched from` START. The performance harness
  proves the unobserved cost.
- The one live `run-jtd` pass checks only Swing wiring: the toggles and shortcuts, the id on the
  Selected card, the Spawn row's layout, and `trace` reading a real session.
- It complements tests, never replaces them: a finding from a trace becomes a test.

## Open questions

None.
