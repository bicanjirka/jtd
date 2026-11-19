# Feature Request: Debuff stacking groups

> **Status.** Implemented (78866e77, 585ca7f5, fa58edb4). The balance pass in
> `FEATURE-balance-after-next-gen-towers.md` retunes the numbers this moved; nothing here is deferred.
>
> One rule was added in implementation: a tower's Effect duration stat no longer reaches a count, so every
> application holds exactly 5 s whatever tower adds it. Nothing grants that stat yet; `TODO.md` asks what it is for.

## Summary

Every stacking effect behaves differently today: one loses all its stacks at once, one fades a stack at a time, one
can't stack at all, and tenacity reaches only some of them. This request sorts every effect into four stacking modes
that each behave one way, so a player (and the next person tuning a number) can predict any effect from its mode. It
also changes caps and values, adds one debuff (Singe), lengthens fuel pools and moves Flashover's threshold to match.

## Goals

1. **Stacks are lost one at a time.** In play, stacks built on high-tenacity enemies were lost all at once, because
   tenacity shortens one shared clock and the whole stack ends with it. A stack that took many hits to build should
   not vanish in one tick.
2. **Several towers building one debuff must make sense.** Two Snipers add to the same Sunder; two Mortars each add
   their own Crack. How a debuff stacks follows from its mode, and its mode from how its source applies it.
3. **Tenacity reaches every stack debuff.** Tenacity is meant to shorten everything that doesn't help the carrier. It
   currently skips Scorch and Sicken because they have no clock.
4. **Fewer special cases.** A new effect is placed in a mode by one rule and inherits that mode's behaviour; it does
   not invent a stacking behaviour.
5. **Intended balance changes.** The value, cap and pool changes below are wanted for their own sake, beyond goals
   1-4. They are not side effects to minimise.

## Current state (what exists today)

Facts only, read from the code at the time of writing.

- **Stacking modes.** A kind is one of five: refresh (one timer), pool (fuel), per-source (heal, shield: one share
  per source on its own clock, `SourceShares`), shared clock (stacks add on one clock that any application refreshes;
  at the end every stack ends together) or earned (stacks wear off one per second, `STACK_DECAY_INTERVAL_TICKS` = 20
  ticks, only while no pool or field still holds them).
- **Shared clock kinds:** Vulnerable (cap 3), Sunder (cap 10, 5 s, -5 armor a stack), Unravel (cap 5, 5 s, -10 magic
  resist a stack). The enemy buffs Spellproof (cap 200) and Resolve (cap 100) use it too; the Command affix holds
  Resolve on its allies at its own number of stacks.
- **Sources of the shared clock debuffs.**
  - Sunder: the Sniper's crit proc (2 stacks) and the Beacon's Prism (1 stack).
  - Vulnerable: only the Hexer's Agony, which adds one stack each time a hex is laid, lasting as long as that hex.
    One Hexer recasting adds stacks too, and the clock keeps the longest hex time, so Vulnerable already outlasts the
    shorter of two hexes. The 4 s row in `StackingDebuff` is used only by tests.
  - Unravel: nothing built in applies it; only the dev panel and tests do. The next feature gives it a source.
- **Earned kinds:** Scorch (cap 100, -1 resilience), Sicken (cap 100, -1 spirit), Toll (cap 10, +4% damage taken
  from every source; held by the Obelisk's fields at 5, or 10 with Deep Toll). A pool earns Scorch/Sicken when it
  starts and again every 0.5 s while it burns. Burn and Soulfire both earn Scorch, so one enemy can earn 4 Scorch a
  second; Sear adds 5 per stream per Scorcher, the Prism 5, fallout 4 Sicken.
- **Stack caps.** A kind has a cap, and `td/effect/CLAUDE.md` lets an application raise it (`Effect.withStackCap`).
  Nothing built in raises a kind's cap: Scorch and Sicken pass their kind's own 100, a Toll field holds at or below
  the kind's 10, Command holds Resolve below its 100. The `MOST_STACKS` comment in `EffectKind` names a node, Fault
  Line, that no longer exists.
- **Refresh kinds with a stat:** Crack (-5 plating, -5 insulation; 5 s from the Mortar), Deep Crack (-10 plating,
  -5 insulation, applied by a Deep Crack Mortar in place of Crack), Expose (-25 agility, -25 resilience; the Beacon's
  Glare reapplies it every tick for a 0.5 s linger), Brittle (+30% physical damage taken), Priority. They cannot
  stack; two Mortars refresh one Crack.
- **Tenacity.** Scales the duration of everything that doesn't help the carrier, once, as it is applied
  (`100/(100+t)` above 0, `1 - t/100` below, floor -100), and a pool's fuel. This is a package invariant in
  `td/effect/CLAUDE.md` ("nothing about the enemy paces it… only as it is applied") as well as the shipped rule in
  `shipped/FEATURE-effect-simplification.md`, which also says "effects with no duration have nothing to change". An
  earned stack has no duration, so tenacity never touches Scorch, Sicken or Toll except through the fuel that earns
  them. Unyielding gives its carrier +100 tenacity mid-fight.
- **Owners of effects.** The enemy owns every effect. A tower sold mid-fight leaves its effects running out their
  clocks, as heal shares do.
- **Obelisk fields do not stack per tower.** Of several fields the quickest pace and the highest cap count (Toll); the
  stronger holds (Silence, Anchor, Drag). Pools of different kinds do add: burn and Soulfire each earn Scorch.
- **Pools.** Burn, Poison and Soulfire burn a fixed multiple of the root of their fuel: 100 fuel burns out in 1.75 s,
  400 in 3.5 s. Pools have no cap. The burn bar shows a pool's burn time over twice the reference (full at 400 fuel,
  half full at 100); fuel keeps rising past a full bar. The 1.75 has no recorded reason beyond "ends cleanly" in
  `shipped/FEATURE-next-gen-cinder-tower.md`.
- **The Scorcher** deals no direct damage: while it streams (1 s, then a 2 s reload, bare), each enemy in its 60°
  cone takes Flame as fuel for its burn, every tick. At full build Flame is 120 a second (Soulfire +10%: 132), up to
  1.4 times that in the Core, with Thermal Shock 1.35 times on a held enemy. Burn pulses are magic and meet
  insulation, four times a second.
- **Flashover** (tier 3): while the Scorcher streams, an enemy in the cone whose burn holds 100 fuel or more feeds
  half the Flame to every enemy within 1.5 cells. Today 100 is a half-full bar; it fires only mid-stream in the Core
  of deep builds.
- **Defence stat floors.** Armor, magic resist, plating and insulation floor at 0; resilience, spirit, agility and
  tenacity at -100. Built-in armor reaches 110. Insulation is rare: of the 34 authored rank stat lines, 20 have none,
  9 have 2-5 and 5 have 10 or more.
- **Dead content.** Nothing built in applies Brittle, Mark, Charge or Unravel. (Mark: the next hit is a guaranteed
  crit and spends the mark. Charge: the next hit from another tower discharges it for 30% of that hit as magic,
  credited to the charging tower.)
- **Saves** hold no active effect (`td.save` reads none), so nothing here touches the save format.

## What this feature adds

Player-visible behaviour. The numbers are the requester's.

### The four modes

| Mode | Behaviour | Members |
|---|---|---|
| **Timed** | One timer, no stacks; ends at once. A source in contact (a zone, a field) renews it while the contact lasts. | Brittle, Mark, Priority, Tar, Silence, Anchor, Drag, Dead Zone, slows, stops, hexes |
| **Count** | One shared count on the enemy. While anything holds it, it loses nothing; once nothing holds it, it loses one stack per fade step. A holder is a pool, a field, an aura, or an application's own hold timer. | Scorch, Sicken, Toll, Sunder, Unravel, Spellproof, Resolve |
| **Shares** | One share per source, each on its own clock and worth its own number of stacks; the count is their sum, up to the cap. A share ends when its clock runs out, so the count falls one source at a time. | Crack, Singe, Expose, Vulnerable, Heal, Shield |
| **Pool** | Fuel that burns by the root of what is left. Unchanged apart from its length. | Burn, Poison, Soulfire |

The shared-clock mode, where every stack ends together, is gone. Deep Crack stops being a kind of its own.

**Placement rule.** How the source applies an effect places it:

- a debuff a tower keeps up by staying in contact (every shell, the light, the stream, a hex) is **Shares**, one share
  per tower;
- a debuff built by separate events (a crit, a Prism mark) is **Count**, held by its application's hold timer;
- a debuff a source builds over time (a pool, a field, an aura) is **Count**, held by that source;
- an effect with no stacks is **Timed**.

### Holds

- **One hold for every Count.** Every application of a Count debuff holds it for 5 s: a Sniper's crit, a Prism mark,
  Sear, fallout's Sicken. Each application restarts the hold. A pool, a field or an aura holds it for as long as that
  source lasts, and no hold timer follows when it ends.
- **Holders never add.** Of several holders of one Count, the quickest pace and the highest level count, as fields do
  today. Burn and Soulfire on one enemy earn Scorch at one pace together, not each at its own. Applications do add:
  every crit, Sear and Prism mark brings its stacks.
- **The fade starts one step after the hold.** The step's countdown starts the moment nothing holds the Count, so the
  first stack goes one step after the hold ends, as earned stacks do today.

### Fade and tenacity

- The fade step is one stack a second.
- Tenacity scales a stack's **fade step** like any duration: as it is applied or held, never live. A held mark is
  held every tick, so its step is set by the enemy's tenacity on the last tick something held it; a Count debuff with
  a hold timer sets it on its last application. A change of tenacity mid-fade (Unyielding, a later tenacity debuff)
  reaches only stacks applied or held after it.
- The **hold timer** of a Count debuff and the **clock of each share** are scaled by tenacity when applied, as every
  duration is today.
- Buffs (Spellproof, Resolve) help their carrier, so tenacity scales neither their hold nor their fade.
- The package invariant stays as written: nothing about the enemy paces a timer, and tenacity changes a length only
  as it is applied.

### What the inspector shows

One rule: every stacking effect shows its stacks and when it next loses one.

- **Count:** "held" with no time while a pool, a field or an aura holds it; the hold's time left while an
  application's hold does; the time to the next stack lost while it fades.
- **Shares:** the stacks, "from N towers", and when the next share ends. Heal and Shield show their sum in their own
  units (regeneration, points), as today.

### Cap rules

1. **Every cap counts stacks.** A share is worth its source's stacks.
2. **A kind has exactly one cap, and nothing raises it.** A holder holds up to its own level below that cap (a Toll
   field 5, Deep Toll 10, Command its own Resolve number). The rule that an application may raise a cap goes.
3. **Twin stats use the same numbers.** Each debuff has a partner on the matching stat:

| Pair | Per stack | Cap |
|---|---|---|
| Sunder / Unravel (armor / magic resist) | -5 | 20 |
| Crack / Singe (plating / insulation) | -5 | 10 |
| Scorch / Sicken (resilience / spirit) | -1 | 200 |
| Vulnerable / Expose (marks) | +15% damage taken / -25 agility and resilience | 3 |
| Toll | +4% damage taken | 10 |

### Value and behaviour changes

| Effect | Today | After |
|---|---|---|
| Sunder | -5 armor, cap 10, all stacks end together after 5 s | -5 armor, **cap 20**, Count, hold 5 s, then one stack a step |
| Unravel | -10 magic resist, cap 5, all at once | **-5 magic resist, cap 20**, Count, hold 5 s, then one stack a step |
| Vulnerable | cap 3, shared clock | cap 3, **Shares: one stack per Hexer with Agony, lasting exactly the hex that Hexer laid** |
| Scorch, Sicken | cap 100, a direct application starts fading at once | **cap 200**, Count; a direct application holds 5 s |
| Toll | earned, cap 10 | Count, cap 10; the Prism's Toll holds 5 s |
| Crack | -5 plating and -5 insulation, refreshes | **-5 plating only, Shares, cap 10 stacks**: a Mortar's share is 1 stack, a Deep Crack Mortar's 2, the Prism's 1 |
| Deep Crack | -10 plating and -5 insulation, its own kind | **gone as a kind**: the node makes the Mortar's shells give 2 stacks of Crack |
| Expose | -25 agility and -25 resilience, refreshes | same values, **Shares: one stack per Beacon with Glare, cap 3**, on the 0.5 s linger it has today |
| **Singe** (new) | | **-5 insulation, Shares: one stack per Scorcher, cap 10** |
| Spellproof | shared clock, 15 s, all at once | Count: hold 5 s like every Count, then one stack a step; stronger (see "Constraints"), its stack counts retuned in the balance pass |
| Resolve | shared clock, held by Command | Count, held by Command; once an ally leaves Command it loses one stack a step |

Insulation moves out of Crack and into Singe; plating stays with the Mortar. With 10 Mortars, or 5 with Deep Crack,
an enemy carries -50 plating.

A lone Hexer drops from up to 3 Vulnerable stacks to 1, and an enemy burning from both a Soulfire Scorcher and a plain
one earns Scorch at one pace instead of two. Both nerfs are accepted.

### Singe

- Applied by the Scorcher as a built-in passive: an enemy touched by its stream carries one Singe stack from that
  Scorcher, -5 insulation. The share lingers 0.5 s after the stream last touched it, like Glare's Expose. That covers
  the cone's edge as the turret turns, and gives tenacity a length to scale.
- It is the Scorcher's debuff for the player, as Crack is the Mortar's: on while the fire is on the enemy, helping the
  magic towers around it and the burn's own pulses.
- The name is Singe. "Char" was rejected as too close to Charge; "Sear" is already the passive that gives Scorch.
- **The Scorcher's upgrade tree is not touched.** Kindling and every other node stay as they are; the tree is a
  separate feature.

### Pools

- A pool of 100 fuel lasts **4 s** (was 1.75 s), 400 lasts 8 s, 1600 lasts 16 s. The square-root rule is unchanged:
  four times the fuel burns twice as long. Total damage is unchanged; per-second damage of an unfed pool falls.
- **Flashover's threshold rises from 100 to 250 fuel**, so it keeps about the share of a stream it fires today (see
  the table under "Constraints"). Its final form is left to the Scorcher's balance; `TODO.md` records the
  recommendation.

### Dead content

Brittle, Mark and Charge stay in the game, marked as dead content in `TODO.md`. Unravel is noted there too, as waiting
on the next feature for its source. A tenacity debuff idea is one line under "Feature ideas".

## Interconnections

- `FEATURE-balance-after-next-gen-towers.md` (written, not shipped): every number here moves tower value. Sunder's
  strip doubles, Unravel's doubles, Crack loses insulation, the Scorcher gains Singe, a lone Hexer's Vulnerable falls,
  pools last 2.3 times longer, and the Magma's Spell Ward grows stronger (its stack counts are enemy content to
  retune there). That pass should start after this one ships, or be told what moved.
- The Scorcher's tree feature (not yet written): this request leaves the tree alone but moves Flashover's threshold
  to 250 as a stopgap. That feature settles Flashover's final form.
- The next feature, which gives Unravel a source.
- `shipped/FEATURE-effect-simplification.md`: its tenacity rule stands. Tenacity still scales a length only as it is
  applied; it now also scales the fade step that way.

## Constraints and open risks

Facts and tensions, not solutions.

- **Determinism.** What the simulation iterates keeps a fixed order; shares must too, and the same source must always
  be first. Headless balance runs must reproduce.
- **Per-tick budget.** Tick p99 ≤ 1 ms, ≤ 512 KB allocated per frame build, with every buildable cell a tower. Today
  the stats of every stack count are precomputed so nothing allocates; caps of 200 and stacks summed from shares must
  keep that true. The harness is `td.PerformanceHarness`.
- **Stacks past the floor are time.** A Count stack beyond what takes its stat to the floor changes no stat; under a
  fade it only adds time at full strength. Scorch and Sicken at 200 can floor any enemy's resilience or spirit (both
  are 200 wide); Sunder at 20 strips 100 armor, nearly all of the highest built-in 110.
- **How strong the 5 s hold is.** Simulated for Sunder: 2 stacks per Sniper crit at even intervals, one enemy in range
  for 30 s, stacks averaged over that time. The 5 s hold matches today's 5 s clock, so while the crits keep coming it
  is about today's strength. It gains in three places: the cap of 20 when crits come fast; crits 5-7 s apart, where
  today's stack falls apart before the next crit; and the tail after the last crit.

  | Crit every | Today (cap 10) | 5 s hold, fade (cap 20) | 3 s hold, fade |
  |---|---|---|---|
  | 2.5 s | 7.9 | 11.7 | 11.7 |
  | 4 s | 6.7 | 7.5 | 6.7 |
  | 5 s | 5.8 | 6.0 | 3.7 |
  | 6 s | 1.5 | 5.0 | 1.4 |
  | 8 s | 1.1 | 1.4 | 1.0 |
  | 10 s | 1.0 | 1.2 | 0.9 |

  A shorter hold, such as 3 s, would make Sunder weaker than today between crits 5 s apart; 5 s is the shortest hold
  that never loses to today. After the last crit, N stacks last 5N stack-seconds today and 5N + N(N+1)/2 under hold and
  fade: 2.1 times at 10 stacks, and at 20 stacks 310 against today's capped 50. That tail is what the towers after
  the Sniper meet.
- **High tenacity still breaks a slow stack.** At tenacity 100 (hold 2.5 s, step 0.5 s), crits 4 s apart average 1.5
  stacks against today's 1.2: the stack now drops a stack at a time instead of at once, but tenacity shortens the
  whole of it, as it should. Crits 1.5 s apart average 15 against 8.7.
- **Spellproof under the 5 s hold.** The Magma's Spell Ward gives 30 or 50 stacks for 15 s today. Counted in
  stack-seconds, 30 stacks become 615 against 450 (1.4 times, lasting 35 s) and 50 become 1525 against 750 (2 times,
  lasting 55 s). No hold keeps today's strength above 30 stacks, because the fade alone outlasts 15 s.
- **Slow fade at cap 200.** Full Scorch takes several Scorchers about 30 s to build and 200 s to fade. Enemies don't
  live that long, so the fade's length is rarely seen.
- **Pools at 4 s ripple.**
  - A continuously fed pool holds about 5.2 times as much unburned fuel at its balance point.
  - The fuel still in an enemy when it dies is lost, and that reservoir is 5.2 times larger, so the Scorcher needs
    more fuel per kill.
  - A pool earns about 2.3 times as many Scorch/Sicken stacks per application, holds them about 2.3 times as long,
    and reveals its carrier for 2.3 times as long.
  - Damage arrives later, so more is lost to enemies that leak, regenerate or are healed meanwhile.
  - A bare Scorcher's 60-fuel stream burns for about 3.1 s against a 3 s cycle, so a pool is nearly always alive.
  - Flashover: the share of stream time an enemy flashes over, simulated on one enemy standing in the cone with
    steady cycles (ignoring tenacity, enemies leaving the cone and the fuel Flashover passes on):

    | Build | Today (100) | 4 s at 100 | 4 s at 250 | 4 s at 400 (full bar) |
    |---|---|---|---|---|
    | 120 Flame, 1.5 s stream | 0% | 66% | 0% | 0% |
    | same, in the Core | 19% | 100% | 25% | 0% |
    | Core + Long Stream II + Bellows | 52% | 100% | 100% | 62% |
    | two such Scorchers on one enemy | 85% | 100% | 100% | 100% |

- **Inspector and text.** Count effects have a hold phase and a fade phase; Shares effects have several clocks. Agony's
  text ("while its hex lasts") becomes literally true; Deep Crack's text becomes two stacks of Crack. UI text for
  them must be built the way the panel builds it, not through a convenient factory.
- **Tests.** Content is not tested; a mechanic is. Today's tests that assert Sunder, Crack, Deep Crack or burn numbers
  must be rewritten on fixtures, never re-pointed at the new numbers.

## Decisions made

- Four modes (Timed, Count, Shares, Pool) and the placement rule, with the members in the table. Shared clock goes.
- Spellproof and Resolve move to Count; tenacity does not touch them.
- Every Count shares one hold (5 s, every application, restarted by each) and one fade step (1 s); a pool, field or
  aura holds for as long as it lasts. Holders never add; applications do. The fade starts one step after the hold.
- The inspector shows every stacking effect's stacks and when it next loses one.
- Vulnerable moves to Shares, per Hexer, lasting the hex; the lone-Hexer nerf is accepted.
- The tenacity invariant is kept: the fade step is scaled as a stack is applied or held, never live.
- Cap rules: caps count stacks, one cap per kind that nothing raises, twin stats share numbers.
- Sunder and Unravel -5 a stack, cap 20. Crack and Singe -5 a stack, cap 10. Scorch and Sicken cap 200. Expose and
  Vulnerable cap 3.
- Deep Crack is two stacks of Crack from one Mortar, not its own kind.
- Singe is its name; per Scorcher, -5 insulation, cap 10, 0.5 s linger after the stream's last touch; a built-in
  passive; the tree is untouched.
- Pools: 100 fuel lasts 4 s. Flashover's threshold rises to 250 in this request; its final form belongs to the
  Scorcher's balance.
- Brittle, Mark and Charge are kept and marked dead; Unravel waits on the next feature.
- The value and cap changes are intended (goal 5). No balance report is run for this request; the balance pass comes
  after.

## Open questions

None. The balance pass after this request retunes the numbers it moves, the Magma's Spell Ward among them.

---

*After planning and implementation, update this document rather than deleting it: mark it implemented, prune resolved
open questions, and promote deferred scope to a new request or note it is still wanted.*
