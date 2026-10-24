# Feature Request: Observe Enemy

**Status: requested.**

## Summary

A dev-panel switch that makes one enemy report, tick by tick, everything that happens to it -
every hit, effect, heal, shield, ability and how it ended - to the game's log. Tests prove each
rule alone; this shows how the rules combine on a real enemy in a real run, so a surprise seen in
play ("why did that survive?", "did the freeze land or diminish?") can be read off instead of
guessed, then pinned down by a test.

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
- Periodic damage reaches the mob through `takeHit` like a hit, with a periodic `AttackProfile`
  (`AbstractTower.dealPeriodicDamage`), so it never crits or spends a mark. An `AttackProfile`
  carries the attacker's `origin()`.
- A hit carries no game time: `doTick` stamps pending moments on the next tick
  (`MobMoments.capturePending`) because towers hit after the mob's own tick ran.
- The dev panel (`PanelDev`, ctrl+shift+d) has a "Selected" section; with an enemy selected it
  offers Apply effect, Clear, Kill, Heal, Remove. Its actions go through `DevControls`, which
  queues them to run on the game-loop thread (`onSelectedEnemy`). The Spawn box takes the same
  wave-script grammar the levels use.
- Logging is SLF4J + Logback (`src/main/resources/logback.xml`): root at INFO, to the console and
  to an async `logs/jTD-<timestamp>.log`. The async appender never blocks and drops events once
  its 2048-entry queue is full, so it does not stall the game loop.
- Performance budget: tick p99 ≤ 1 ms with every buildable cell a tower (`td.PerformanceHarness`).

## What this feature adds

- A dev-panel control that starts and stops observing the selected enemy. Only the dev panel can
  turn it on: no wave script, level or saved game can make an enemy observed.
- While observed, the enemy writes one log line per event, each with the tick, the enemy's
  identity (name, rank, a per-mob id so two of the same kind can be told apart) and the numbers
  that explain the outcome. Events, at least:
  - **hits**: what arrived, from whom, its type; crit or not; dodged; mark spent; what the
    shield absorbed; what landed; health before and after; overkill;
  - **periodic damage**: the same, marked as periodic, with the effect it came from where known;
  - **effects**: requested, then what happened to it - landed (at what strength and length),
    shortened by resistance, shortened by diminishing returns, blocked, kept out by another
    effect, refreshed, stacked, turned into damage by Inversion; and later expired, dispelled,
    consumed or stacks lost;
  - **health and shields**: regeneration and heals that changed health, shield gained, broken
    or absorbed;
  - **abilities**: what it cast or spawned, and what it did to others;
  - **its end**: killed (bounty and score paid), leaked (lives lost), hatched, or removed.
- An enemy that isn't observed costs nothing measurable; the performance budget still holds.
- Out of scope for v1: showing the trace on screen, filtering events by kind, recording a trace in
  headless balance runs, and keeping the observed mark through a save and load.

## Interconnections

- `TODO.md`'s "What the balance tools cannot see yet" lists measures the recorder doesn't take
  (damage wasted on overkill and shields). This trace shows them for one enemy live, but does not
  close that gap: the balance tools need them in aggregate, headless.
- The `run-jtd` skill drives the live game. A trace in the log gives it something to read besides
  screenshots when checking an enemy rule live.

## Constraints and open risks

- The engine is headless and the mob is owned by the game-loop thread; the dev panel lives on the
  EDT. Turning observation on or off crosses that boundary.
- Hits arrive without a game time, but each line must carry its tick.
- A busy enemy under several towers can produce many lines per tick; the async log drops events
  rather than block once its queue fills, so a trace can lose lines under load.
- What the enemy does *to others* (an aura, a heal on an ally, a spawn) is partly owned by the
  ability system and the receiving mob, not the caster.
- Event names and numbers in the log are dev text, not game vocabulary; whether they should use
  `td.term` wording is open (below).
- The design must not let a level or wave script reach it, and must keep the "Test mechanics,
  never content" rule: the tests prove that observed events are reported, not what any built-in
  enemy logs.

## Decisions made

- Name: **Observe enemy**.
- Dev panel only; never placeable in a level's waves, never saved.
- Output goes to the existing log, not to a new file or window.
- It complements tests, never replaces them: a finding from a trace becomes a test.

## Open questions

1. **How is an enemy chosen?** (a) Only the selected enemy, through a toggle in the Selected
   section, observed from that moment on; or (b) also an "observe" checkbox by the Spawn box, so
   the enemies it spawns are observed from their first tick (catching their spawn and early hits).
2. **One at a time or several?** (a) Selecting and observing another enemy stops the first; or
   (b) any number may be observed at once, each line telling them apart by its id.
3. **Does the panel show that an enemy is observed?** (a) Only the toggle's state in the Selected
   section; or (b) also a marker on the board over every observed enemy.
4. **Wording:** (a) plain dev text with raw numbers (damage units, ticks); or (b) the game's terms
   and points, as the Info pane shows them.
