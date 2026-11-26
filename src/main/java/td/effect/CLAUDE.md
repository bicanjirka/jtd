# `td.effect`

- `Effect` is one effect-level record (kind, time, sink, origin) plus one `EffectDynamics` family:
  flag, encasing (a freeze and its `IceKind`), stacks, stack shares, slow, growing slow, fuel pool, heal, shield pool,
  distance or dose. A family carries
  only its own data. This package never depends on `td.enemy`: the producer binds a `DamageSink` when
  it creates the effect.
- Each `EffectKind` is one row holding its `EffectRule`: category, stacking
  (`EffectStacking`), stack cap, what each stack does to stats, how much of speed it takes away, and tags.
  Every classification method reads the rule. `ActiveEffects` keeps one live effect per kind and
  combines applications by the rule's mode, and every kind is in one of five: timed (one timer, the stronger and the
  longer kept), count, shares, pool, or dose. A kind's mode follows from how its source applies it: what a tower keeps up by
  contact is shares, what separate events build is a count held by each application, what a source builds over time
  is a count held by that source, and what has no stacks is timed. `ActiveEffects` exposes queries only, never its map.
- A kind in shares (`SHARES`) keeps one share per source on its own clock (`SourceShares`): a stat debuff's share is
  worth its source's stacks (`StackShare`) and the kind's stacks are their sum up to the cap; a heal's or a shield's
  carries its own amount. The effect's time is always its last share's, and anything that changes the time changes the
  shares. The mob keys a tower's share to the tower's `AttackOrigin`.
- A count (`COUNT`) is one shared number of stacks on the enemy, whoever adds them. An application adds its stacks
  and holds the count `Effect.HOLD_TICKS`, never shortening a hold already running; a holder (a pool, a field through
  `hold`, an aura's holder application) adds nothing but raises it to its own level and holds it while it lasts. Of
  several holders the quickest pace and the highest level count. Once nothing holds it, it loses a stack each fade
  step, the first a whole step after the hold. The fade step is a length like any other: each application carries
  its own and each tick a holder holds sets it, so tenacity reaches it only then.
- A kind has one stack cap and nothing raises it; every stack count has its bundle precomputed.
- A slow (`Effect.slowStrength()`: `CHILL`, `TARRED` and `DRAGGED`, whose strength each application brings, and
  `BLIGHT`'s `GrowingSlow`) takes a share of the speed its carrier has, gains included, and only the strongest counts,
  across kinds too. `ANCHORED` (`EffectTag.SPEED_CEILING`) is no such share: it holds speed to a share of base through
  `capAtShareOfBase`, the one answer to a speed gain. Each fades over its last `SLOW_FADE_TICKS`, or its whole length if
  shorter; one refreshed each pulse never gets there. A growing slow is the exception: it grows by the tick it has run,
  from its start, never under its floor, and ends at its strongest, so what lengthens it makes it grow further. Of one kind a weaker slow is ignored while the stronger runs, and
  a stronger one replaces it, weaker tail and all. `FREEZE` is a hard stop.
- `BURN` and `POISON` are each a fuel pool of one `FuelContribution` per source, each with its own sink
  (`EffectKind.isFuelPool()`): an effect carries the damage it will deal, and fuel from a source already feeding the
  pool joins its share. Every tick the pool burns a fixed multiple of the root of its fuel, every share by the same
  part, so four times the fuel lasts twice as long; there is no cap. Damage lands in a pulse a few times a second
  carrying what has burned, split across the shares (largest remainder), and the part of a unit left over stays
  owed, so a pool deals exactly its fuel. Pulses ride the stack clock, so `STACK_INTERVAL_TICKS` must stay a multiple
  of the pulse interval. The last part of a unit burns at once. The two kinds are separate
  pools, so they stack with each other. A pool holds its lasting mark (`SCORCHED` for
  burn, `SICKENED` for poison, `EffectInteractions.earnedBy`): it earns a stack when it starts, unless another pool
  already holds that mark, and builds another every `STACK_INTERVAL_TICKS` while it lasts; several pools holding one
  mark build it at one pace. A top-up earns none, and the mark's fade step is the pool's last application's.
- `IRRADIATED` is a dose (`EffectDynamics.Dose`): damage every tick that halves every half-life, spent once it deals
  under `Dose.SPENT_PER_TICK`, so what it deals in all is bounded. A second dose adds its rate to what is left and the
  sum halves as one; its time always follows its rate, so tenacity scales the rate, not the time. While it lasts,
  `REGENERATION` is capped at 0 (`capAtShareOfBase(0)`), heals included; a crumble below 0 stays.
- `SCORCHED` and `SICKENED` are counts: each stack lowers `RESILIENCE` or `SPIRIT` by one, and a tower can add them
  directly (`Effect.scorched`, `Effect.sickened`).
- Every timer counts down one tick per tick; nothing about the enemy paces it. Only tenacity and
  the freeze meter change how long an effect lasts, and only as it is applied.
- `MARKED` is spent by a hit, never by periodic damage: the mob calls `consume(MARKED)`, and only
  when its resilience is below 100.
- A field effect (`SILENCED`, `ANCHORED`, `DRAGGED`, `DEAD_ZONE`) is an ordinary timed effect the tower that holds
  the field refreshes every tick, so it ends a while after the enemy leaves; of two fields the stronger holds.
  `TOLL` is a count held instead (`hold`): each tick a field holds it, it builds at that field's pace up to that field's
  level and loses nothing. `DEAD_ZONE` sets `SPIRIT` to its floor, so heals,
  shields and rewinds are worth nothing and nothing is kept out or stripped; a fallout zone applies it too.
- A `HEX` kind carries no stat of its own but `BLIGHT`'s growing slow; the tower that cast it watches it and pays it out.
  `BACKLASH` is the one hex the enemy itself reads: its regeneration, and every heal and shield it is given, are dealt
  through the hex's sink.
- `SOULFIRE` is a third fuel pool beside burn and poison (`EffectKind.isBurning()` covers it and burn), and it
  earns `SCORCHED` and `SICKENED` both. It is its own kind, so it burns what is burn-immune.
- A burning pool sets `STEALTH` to 0 like a reveal, through its rule.
- `CHARGED` is spent by a hit from any attacker but the one in its `origin`, never by periodic
  damage; the mob pays its share of that hit through the charge's own sink, so the charger is
  credited.
- Effects change a mob only through `contributeTo(StatAccumulator)`, which applies each kind's
  `modifiersAt(stacks)`, a slow's hold on speed and what burning pools carry. Damage-taken
  rows **add**; revealed sets `STEALTH` to 0, which beats invisibility's 1; every stat is floored by its range. A heal adds its amount to `REGENERATION`: it is
  `SHARES`, one `HealShare` per `Effect.origin` (`Effect.from`), shares from different sources adding up, a
  recast refreshing its own share (the larger amount, the later end), each running out on its own clock. A
  shield is `SHARES` the same way, one `ShieldShare` per source, a recast topping its own share up. Its points
  are what was cast, never scaled; `ActiveEffects.absorb` lets the shares that take a hit's type hold it, the one
  ending soonest first, each worth the `worth` the mob passes (its spirit's `StatCurves.restoreFactor`), and at no
  worth every share the hit meets breaks.
  The mob applies regeneration to itself, capped at max health; `Damage` can't be negative.
- Tenacity and the freeze meter only scale an authored duration (a count's hold and fade step, a share's clock),
  never change an effect's curve, and a pool's fuel (and the fade step of what it holds) in place of its duration: `duration = authored * durationFactor(tenacity)`, and `* FreezeMeter.nextShare()` for `FREEZE`; never under a tick,
  though a freeze that would last under one doesn't land. Only an immunity keeps a kind out (`Effect.isKeptOffBy`), and a ruby freeze holds the immune all the same. `FREEZE` isn't renewed
  while it holds (`EffectTag.NOT_RENEWED`); `DAZED` refreshes like any other kind and has no meter.
- `EffectInteractions` is the one place for how an active kind acts on another: what a pool earns, and what a
  kind keeps out (a reveal keeps `INVISIBLE` out and strips nothing). Add a rule there, never a condition in
  `ActiveEffects`.
- `EffectKind.category()` groups kinds for the inspector; it never changes numbers.
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
2. A named `Effect` factory if towers or abilities apply it, and a `StackingDebuff` row if it is a count a proc adds to.
3. An `EffectTemplate` if abilities author it.
4. The cases the compiler forces (`EnemyFrameBuilder.markerPaletteFor` and `dotFor`, `Java2DFrameRenderer.colorFor`,
   `EffectSentences.of`, `ActiveEffects.magnitude`, `DevEffects.offer`).
5. An `EffectPictograms` row (else it shows its monogram).

A plain stat debuff needs no other code; a rule between kinds is a row in `EffectInteractions`.
