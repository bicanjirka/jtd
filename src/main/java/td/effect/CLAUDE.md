# `td.effect`

- `Effect` is one effect-level record (kind, time, sink, origin) plus one `EffectDynamics` family:
  flag, stacks, chill level, fuel pool, heal, tick damage, shield pool or distance. A family carries
  only its own data. This package never depends on `td.enemy`: the producer binds a `DamageSink` when
  it creates the effect.
- Each `EffectKind` is one row holding its `EffectRule`: category, resistance, stacking
  (`EffectStacking`), stack cap, pacing (`EffectPacing`), what each stack does to stats, and tags.
  Every classification method reads the rule. `ActiveEffects` keeps one live effect per kind and
  combines applications by the rule's stacking: a refresh keeps the stronger and the longer, a level
  or a pool adds up, stacks add to their cap. `ActiveEffects` exposes queries only, never its map.
- `CHILL` is a level (the share of speed lost, up to a cap) made of one `FuelContribution` per
  application, each fading linearly on its own slope. An application only adds what fits under the
  cap. It also cuts a burn's damage, most at the cap. `FREEZE` is a hard stop.
- `BURN` and `POISON` are each a decaying fuel pool of one `FuelContribution` per application, each
  with its own sink (`EffectKind.isFuelPool()`). Tick damage is apportioned across contributors
  (largest remainder), so every tower gets credit for its share. Damage lands in a pulse a few times
  a second carrying the whole window's worth; the pool still decays every tick, and pulses ride the
  stack clock, so `STACK_INTERVAL_TICKS` must stay a multiple of the pulse interval. A top-up adds
  `L0 * (1 - fuel / lmax)`. A pool ends when a tick would round to zero. The two kinds are separate
  pools, so they stack with each other. A pool earns a stack of its lasting mark (`SCORCHED` for
  burn, `SICKENED` for poison, `EffectInteractions.earnedBy`) when it starts and another every
  `STACK_INTERVAL_TICKS` while it lasts, up to their cap; a top-up earns none.
- `SCORCHED` and `SICKENED` are real effects that only `ActiveEffects` creates. Each stack lowers
  `RESILIENCE` or `SPIRIT` by one. They outlast the pool, are never cleared by another effect
  (a freeze puts out the burn, not its mark) and, once no active pool earns them, lose a stack every
  `STACK_DECAY_INTERVAL_TICKS` scaled by the enemy's spirit factor (`tick(spiritFactor)`; `0` at
  spirit -100 means never).
- A kind with `EffectKind.maxStacks()` above 0 (`VULNERABLE`, `SUNDERED`, `RESONATING`,
  `FRACTURED`, `SATURATED`) stacks on the enemy, whichever tower applied them, on one shared clock
  that any application refreshes; a full stack only refreshes. An application may raise the cap
  (`Effect.withStackCap`), and the stack keeps the highest cap any application brought. `FRACTURED` has no clock: it loses a stack at
  a time like `SCORCHED`, at the spirit pace floored at `MIN_DEBUFF_PACE`. A `faultLine` Fractured
  (a higher cap) loses none while the enemy is exposed (or revealed); one such
  application makes the whole effect so until it wears off.
- A kind with `isPacedBySpirit()` counts its timer down at `max(MIN_DEBUFF_PACE, spiritFactor)`, and not at all at spirit -100;
  freeze, invisible, shield, heal and the pools keep their own pace.
- `MARKED` is spent by a hit, never by periodic damage: the mob calls `consume(MARKED)`, and only
  when its resilience is below 100.
- A field effect (`SILENCED`, `ANCHORED`, `CORRODED`, `DRAGGED`, `UNDERTOW`, `DEAD_ZONE`, `FRAIL`) is an
  ordinary timed effect the tower that holds the field refreshes every tick, so it ends a while after
  the enemy leaves, paced by spirit like any debuff. A field effect with a number holds it in stacks:
  `CORRODED` and `FRAIL` in `FieldStrength`, `DRAGGED` in points of speed (`DragStrength`, capped at
  `MOST`). A field tops the stacks up to the strength it gives, so of two fields the stronger holds,
  and lowers (`loseStacks`) only what it gave itself. `TOLL` is a stack debuff on one shared clock
  (every stack goes when it runs out), never paced itself. `UNVEILED` keeps `INVISIBLE` out.
  `DEAD_ZONE` keeps `HEAL` and `SHIELD` out, and `FALLOUT` (a ground zone's) only `HEAL`, through
  `EffectInteractions`' held-off table, which unlike a block strips nothing already there. `BRITTLE` counts
  only while `FREEZE` is active (`EffectInteractions`' requires table).
- A pool earns `SICKENED` itself, but a tower can also add it (`Effect.sickened`): the stacks add up
  to the whole of spirit, and wear off one at a time at the enemy's spirit pace.
- A `HEX` kind carries no stat of its own; the tower that cast it watches it and pays it out.
  `INVERSION` is the one hex the enemy itself reads: `invertedThrough(sink)` turns a heal into
  damage per tick, and `INVISIBLE` is an `EffectInteractions` row it blocks and removes.
- `SOULFIRE` is a third fuel pool beside burn and poison (`EffectKind.isBurning()` covers it and burn): a
  freeze puts it out and lands part of it, a chill cools it, and it earns `SCORCHED` and `SICKENED` both.
  Nothing resists it, so it burns what is burn-immune.
- A tower tunes the pool it feeds through `PoolTuning`, carried by the pool: how much it holds, how fast it
  marks, how much of it a freeze lands, and the stat modifiers it carries while it burns (Cauterize, Heat
  and the magic mark are such modifiers). Two towers on one pool leave it the stronger of each setting,
  and what it carries only lasts while the pool does.
- A freeze always lands part of the burn it puts out at once (`burstPool`, its remaining pool
  `fuel / (1 - alpha)`); a burn pool above `BURN_REVEAL_FUEL` sets `STEALTH` to 0 like a reveal, without
  exposing the enemy. What `TARRED` does to a burn it catches and a freeze it suffers is read at `apply`
  from `EffectInteractions`' tables.
- Two hexes bend fire and ice, each a row in one of `EffectInteractions`' tables keyed only on the
  kind being active: what `RIME` does to a freeze and `ASH` to pools and chills (the pool clock still counts
  down by one, since pulses ride it). An `UNDERTOW` (a flag beside the field's drag) counts as part of
  a chill for a freeze. `ASH` keeping out `FREEZE`, and `RIME`/`ASH` replacing each other, are
  `EffectInteractions` rows too.
- What a curse carries on from a dead enemy is `EffectKind.spreadsWithCurse()`: the debuffs that
  weaken an enemy, copied with their own sink and time left, never the marks a tower leaves for
  itself.
- `CHARGED` is spent by a hit from any attacker but the one in its `origin`, never by periodic
  damage; the mob pays its share of that hit through the charge's own sink, so the charger is
  credited.
- Effects change a mob only through `contributeTo(StatAccumulator)`, which applies each kind's
  `modifiersAt(stacks)`, the chill level, the pools' rules and what burning pools carry. Damage-taken
  rows **add**; revealed sets `STEALTH` to 0, which beats invisibility's 1; exposed or revealed take 50
  `RESILIENCE` once; every stat is floored by its range. Heal goes in as restorative, so spirit and `HEALING_RECEIVED` scale it. A
  shield is an absorb pool (`ActiveEffects.absorb`), applied after armor and plating, scaled once when
  applied by spirit and `HEALING_RECEIVED`; reapplying keeps the larger pool. Cauterize lowers
  `HEALING_RECEIVED`.
  The mob applies regeneration to itself, capped at max health; `Damage` can't be negative.
- Resistance and diminishing returns only shorten an authored duration, never change an effect's
  curve: `duration = authored * (1 - resist[kind]) * freezeStep`, and under one tick is blocked.
  `EffectKind.resistedBy()` names the resisting stat. Every kind in `EffectCategory.HARD_CC`
  diminishes, on every enemy, each on its own ladder (`DiminishingReturns.of(kind)`, per mob, only
  a fresh application climbs a step): Dazed
  stops an enemy like a freeze but has no `EffectInteractions` rows: it keeps burn and chill.
- `EffectInteractions` is the one table of how an active kind acts on another: what it keeps out (a
  frozen enemy cannot burn) and what applying it consumes (freezing removes a burn, and a chill,
  which also makes the freeze last longer by the chill's level; shrouding removes a reveal). Add a rule there, never a
  condition in `ActiveEffects`.
- `EffectKind.category()` groups kinds for diminishing returns and the inspector; it never changes
  numbers.
- `EffectTemplate` (sealed) is the authored form an ability carries. Only kinds an enemy ability
  applies have one; the rest are applied by towers.
- `ShieldTemplate` can be restricted to one `DamageType` (`physicalOnly`/`magicOnly`).
- `EffectTransitions` records when each kind was last gained or lost; the UI derives
  transitions from it.
- Before an effect deals through its sink, `ActiveEffects` tells the `PeriodicDamageListener` it was
  given which kind deals, so a trace can name the effect behind a periodic hit; a new sink call does too.

The per-tick order around `ActiveEffects.tick()` belongs to `DefinedEnemyMob.doTick`; see
`td/enemy/CLAUDE.md`.

## Adding a kind

1. An `EffectKind` row with its title and `EffectRule` (a kind that helps its carrier is `.helpsCarrier()`), and its sentence in `EffectSentences`, built from the rule and constants, never typed numbers.
2. A named `Effect` factory if towers or abilities apply it.
3. An `EffectTemplate` if abilities author it.
4. The cases the compiler forces (`EnemyFrameBuilder.markerPaletteFor`, `Java2DFrameRenderer.colorFor`,
   `EffectSentences.of`, `ActiveEffects.magnitude`, `DevEffects.offer`).
5. An `EffectPictograms` row (else it shows its monogram).

A plain stat debuff needs no other code; a rule between kinds is a row in `EffectInteractions`.
