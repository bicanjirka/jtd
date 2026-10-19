# Feature Request: The Obelisk as a Kill Box

**Status: planned** (2026-10-19). Supersedes the tree of `FEATURE-obelisk-support-role.md`, whose open questions 24
to 30 this answers.

## Summary

The Obelisk owns a stretch of path: it holds enemies there longer and weakens them while they are held, so every tower
built around it gets more shots and each lands harder. The Beacon makes towers stronger wherever the enemy is, and the
Radar marks one target; the Obelisk makes a *place* deadly. Its tree is rebuilt around that: one head holds, one head
denies, an extra line bites, and three specials reach past the field.

## Current state (what exists today)

- `PulseTower`: 25 credits, a 1.5-cell field, 1 magic damage a tick to everyone inside, hidden enemies included;
  Interference takes 25% off its damage for every other overlapping field, down to 25%. Attuned, an enemy earns a Toll
  stack a second inside, up to five; Toll is the strength of every numeric field effect (half on entry, full at five).
  The field's rules linger 2 s after the enemy leaves.
- Its tree: heads Resonant (Coils, Coils II's Vulnerable, Arc Discharge, Meltdown / Tesla Coil) and Phase (reveal and
  Daze on entry, longer reveal, Null Field, True Sight / Dead Zone); extra line Corrosion, Mirror Field, Undertow, Event
  Horizon; specials Rattle Field, Soul Drain, Kill Zone (4x the list price). Both head-III nodes need 30 s at full Toll.
- The review of that tree (`FEATURE-obelisk-support-role.md`, "Review of the tree") found the effects too weak for their
  price: they hold only inside the field and 2 s after, about 45% of a nearby Sniper's shots, so Kill Zone buys about
  +13% for 170 credits where a 20-credit Beacon gives +11% to +38%. Mirror Field and Rattle Field are dead nodes;
  Resonant scales nothing of its own; Kill Zone is the best special for both heads.
- Nothing in the game moves an enemy back along the path, and nothing links two towers on the board.
- The lab prices a support node against one partner tower; the competent player counts that worth.
- Dazed has its own diminishing returns (each daze within 10 s lasts 20% less; a sixth doesn't take hold) and stops a
  freeze-immune Yeti. Undertow counts as a chill for a freeze; Anchored holds speed to 75% of base.

## What this feature adds

**Base (25 credits).** The field keeps 1 magic damage a tick and Interference, and drags: a flat 15% slow inside, its
own effect (not a chill, so chill resistance doesn't blank it and it doesn't feed freezes). Two fields' drag refreshes,
never adds. Toll does not scale the base drag. Any field's total slow is capped at 50%.

**Toll.** Attune as today: a stack a second inside, up to five; every numeric effect a node adds is half strength on
entry and full at five, past full when Toll stacks higher. On/off rules (silence, no heals, reveal) hold from the first
tick.

**Head Gravity: time held.**

| Tier | Node | What it does |
|---|---|---|
| I | Undertow | drag up to 30% at full Toll, Anchored; counts as a chill for a freeze |
| II | Snare | a visit's first Toll stack Dazes for 0.5 s |
| III | Event Horizon | each death inside widens the field by 10% of its range until the wave ends, up to twice its range |
| IV-A | Gravity Well | the drag deepens toward the field's centre, up to 50% at the middle |
| IV-B | Stasis | at full Toll the enemy is Dazed for 1 s and Stilled: while the Daze holds, Stilled eats 1 Toll every 250 ms |

Stasis has no once-per-visit rule: the Daze's length (its diminishing returns, anything that lengthens it) decides how
much Toll is eaten, and the enemy must rebuild Toll before the next one, with every Toll-scaled effect on it weaker
meanwhile. A Daze that doesn't take hold eats nothing; Stasis tries again a second later.

**Head Ward: nothing hides, casts or mends inside.**

| Tier | Node | What it does |
|---|---|---|
| I | Sight | revealed while inside and 0.5 s after |
| II | Null Field | silenced |
| III | Dead Zone | no heals or shields take hold |
| IV-A | True Sight | the reveal lasts 4 s after leaving, and a revealed enemy can't be shrouded or fade |
| IV-B | Purge | an enemy loses its buffs and shields (Sprinting, shields) once, as it enters the field or steps onto a Ley Line; stealth is True Sight's |

The Ward is a local detector; the Radar's Wide Band stays the wide one.

**Extra line, bite (any head).**

| Tier | Node | What it does |
|---|---|---|
| I | Frailty | the enemy is Frail, +damage taken from every source: +10% on entry, +20% at full Toll (named apart from the Vulnerable other towers apply) |
| II | Corrosion | -armor and -magic resist, up to -30 each at full Toll |
| III | Resonant Coils | Toll builds twice as fast |
| IV | Meltdown | Toll cap 10: effects up to 1.5x (the 50% slow cap still holds) |

**Specials (4x the list price, 100, as on every tower).**

- **Arc Discharge (anti-boss).** Once a second a zap strikes the visible enemy of the highest rank inside, the
  furthest along of those: it becomes every tower's Priority and gains a stack of Condemned (+5% damage taken from every
  source, up to 4 stacks, +20%), both held 3 s after the last zap and paced by spirit. No chain, no damage. Exposed was
  dropped: the Armored and Frenzied bosses are crit-immune, and a crit mark did nothing to them.
- **Soul Drain.** Drains 15 spirit a second inside, so every tower's debuffs last longer and heals land weaker for the
  rest of the walk. The field's own lingering effects are paced by spirit like any debuff; no floor rule.
- **Ley Line.** The Obelisk casts one beam to another Obelisk within 5 cells (any Obelisk may host, and a host may hold
  several lines). An enemy on a beam gets both ends' field rules, as if inside, at the Toll it already carries; a beam
  builds no Toll and deals no field damage. The caster picks its host by the most path crossings, then the most path
  covered, then the shorter beam. The player may pin a line by hand; a pinned host that is sold returns the caster to
  auto-pick. Lines don't count toward Interference.

**Gone:** Kill Zone (Frailty is the bite line's), Mirror Field and Rattle Field (dead), Tesla Coil's chain, Coils
II's Vulnerable at full Toll, Phase Field I and II's range, and the Resonant and Phase heads as they are.

**Gates.** Both head-III nodes (Event Horizon, Dead Zone) keep the 30 s at full Toll deed.

**The board.** The field is tinted by head: grey plain, deep blue Gravity (Gravity Well darker toward the centre),
violet Ward; owning the bite line adds a red rim. A Ley Line is a thin beam in the caster's tint, brighter while an
enemy is on it, dashed when pinned. Pinning: select the caster, press a "Pin line" toggle, click a host; press it again
to unpin. Stilled shows as a marker on the enemy.

**The tools.** The lab prices each Obelisk node with a Sniper, a Burst and a Mortar around the field as players build
it, and Ley Line with a plain host Obelisk 4 cells away across the path; the competent player counts that worth.

**Out of scope:** the levels' retune (`TODO.md`, "The built-in levels are not tuned to the support Obelisk"), other
towers' trees, new enemies.

## Interconnections

- **`FEATURE-obelisk-support-role.md`**: the measurements, the Elite Ghost shroud rule (cast every 3 s, lasts 3 s) and the
  bought-reveal principle stand; its Design section and questions 24 to 30 are replaced by this.
- **The Radar**: Wide Band stays the wide reveal; the Ward is local. Arc Discharge's Condemned is a stat mark, not the
  Radar's crit mark.
- **The Beacon**: still raises the field's damage and range like any tower's; the done criteria cap the Obelisk's
  support at the Beacon's.
- **The Hive**: Undertow still counts as a chill, so Gravity and freezes combine; Stasis uses Daze, not freeze.

## Constraints and open risks

- **Hold stacks with other slows.** The 50% cap is for the field's slows; tar, chill and Gravity together may still
  pin an enemy, and the report should show whether they do.
- **Ley Line along a path.** A beam that runs along the path is a box up to 5 cells long, and with both ends' rules it is
  the strongest Gravity build; the criteria below judge it.
- **Two Dazes, one clock.** Snare and Stasis share Dazed's diminishing returns, so a long hold teaches the enemy to
  shrug them off.
- **Pinning is new input.** It must go through the engine's input semantics, not `TowerDefense`, and every control is a
  `HudToggleButton`.
- **Per-tick cost.** Beams, Gravity Well's centre distance and the auto-pick must stay inside the performance budget.
- **Player-visible text.** The README tower row and the Effects table (new: drag, Stilled; gone: Kill Zone), every node's
  words, and the panel's behaviour lines.

## Decisions made

- The Obelisk is a kill box: it holds and weakens enemies in its stretch of path.
- Heads: Gravity (time held) vs Ward (counter-play). Detection costs the Gravity head. Bite is the extra line, open to
  either head.
- Every earlier ✅ in `FEATURE-obelisk-support-role.md` was open to rethinking; what this doc doesn't restate is gone.
- Specials: Arc Discharge, Soul Drain, Ley Line, at the usual 4x (100).
- **Done when**, measured with `--seeds 12 --strong --variety` and `BuildHarness`:
  - an all-Obelisk army holds under 100% on every level; banning it costs x0.75 to x0.95;
  - the lab shows support from its nodes, and the competent player builds and upgrades it;
  - Elite Ghost rounds need a revealer (Ward or Wide Band) and damage;
  - at equal budget the best Gravity build and the best Ward build land within 20% of each other, each winning somewhere;
  - every special is bought in some play, and none in over 70% of the plays that buy a special;
  - no Obelisk build lifts the towers around it more than an equal-cost Beacon at its best (+38%).
- Sequencing, each step committed and compared with the one before (`--save` / `--compare`): the lab's box of three
  partners and a saved baseline; the base drag and the reshaped tree with its prices; Ley Line's auto-pick and beam;
  pinning and the head tints; tuning to the criteria and one live `run-jtd` check.

## Open questions

None. Numbers are starting points the report tunes.

---

*After planning and implementation, update this document rather than deleting it: mark it implemented, prune
resolved open questions, and either promote deferred scope to a new request or note it is still wanted for a
later version.*
