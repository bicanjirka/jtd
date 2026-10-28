# 0019. An EDT command to the engine is a request the next tick applies

- Status: Accepted, not yet implemented
- Date: 2026-10-28

## Context
`GameEngine` keeps its round state (wave index, ready, outcome, leaked rounds, leaks at round
start) in several separate `volatile` fields. The game loop writes them, and so do the dev panel's
jump-to-wave and skip-wave commands, directly from the EDT. The EDT also reads three of them one by
one to draw the header. That breaks 0007 twice: correlated fields cross separately, and two
threads write them. A known bug (pausing with dev-spawned enemies starts the next wave) lives in
this state.

## Decision
- The round state becomes one immutable value with pure transitions (started, cleared, lost,
  jumped to a wave), published behind one `volatile`.
- A command from the EDT that changes game state is a request the next tick consumes, as
  `requestNextWave` already is. It never writes engine state.
- Implemented as the first phase of the pause-bug fix.

## Consequences
- Round rules become testable on plain values, without an engine.
- A debug command takes effect on the next tick, not instantly. That's invisible at tick rate, and
  it works while paused, because the dev panel's queued changes already run then.

## Rejected
- *Stopping the loop for debug commands* (`stopLoopThen`): a visible stall for a dev convenience.
- *Leaving the writes as they are*: torn reads and a write race.
