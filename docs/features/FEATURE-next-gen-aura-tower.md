# Feature Request: Next-gen Beacon Tower

**Status:** implemented 2026-11-16 (drafted 2026-11-13, third round, reviewed with the whole bundle). Tiers 0 to III and the look, written to the node rules in
`FEATURE-next-gen-upgrade-tiers.md`. Part of `FEATURE-next-gen.md`. Weighed on paper (`trees/aura.py`); the numbers below are
that model's, and the XP ones rest on placeholder gates.

## Summary

The Beacon (`AuraTower`) stops buffing numbers. Today it multiplies its neighbours' damage and range: every other
tower's balance has to allow for it, several stack, and it pays most for more copies in its circle. Next-gen it gives
no Damage, Range, Fire rate or Crit. It gives what a light gives: nothing hides in it, nothing jams under it. Above
that, a Tier I signature points the light at one enemy for every tower to turn on (the Spotlight), a Tier I pick
decides how the towers in the light learn (Lessons), and a Tier III payoff makes the light carry the mark of every
kind of tower it lights (Prism), so a mixed cluster pays in the fight as well as in XP.

It is the only tower that answers what enemies do to towers (jam and blinding) and to each other (auras), and with
the Radar gone it holds the game's reveal.

## What this feature adds

The tables below are read by `td.ui.PreviewUpgrades` (run-jtd), which draws the tree beside today's panel and lists
what the nodes no row replaces leave unused. The column rules are in run-jtd's `SKILL.md`.

### How the tree builds

- **Light is the Beacon's passive and its whole bare tower.** It never attacks, so the light is what it does, as a
  shot is the Sniper's. One circle: every tower whose centre is in it is Lit and takes two thirds of the jam and of
  the blinding; every enemy in it is Revealed. One circle for both means the Beacon stands where the towers meet the
  path, which they mostly do anyway.
- **No stat buffs, no range nodes.** Nothing it gives is a share of a tower's damage, range or fire rate, and the
  light's 2.5 cells never grow: what it lights is decided where it is built. What it gives is a counter (reveal, jam,
  auras, dodge), a direction (Priority on one enemy) or time (XP).
- **Levers** are plain stats: how much jam it takes, how long a Reveal lingers.
- **The Spotlight is Tier I's signature**: the lantern's beam holds one enemy in the light, which has Priority while it
  is held, so every tower that picks one target turns to it. Which one is the pick: Searchlight (the biggest),
  Last Light (the leaker) or Ringleader (the one that leads the others).
- **Lessons are Tier I's other pick**, how the light teaches: Kinship (a mixed cluster), Mentor (a newcomer among
  veterans) or Breakthrough (towers pushing their tiers). Kinship is one of the three ideas the bundle names as this
  game's own; it stays the Beacon's.
- **The light's edge is Tier II's pick**: Daylight (an enemy in the light gives its allies no aura) or Glare (an enemy
  in the light is Exposed: it dodges less and is crit more). Each is a counter; the pick switches between waves, so
  the wave preview decides it.
- **Prism is Tier III's payoff**: an enemy in the light carries one stack of the mark of each kind of tower the light
  holds. A stack from the light is an ordinary stack of that debuff and keeps its own rules.
- **Reveal comes only from the light's touch**: inside the circle and a moment after, longer with Afterglow. A Reveal
  on every Lit tower's hit was weighed and dropped: it reached wherever a Lit tower fires, and a Lit Burst would have
  revealed nearly everything. The Mortar's Flare reveals one rare spot; a bare Beacon reveals about as much path as
  one flare (Decisions made).
- **Its names**: Light, Lit, Reveal, Jam, Spotlight, Priority, Searchlight, Last Light, Ringleader, Kinship, Mentor,
  Breakthrough, Daylight, Glare, Expose, Prism. Sixteen, the most of any next-gen tree, though eight are members of
  three picks and Prism adds none of its own. Its enemy debuffs are Reveal, Priority and Glare's Expose.
- **The Beacon's balance is its own.** It makes every other tower better, but no other tower's numbers ever allow
  for it: each tower is weighed alone, with only what is its own. Whatever the Beacon adds, and any combination of
  it with another tower that runs away, is tuned in the Beacon's nodes (Constraints and open risks).

- **On paper** (`tree_calc.py`, `trees/aura.py`). The Beacon deals no damage, so it is worth what the towers it
  lights gain: four reference towers (the Sniper's median build) at the Beacon's own tier. Each mechanic is a share of
  what those four deal, so four times it reads against one more copy of the reference: +25% matches a copy. The
  Beacon's counters have scenarios of their own, read like the defences: `jammed` (a jammer near the cluster half the
  wave), `blinded` (a blinding field half the wave), `ghosts`, `commanded` (Resolve on most of a crowd), `elusive`.

  | Mechanic | No defence | Its counter |
  |---|---|---|
  | The bare light | 0 | jammed +9%, blinded +13%, ghosts +60% |
  | Steady Light | 0 | jammed +10%, blinded +12% |
  | Spotlight | +7% on a crowd, +12% on one enemy | |
  | Kinship, Mentor, Breakthrough | +4% each | |
  | Glare | +7% (crit builds) | elusive +14% |
  | Daylight | 0 | commanded +15% |
  | Sanctuary | 0 | jammed +13%, blinded +15% |
  | Prism, by the type lit | Aerie +12%, Hexer +8% on a crowd, Stormcaller +5%, Obelisk +3%, Scorcher +1%, Mortar and Sniper 0 | Mortar +7-8% on plating and insulation, Sniper +2% on a Knight |

  Against the reference per dollar with no defence: Tier 0 nothing, Tier I -60%, Tier II -40% to -68%, Tier III 0% to
  -28%. It is a counter tower: against Ghosts its bare light is worth 2.4 copies, against jam and blinding about half
  a copy bare and nearly one with Steady Light and Sanctuary. On a level with neither it is not worth building until
  Tier III, which is accepted.
- **Fair prices** (halfway between Walkers and the counter each answers): Steady Light $11, Afterglow $0 (utility:
  inside one cluster the bare light's linger already covers the lit towers' reach, so it pays only towers beyond the
  light); Spotlight $35 (its members even: they differ only in which enemy they hold, which the paper can't see),
  Lessons $21 (even); Edge of light $69 (Daylight $55 against Commanding, Glare $82, which helps every crit tower);
  Sanctuary $50, Prism $89.
- **Prism and the Aerie were the runaway combination.** A 30% Chill on everything in the light added +24% of four
  towers, a copy on its own, and put Tier III a third over the reference. At 15% it adds +12%. By the rule above, the
  Beacon's number moved, not the Aerie's.
- **What the paper can't see**: which enemy the Spotlight holds (a leaker, a commander), a tower built late
  (Mentor's case), towers beyond the light (Afterglow's), and how much of a climb XP rather than credits holds back:
  the model takes half, with gates of 120 XP to Tier II and 200 to Tier III.

### Bare tower

| Stat | Value | Today | Replaces |
|---|---|---|---|
| Price | $15 | $20 | |
| Light | 2.5 cells | range 1.5 cells, towers only | |
| Buff | none | Damage and Range +20%, stacking | |
| Jam taken | ×0.66 for every Lit tower, jam and blinding alike | ×0.5 | |
| XP | earns with the towers it lights, as today | | |
| Passive | Light: every enemy in its light is Revealed while inside and 1 s after; every tower in it is Lit and takes two thirds of the jam and of the blinding. | Aura: Damage and Range +20%, Jam taken ×0.5 | |

### Tiers

| Tier | Gate | Unlock | Colour |
|---|---|---|---|
| 0 | - | - | #d8b866 |
| I | XP 60 | - | #ecd48c |
| II | XP 120 | - | #f2e3b3 |
| III | XP 200 | - | #f8ecc2 |

### Upgrade tree

| Tier | Node | Kind | Cluster | After | Glyph | Colour | Cost | Replaces | Does | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | Steady Light | stat | | | aura.steady_light | | | aura.steady_light | Lit towers take a third of the jam and of the blinding, from two thirds. | A quarter was too strong. Name is a proposal. |
| 0 | Afterglow | stat | | | aura.afterglow | | | aura.afterglow | Reveal lasts 3 s after the light, from 1 s. | Against shrouds: an Elite Ghost re-casts every 3 s, so a Reveal that outlasts the cast keeps its group seen. A 0.5 s to 1.5 s linger moved nothing (`TODO.md`); 3 s is the first that may. |
| I | Searchlight | ability | Spotlight | | aura.searchlight | | | aura.searchlight | The beam holds the enemy in the light with the highest rank, then the most health: it has Priority while held. | A boss or an elite. Today's Radar Command Ping and the Obelisk's Arc Discharge as one rule. |
| I | Last Light | ability | Spotlight | | aura.last_light | | | aura.last_light | The beam holds the enemy in the light nearest its exit: it has Priority while held. | The leaker. Priority comes before a Taunting enemy's pull. |
| I | Ringleader | ability | Spotlight | | aura.ringleader | | | aura.ringleader | The beam holds an enemy in the light that gives its allies an aura, or else the one with the most allies within 1 cell: it has Priority while held. | Against the affixes that lead (Commanding, Mending, Jamming, Warding) and the Ghost's shroud; on a plain wave, the middle of the pack. |
| I | Kinship | ability | Lessons | | aura.kinship | | | aura.kinship | Lit towers earn 5% more XP for each other tower type in the light, up to 5. | A mixed cluster: copies add nothing. At 10% a type it beat Mentor and Breakthrough everywhere (On paper). |
| I | Mentor | ability | Lessons | | aura.mentor | | | aura.mentor | The Lit tower with the least XP earns twice as much. | A newcomer among veterans: a tower built late catches up. Today's Apprentice. |
| I | Breakthrough | ability | Lessons | | aura.breakthrough | | | aura.breakthrough | When a Lit tower opens a tier, every other Lit tower gains 10 XP. | Towers pushing their tiers: rewards upgrading the towers in the light. Each tower opens each tier once, so the chain ends by itself. Replaces the sold-tower Legacy (Decisions made). |
| II | Daylight | ability | Edge of light | | aura.daylight | | | aura.daylight | An enemy in the light gives its allies no aura. | A shroud, a command, a mending or a jamming aura stops at the light's edge. Silence stops casts; Daylight stops auras. |
| II | Glare | ability | Edge of light | | aura.glare | | | aura.glare | An enemy in the light is Exposed. | Against Elusive and for crits: it dodges less and is crit more. Expose stays in the game here, at half its old strength (Terms). |
| III | Sanctuary | stat | | | aura.sanctuary | | | aura.sanctuary | Lit towers take no jam and no blinding. | The full answer to a jamming boss and a Juggernaut's blinding field. |
| III | Prism | ability | | | aura.prism | | | aura.prism | An enemy in the light carries one stack of each Lit tower type's mark while inside: Sniper Sunder, Mortar Crack, Scorcher 5 stacks of Scorch, Aerie Chill (15%), Obelisk Toll, Stormcaller Static, Hexer Soulbrand. | One stack per type, never per tower: copies add nothing, as for Kinship. The Beacon's pane lists what its light carries now ("Sunder, Crack, Scorch"). A stack from the light is an ordinary stack (Terms). The Scorcher's mark is 5 stacks, as its Sear gives: one stack of Scorch is a single point of resilience and did nothing. The Aerie's Chill is 15%: at 30% it alone was worth a copy of the reference tower (On paper). |

### Terms

New or changed meanings of names the rows use. A name with no row here reads as the game says it today.

| Term | Glyph | Means |
|---|---|---|
| Light | | The Beacon's circle and its passive. Every enemy in it is Revealed; every tower whose centre is in it is Lit. Lights that overlap stack, each as if it stood alone. |
| Lit | | A tower in a Beacon's light: it takes two thirds of the jam and of the blinding. It replaces today's Aura buff and gives no damage, range or fire rate. |
| Spotlight | sonar.special.command_ping | The lantern's beam holds one enemy in the light until it dies or leaves, then picks again by its rule: the held enemy has Priority, and every tower that picks one target turns to it. Searchlight, Last Light or Ringleader chooses which. |
| Kinship | aura.head.amplifying_core.1 | Lit towers learn faster for every other kind of tower in the same light. |
| Mentor | aura.extra.tutelage.3 | The Lit tower that has learned least learns twice as fast. |
| Breakthrough | aura.extra.tutelage.2 | A Lit tower opening a tier teaches every other Lit tower a little. |
| Daylight | pulse.head.ward.4b | An enemy in the light gives its allies no aura. |
| Expose | | Agility -25 and Resilience -25: it dodges less and is crit more. Half today's -50 and -50, which was too strong. |
| Prism | aura.head.amplifying_core.2 | The light carries the mark of every kind of tower it lights: one stack of each Lit type's debuff on every enemy inside (the Scorcher's is 5 stacks of Scorch). A stack from the light is that debuff as any tower gives it, with no tie to the light: it fades by its own rule once the enemy leaves (Sunder, Crack, Scorch, Chill, Toll), and a Static or Soulbrand stack, which never fades, stays. A Static or Soulbrand stack from the light belongs to no tower: it adds its damage when the enemy discharges or bursts, and pays no Sparks or Souls. |

### Look

A lantern on a stone post, in a warm pale gold, `#d8b866`. It is the quietest tower on the board,
the opposite of today's two breathing rings, pulsing head and line to every tower it buffs. Drawn in the art pass (2026-11-13), in the raised view with a footprint that grows with the tier (`FEATURE-next-gen-tower-looks.md`, "Art direction"). The sheets show exactly what to build: [every tier](next-gen-art/catalogue.png), [on the board](next-gen-art/board.png), [the moving part and its cues](next-gen-art/heads.png), [shots and casts](next-gen-art/shots.png). Every measure, in cell units, is in `TowerFoundations.beacon`, built from a `TowerPose`.

**In code** (`FEATURE-next-gen-tower-looks.md`, "Building a tower's look"). The look is `TowerLook.AURA`. Nothing aims.

| What the tower holds | In the pose |
|---|---|
| the tower types in its light (Kinship, Prism) | `withPanes(List<TowerLook>)`, in a fixed order; a pane a type, repeating round the octagon |
| Daylight owned | `TowerCue.DAYLIGHT` |

- The breathing rings are gone; the lamp's slow ray is the Beacon's idle life.
- The panes come from `AuraTower.litTowers()` (the order built), one per distinct look, and only while Kinship or
  Prism reads the tower types (`readsTowerTypes`); otherwise the glass is plain.
- The reveal ray (`AuraTower.reveals()`, each with its tick) and the Spotlight (`heldEnemy()`) are `BeamDraw`s from
  the lantern in `TowerEffectFrameBuilder`; the wedge is a wide, faint beam. Glare's glint is a steady `CritSparkDraw`
  on every Exposed enemy, which has no dot of its own any more.
- A caster in daylight gives its allies nothing, so its aura and jamming rings are not drawn at all rather than cut
  off at the light's edge.
- The Lit mark is a `TowerStatusDraw` in the gold of the lantern, in each Lit tower's bottom-left corner, only while
  the Beacon is selected. Prism's list is the pane's "Light carries" row (`TowerInspection.carries`).

| Tier | Foundation |
|---|---|
| 0 | a round stone post with a gold diamond lantern on it; no plinth |
| I | one octagonal plinth step; the lantern becomes an octagon of eight glass panes under a small square roof |
| II | two steps; the post widens into a small round tower under the lantern |
| III | three steps; a lighthouse: a taller tower with a gallery round its lamp, eight rail posts on it, and a white finial |

- **The light** is a steady soft glow on the lantern, a quarter of a cell wide, never a ring on the ground. It does
  not pulse. Selecting the Beacon shows its circle and a small gold lantern mark in the bottom-left corner of each
  Lit tower ([icons](next-gen-art/icons.png)); nothing else draws a line between towers.
- **A reveal is the one motion**: when a hidden enemy enters the light, a short pale ray reaches from the lantern to
  it and fades. The enemy keeps today's Reveal mark.
- **The Spotlight**: a narrow, faint wedge of light from the lantern follows the enemy it holds, steady; the enemy
  keeps today's Priority mark.
- **Lessons**: none on the board; the XP shows in the pane.
- **Kinship and Prism**: the lantern's panes take the colour of each tower type in its light, one pane a type,
  repeating round the octagon. At Tier 0 the single glass takes the first type's colour.
- **Daylight**: the glow turns whiter and stronger, and an enemy aura that meets the light's edge is drawn cut off
  there. **Glare**: a white four-point glint on each Exposed enemy.

## Interconnections

- `FEATURE-next-gen-upgrade-tiers.md` (its node rules), `FEATURE-next-gen-tower-looks.md`. It uses no abilities and
  no shot flight.
- **The Radar leaves** (`FEATURE-next-gen.md`); its reveal comes here: Deep Scan's and Wide Band's Reveal become the
  light's, Command Ping's Priority the Spotlight's. Expose comes too, halved, as Glare's. Ping and Mark stay behind.
- **The Obelisk no longer reveals** (`FEATURE-next-gen-pulse-tower.md`), and its Arc Discharge (Priority on the highest
  rank inside) is the Spotlight's Searchlight now.
- **XP stays here.** The Obelisk's Tithe (its open question 8) is dropped: Kinship is the Beacon's idea, and the
  Obelisk has the most names already.
- **Prism's Toll gives the Beacon a share of Frailty**, the Obelisk's "everything hits harder here" effect: one stack,
  +4%, on every enemy in the light while an Obelisk is Lit. An accepted exception to Frailty being the Obelisk's alone.
- **Prism reaches into other towers' terms**: Sunder (Sniper), Crack (Mortar), Scorch (Scorcher), Chill (Aerie), Toll
  (Obelisk), Static and Soulbrand (Burst). A change to one of them changes Prism with it. A Static or Soulbrand stack
  with no owner is new in the Burst's rules (`FEATURE-next-gen-splash-tower.md`).
- **Tier gates are XP** (`FEATURE-next-gen-upgrade-tiers.md`): Kinship, Mentor and Breakthrough shorten a Lit tower's
  road to its next tier.
- **No attack-speed buff any more.** The Aerie's open question 7 (Feeding under buffs) and the Scorcher's rule that
  attack speed shortens only its reload name the Beacon among their sources.
- **The Ghost.** Its shroud is an aura of the Elite and the Boss: Daylight stops it at the light's edge, and Afterglow
  outlasts its re-cast. A Reveal sets Stealth to 0 and keeps Invisibility out while it lasts, nothing more, so it is
  worth something only against the Ghost and the Rift's Eclipse, and mostly to towers that aim.
- **The Mortar's Flare** (`FEATURE-next-gen-mortar-tower.md`) is the other reveal: one shell in about ten leaves a
  1.5-cell flare until the wave ends.
- **Jam and blinding** are one stat today (`DISRUPTION_TAKEN` scales a jam and a blinding field's miss chance), so the
  light and Sanctuary answer the Jamming affix, the jamming bosses and the Juggernaut's blinding field alike.
- `td/tower/aura/CLAUDE.md` describes `buffFor`, `AURA_POWER`, Chosen's ×3, Withering Field's interval and Rally's spot;
  those lines go. `td/tower/CLAUDE.md`'s "`AuraTower.buffs`' single 'is this an aura' check" and the stat layer
  "received emissions (`buffFor`)" change with it: a Beacon that emits only Jam taken may need no emission layer at
  all, for the plan to decide.
- `ExperienceAwarder` stays as it is: the Beacon earns through the towers it lights (`xpBonusFor`).

## Constraints and open risks

- **A bare Beacon gives no XP.** On a level with no Ghosts and no jammers it is worth little until Tier I; it is
  cheap and earns its way there through the towers it lights.
- **XP is worth nothing past a tower's last gate** (ranks are gone), so Lessons fade late. Sanctuary and Prism
  carry the Beacon's late tiers.
- **One circle for towers and enemies** may put a Beacon among towers far from the path, revealing nothing, or by the
  path with few towers lit. 2.5 cells is a guess at a circle that does both, and no node changes it.
- **Priority is +15% damage taken on one enemy** and turns every single-target tower in range to it. On a crowd,
  every Sniper turning to the Searchlight's boss may leak the rest; that is the pick's trade.
- **Daylight reads every enemy aura** (`Commanding`, `Warding`, `Mending`, `Jamming`, the Ghost's shroud): an aura's
  reach today knows nothing of towers, so the rule is new code on the enemy side. The plan finds where.
- **No other tower is balanced with a Beacon beside it.** Each tower's doc, model and price count only what is its
  own; a Beacon's jam taken, Priority, Expose, Prism stacks and XP never enter them. When a Beacon with some tower
  forms a combination that runs past the ceiling, the Beacon's node is what changes, never the other tower.
- **Prism breaks "one or two debuffs per tower"**: it hands out up to seven. It adds no name, its list sits in the
  pane, and one stack per type caps each. Sunder, Crack and the rest are still tuned
  without it: if a stack from the light takes one past its ceiling, Prism's stack is what changes.
- **Per-tick cost.** Prism refreshes up to seven debuffs on every enemy in every light each tick, and the Spotlight
  scans the light; both stay inside the tick budget (`td.PerformanceHarness`).
- **XP doesn't show what it is worth.** A player sees "10% more XP", not "a tier sooner" (open question 2).

## Decisions made

From the review rounds (2026-11-13):

- **The Radar's reveal comes to the Beacon** (the user's call).
- **Keep the Beacon, without its numbers.**
- **No range nodes.** 2.5 cells is enough; Wide Light, Wide Light II and Harmony (a light that grows with the
  types in it) are dropped.
- **Jam taken: two thirds bare, a third with Steady Light, none with Sanctuary.** A quarter in Tier 0 was too strong.
- **The Spotlight holds its enemy** until it dies or leaves the light, then picks again; a pick every second would
  flick the beam.
- **Ringleader always holds someone**: an aura-bearer first, else the middle of the pack.
- **Lessons stays a pick; Legacy (a sold Lit tower's XP passed on) is dropped.** Towers are sold mostly between
  waves, where a cluster pick switches for free, so Legacy would be the one pick for a sale and a frustration for the
  player who forgot to switch to it. Breakthrough takes its place.
- **XP stays in the Beacon**; the Obelisk's Tithe is dropped.
- **Glare is Expose, at -25 agility and -25 resilience**, half today's.
- **Prism is one stack per tower type, not per tower**, and gives Toll's Frailty as an accepted exception. In code a
  light adds its stack as an enemy enters it, on top of what the enemy carries and up to the mark's own cap, and again
  once the mark has worn off it while it stays; never every tick.
- **Lights that overlap stack** (decided at implementation): the jam shares multiply, each light adds its own Prism
  stack, and each keeps its own Static and Soulbrand tag.
- **A stack from the light is an ordinary stack**: it behaves as if any tower gave it, so marks that fade fade once
  the enemy leaves the light, and Static and Soulbrand, which never fade, stay. Their stacks from the light pay no
  currency.
- **The passive is the light alone**: Revealed enemies and Lit towers taking two thirds of the jam. Kinship is a Lessons
  member, so the bare tower reads as one idea and Lessons is a pick of three rules for three situations.
- **Reveal only by the light's touch**: 1 s after the light bare, 3 s with Afterglow. A Reveal on every Lit tower's hit
  (Tracer Light) was far stronger than the Mortar's rare Flare and is dropped.
- **A light's Static or Soulbrand stack pays no currency**; in code its damage is credited to the Beacon's record.
- **Prism gives the Aerie Chill**, not Freeze.
- **Every balance tied to the Beacon is the Beacon's.** No other tower is weighed with it, and a runaway combination is
  tuned in the Beacon's nodes.
- **Prism's Chill is 15% and the Scorcher's mark 5 stacks of Scorch**; Kinship is 5% a type, Breakthrough 10 XP, so
  the three Lessons price even (On paper).
- **Weak with no defence until Tier III is accepted**: the Beacon is a counter tower.
- **Tier gates stay XP.** Tower types already pay through Kinship and Prism; a gate on them would be a third.
- **A tree of four tiers**, with Sanctuary and Prism in Tier III.
- **Dropped**: the Aura's Damage and Range +20%, `AURA_POWER`, Amplifying Core and its II, Keen Edge, Broadcast,
  Conduit, Broadcast II's buff duration, Shared Lessons, Withering Field, Chosen, Rally and its path spot, the lines to
  buffed towers; from the drafts, Tutelage and Tutelage II, Wide Light and its II, Afterglow II, Legacy, Study (XP
  doubled from the held enemy), Golden Hour (bounty in the light: no bounty mechanic), Burning Glass (a burning beam:
  damage on a support), Twin Lamps (a second beam), Signal Fires (Beacons sharing tower types), Harmony, Tracer
  Light, Vigil (Lit towers start a wave warmed up), Lore (XP the player hands out), Embers (a currency) and a discount
  on Lit towers' upgrades (build a Beacon, buy, sell it).

## Open questions

1. ✅ **The price.** $15, since it adds no damage (the user, 2026-11-13: "the cheapest tower"). The Sniper and the
   Burst cost $15 too in the drafts, so it shares the lowest price until the balance pass sets them all.
2. ✅ **Showing what XP is worth.** A Lit tower's pane shows its XP bonus as a percentage with the Lesson's name,
   beside its XP toward the next gate (which every tower's pane shows once ranks go). No forecast of when the tier
   opens: that depends on a fight that hasn't happened (final review; the user had no preference).
3. ✅ **Tier gates.** XP 60, 120 and 200, as every tower (`FEATURE-next-gen.md`, "Answered by the user").
4. ✅ **What the paper takes on faith.** Left to the balance pass, with the gates now set at 60, 120 and 200. The XP gates (120 and 200 are placeholders) and how much of a climb XP holds
   back (half) set what Lessons are worth; `BalanceReport` judges them once the gates are set.
5. ✅ **The floor rule** (the bare Burst on damage) cannot apply to a tower that deals none. As for the Obelisk, the
   Beacon is read by what the towers it lights gain.
6. ✅ **The Edge of light pick** (Daylight $55, Glare $82) is not marked `rebalance`, but its members lie the
   furthest apart of the unmarked clusters. Left to the balance pass: Daylight's worth is the commanded waves the
   paper models thinly (final review, by "Balance waits"). Weakening Glare (Expose at -20 and -20) is its lever.
