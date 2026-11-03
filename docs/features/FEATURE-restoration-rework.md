# Feature Request: Restoration rework

## Summary

Heals, regeneration, shields and spirit follow one small set of rules instead of several overlapping
ones. A heal is regeneration for as long as it lasts; spirit scales what regeneration restores and what
a shield is worth, and nothing else; shields stack and take a hit before armor. The goal is a game whose
recovery mechanics a player can predict from the sheet, and code with one place for each rule.

## Current state (what exists today)

### Spirit

- `EnemyStat.SPIRIT` runs from −100 with no upper bound. Built-in enemies author 0 to 60; only
  Sickened (a poison mark, one point per stack, up to 100) takes it below zero.
- It does three jobs:
  1. It scales heals and shields by `max(0, 1 + spirit/100)` × healing received.
  2. It paces the timers of 32 debuff kinds, floored at a quarter speed and stopped at −100.
  3. It paces how Sickened and Scorched lose stacks (no floor; never at −100).
- The pacing is one idea spread over six places: `EffectPacing` (three values), a pacing column and two
  builders on `EffectRule`, 32 tagged `EffectKind` rows plus an `isPacedBySpirit` exception,
  `ActiveEffects` (`MIN_DEBUFF_PACE`, `STACK_DECAY_INTERVAL_TICKS`, `paceSteps`, `countDown`,
  `decayStacks`, a step loop in `tickChill`, three `tick` overloads), the factor computed in
  `DefinedEnemyMob.doTick`, and Spirit's meaning text and `td/effect/CLAUDE.md`. It costs almost no
  processing time.
- An effect's tooltip shows "Time left" as remaining ticks ÷ tick rate, which ignores pacing. On a
  spirit −50 enemy a paced debuff really lasts twice what the tooltip says; at +60, about 40% less.
  (Read from `EnemyStatText.effectRows`, not checked in the game.)

### Healing received

- `EnemyStat.HEALING_RECEIVED`, default 100%. No enemy authors it; only Cinder's Cauterize changes it
  (−50% while burning, "gets 50% less from Heal and Shield"). Its row shows only while it is off 100%.

### Heal and regeneration

- `REGENERATION` is a stat in hp/s. The Regeneration trait adds a plain flat that spirit does not scale.
- A heal (`HEAL`, `HealOverTime`) enters the stat sheet through a separate restorative channel
  (`StatAccumulator.restoreFlat`), which `StatSheet` scales by spirit × healing received. That forces a
  two-phase resolve: spirit and healing received first, then every other stat.
- The stat breakdown records the restorative part as a plain flat and leaves the scaling out. So the
  hover shows Base 0, Heal +20, Now 32 with nothing explaining the extra 12, and it counts the heal
  before the multipliers where the sheet counts it after.
- An enemy holds one Heal; a reapplication keeps the stronger amount and the longer time.
- Health regenerates in whole units, with the fraction carried to the next tick and the total capped
  at max health (`MobHealth.regenerate`). One point is 100 damage units.
- Heal sources: Mender (an ally pulse every 20 ticks lasting 40–80, a self-heal, Last Rites on death),
  Fast's boss and its spawnlings, Salamander, Yeti, and the Healing Ground zone (pulsing, each pulse
  lasting two pulses). Today's one-Heal rule is what keeps a pulsing source from piling on itself.

### Shields

- An enemy holds one Shield (`ShieldPool`: remaining, max, an optional damage type). A reapplication
  keeps the larger pool.
- The pool is scaled by spirit × healing received once, when cast; the cast size is not kept, so the
  tooltip's "Absorbs 160 / 160" cannot say why it isn't 100.
- A shield absorbs after armor, plating and damage taken (`DefinedEnemyMob.takeHit` after
  `HitResolution.resolve`), and damage-over-time ticks drain it too.
- Type-limited shields: Armored and Shieldbearer cast physical-only shields, Jammer and Juggernaut
  (1500) cast magic-only ones. Golem casts one that lasts until broken; Mender's Last Ward, and the
  enemy Ward Mine zone, also cast shields.
- The board shows a shield as a faint ring whose brightness follows the share left. The Info pane
  shows it only as the Shield icon's "Absorbs" row.

### What towers and effects do to heals and shields

| Thing | Today |
|---|---|
| Inversion (hex) | A heal becomes a separate damage-each-tick effect credited to the hex's tower (`invertedThrough`, `TickDamage`); a shield becomes one magic hit of its pool |
| Cinder: Cauterize | −50% healing received while burning |
| Sniper: crits pierce shields | A crit goes past the shield |
| Sonar: shield-break proc | Takes 25% of what the shield has left |
| Sonar: Ultrasound | Adds magic, up to 50% against an enemy with armor **or a Shield** |
| Seeker: Nullifier | +50% damage against a shielded enemy, and its missiles remove Shield and Heal |
| Seeker: EMP payload, Pulse: Purge | Remove Shield and Heal |
| Dead Zone / Fallout | Keep Heal and Shield out / keep Heal out |
| `AttackProfile.withIgnoredShields` | Unused: nothing calls it |

### Health display

- The board has no health bar: health shows as the enemy body's opacity.
- The Info pane draws a health bar with the bounty and lives tallies beside it.

## What this feature adds

### Spirit: recovery only

- Spirit scales **regeneration and shields, and nothing else**, through one factor
  `f = max(0, 1 + spirit/100)`: linear, with no upper limit, ×0 at −100.
- Debuffs stop depending on spirit: every timer runs one tick per tick, and Sickened and Scorched lose
  stacks at a fixed interval. "Time left" becomes true as a result.
- Healing received is gone. Cauterize lowers **spirit by 50** while the enemy burns instead, so it still
  weakens both regeneration and shields.
- Spirit's meaning text says what it now does: stronger regeneration and shields, weaker below zero.

### Heal is regeneration

- A heal adds its plain amount to Regeneration while it lasts and stops adding when it ends: a heal of
  20 gives 20 regeneration.
- Every tick restores regeneration × f, base regeneration from traits included. Fractions carry over,
  and anything past max health is lost.
- The Regeneration row shows the rate the enemy really restores. Its hover lists Base, each source,
  Together, Spirit ×f, and Now, and those numbers always match what the tick does.
- Heals stack **per source**, a source being one caster's one ability, or one zone. A recast from the
  same source refreshes its own entry (the larger amount, the later end); different sources add up.

### Inversion

- While Inverted, the enemy's whole regeneration tick (× f) lands as magic damage over time, credited
  to the hex's tower. It goes through magic resist and, like all damage over time, skips shields.
- A shield cast on an Inverted enemy still strikes it as one magic hit of its pool.

### Shields

- A shield's pool is what was cast: 100 is 100. Spirit no longer changes its size.
- Shields stack **per source**, by the same rule as heals; a recast tops its own entry up to the larger
  of what is left and the new pool, with the later end.
- Type-limited shields stay. A hit is absorbed only by the shields that take its type, the one ending
  soonest first; a shield that lasts until broken goes last.
- A hit meets shields **before** armor:
  1. dodge, as today: a dodged hit touches nothing;
  2. the crit roll and multiplier: the raw damage;
  3. each shield that takes the type absorbs raw ÷ f;
  4. what the shields could not hold goes on through armor or magic resist, plating or insulation,
     damage taken and damage-over-time taken.
  So damage-taken debuffs (Vulnerable, Brittle, …) weaken the body, not the shell. At f = 0 a hit breaks
  every shield that takes its type, and all of it goes on.
- Damage over time (burn, poison, bleed, Inversion) skips shields and goes straight to health.

### Tower interactions

- Ultrasound's "or Shield" clause is dropped: its share follows armor alone.
- Sniper's crits still pierce shields.
- Sonar's shield-break proc takes 25% of every shield.
- Nullifier keeps +50% against a shielded enemy; it counts before the shield, being part of the hit.
- Nullifier's dispel, the EMP payload and Purge remove every heal and every shield.
- Dead Zone and Fallout are unchanged.
- The unused `withIgnoredShields` goes.

### Display

- The Info pane shows a **shield strip under the health bar**: its fill is the total shield ÷ max
  health (full at most), the total is written beside it, and a type-limited share is tinted
  (physical or magic). It is absent when the enemy has no shield.
- The single Heal and Shield icons stay; each one's tooltip lists its entries (source, amount or what
  is left, type, time left).

### Out of scope

- A health bar over enemies on the board: a later feature of its own.
- Retuning content after the shift: this feature reports it; retuning is a separate task.
- Merging damage over time and heal over time into one fuel model: rejected (see Decisions).

## Interconnections

- **Uniform stat presentation** (`FEATURE-stat-presentation.md`) built the breakdown hover this
  request corrects, and `StatUniformityTest` holds every stat row to its rules, a removed stat
  included.
- **Board health bar** (not yet written) will want the same health-plus-shield reading the Info pane
  strip settles here.

## Constraints and open risks

- **Balance shifts on several axes at once:**
  - shields on armored enemies drain faster, since they now take damage before armor;
  - damage over time ignores shields;
  - debuffs no longer last longer on low-spirit enemies or wear off sooner on high-spirit ones;
  - base regeneration is now scaled by spirit;
  - a pack of healers now adds up;
  - Cauterize becomes −50 spirit, a different number for an enemy that already has spirit.
- **Content tests:** per `CLAUDE.md`, a content change adds no test, and a test that breaks on content
  is rewritten on a fixture or deleted, never edited to the new value. Spirit pacing, the restorative
  channel, shield scaling and healing received all have tests today that the rework will break.
- **Allocation:** stacking heals and shields per source puts a list where a single entry was. Per-tick
  and per-frame code stays inside the performance budget (`td.PerformanceHarness`).
- **The engine stays headless:** the per-source identity of a heal or shield has to come from the
  engine (caster, ability, zone), never from the UI.
- **The hit trace** (`HitResolution.steps`, the dev panel's enemy trace) must show the new order,
  shields included, from the same steps as the hit itself, never a second formula.
- **Text and docs that say the old rules:** Spirit's and Cauterize's sentences, Inversion's, Ultrasound's,
  the Shield and Heal tooltips, the `td/effect`, `td/stat` and `td/enemy` `CLAUDE.md` files, and the
  README's enemy and tower tables.

## Decisions made

- Spirit scales regeneration and shields only, linearly: `max(0, 1 + spirit/100)`, no upper limit.
  Diminishing and half-weight curves, and a ±100 cap, were considered and declined.
- Spirit no longer paces any debuff or stack decay.
- Healing received is removed; Cauterize becomes −50 spirit.
- A heal adds its plain amount to Regeneration; the factor applies on each regeneration tick, to base
  regeneration too.
- The Regeneration row shows the real rate, with the spirit factor as its own line in the hover.
- Heals and shields stack per source, a source being a caster's ability or a zone; a recast refreshes
  its own entry.
- Shield pools are not scaled; shields drain soonest-ending first; type-limited shields stay.
- A hit meets shields after the crit and before armor; damage-taken debuffs act on health only.
- Damage over time skips shields.
- Inversion turns all regeneration into magic damage over time; a shield cast while Inverted strikes as
  one hit.
- Ultrasound drops its shield clause; the other tower interactions keep their meaning (listed above).
- Info pane: a shield strip under the health bar.
- Damage over time and heal over time stay separate mechanics: burn and poison pools carry per-tower
  credit, tuning and stack clocks that heals do not need, and a fuel that spirit drains faster would not
  make spirit raise total healing.
- One request, landed in this order, a commit for each: spirit pacing and healing received; heal as
  regeneration with Inversion; shields; the display and texts; a `BalanceReport` comparison against a
  baseline saved before the first commit, reported, not retuned.

## Open questions

- What the Spirit line in the Regeneration hover is called when Cauterize is what lowered spirit:
  "Spirit", or the source that changed it.
- Whether the wave-preview hover (full health, no live effects) shows anything of shields an enemy will
  cast, or stays as it is.

---

*After planning and implementation, update this document rather than deleting it: mark it implemented,
prune resolved open questions, and either promote deferred scope to a new request or note it's still
wanted for a later version.*
