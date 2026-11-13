# Feature Request: Next-gen Obelisk Tower

**Status:** draft, 2026-11-13, sixth round, reviewed with the whole bundle. Tiers 0 to III and the look, written to the node rules in
`FEATURE-next-gen-upgrade-tiers.md`. Part of `FEATURE-next-gen.md`, which records why the Radar leaves the game and
its turning beam comes here. Weighed on paper (`trees/pulse.py`); the numbers below are that model's.

## Summary

The Obelisk (`PulseTower`) stays the kill box: a place on the path that holds enemies and makes them take more while
they are held. Toll becomes the passive itself, open from the start: every stack deepens the drag and makes the enemy
frail. Next-gen gives it two things it lacked. It carries the game's one "everything hits harder here" effect
(Frailty), now that the Beacon no longer buffs damage. And it takes the Radar's beam as its signature ability: the
Sundial, the stone's shadow turning once around it like a sundial's, an aimed hit on every enemy it passes and a Toll
stack. A Tier II pick decides how the stone holds, Burden (a deep, steady slow), Gravity (a hard stop) or Umbra (the
shadow bites in the field), and its rune shows which.

## What this feature adds

The tables below are read by `td.ui.PreviewUpgrades` (run-jtd), which draws the tree beside today's panel and lists
what the nodes no row replaces leave unused. The column rules are in run-jtd's `SKILL.md`.

### How the tree builds

- **Time held is the Obelisk's damage.** Its own damage stays small; its worth is the seconds the enemies spend in
  front of the other towers, and how much harder each of their hits lands meanwhile. One number holds both: Toll.
- **Toll is the passive**, no longer an Attune: an enemy inside gains a stack every second, up to 5. Outside every
  field it loses a stack a second. Each stack deepens the field's drag and makes the enemy take more damage from every
  source (Frailty). Toll is the enemy's, and it is only a number: every field and the Sundial add to the same stacks,
  and what a stack does never depends on which stone gave it. Two fields' drags never add. That removes
  Interference, a rule the player never saw.
- **Three kinds of node, combinable**, as the Sniper's are crit, the Fourth Shot or speed: the ones that build Toll
  (Quick Toll, Deep Pull, Burden, Deep Toll), the field's own (Gravity, Knell, Hum, Wide Field) and the shadow's
  (the Sundial, Deep Shadow, Umbra, Swift Shadow). They describe how nodes combine; no rule ties a cluster to them.
- **Tier 0 is neutral**: range and damage, which every build wants. The Toll levers start in Tier I.
- **The Sundial is Tier I's signature**: every 3 s the stone's shadow turns once around it, out to 4 cells. Every
  enemy it passes takes an aimed hit, as the Sniper's shot is aimed, and a Toll stack. What the Radar's beam had that
  nothing else does stays: it reaches everything along one bearing at once, so a path that runs away from the stone is
  swept along its whole length.
- **Ward is Tier I's other orphan**: an enemy in the field is Silenced. Every build may deny casters.
- **How the stone holds is the Tier II identity pick**: Burden (each stack drags deeper, the field's slow may reach
  75%), Gravity (a beat inside Dazes, once a visit) or Umbra (the shadow hits twice as hard on an enemy in the field).
  The rune on the stone changes with it.
- **Full Toll is 5 stacks or more**, so Knell works under Deep Toll too.
- **The payoff is the Tier III pick**: Knell (the stone rings once a second while anyone with full Toll is inside,
  and everyone in the field takes a hit: a crowd) or Deep Toll (a Deep Toll field builds Toll to 10: the one enemy that
  stays, a boss).
- **On paper** (`tree_calc.py`, `trees/pulse.py`). Per dollar of the tower, crowd role, against the reference (the
  Sniper's median), Tier 0 to III:

  | Build | 0 | I | II | III |
  |---|---|---|---|---|
  | Burden, Deep Toll | -53% | -29% | -14% | -3% |
  | Burden, Knell | -53% | -29% | -14% | -4% |
  | Gravity, Deep Toll | -53% | -29% | -14% | -22% |
  | Gravity, Knell | -53% | -29% | -14% | -11% |
  | Umbra, Deep Toll | -53% | -29% | -13% | -18% |
  | Umbra, Knell | -53% | -29% | -13% | -9% |

  Both clusters read situational, with no `rebalance` (Decisions made). Tiers 0 and I stand under the band, and on a
  crowd under the floor (the bare Burst) up to Tier II; on one enemy every build is under it at every tier. Accepted
  (Decisions made): the paper sets four reference towers around the field, while a kill box in play holds more, and
  the drag and Frailty multiply every one of them. The Balance Report judges it once the revamp is in.
- **Where the paper is blind.** A lone enemy is spawned once the last one has left the reach, so a longer reach
  spreads the same damage over more seconds and reads as less: reach is undervalued on one enemy. The path is straight
  and a cell from the stone; one that runs away from it, where the Sundial is at its best, has no scenario.
- **Fair prices** (halfway between Walkers and the enemy each counters most): Wide Field $8, Hum $5; Sundial $20,
  Ward $1, Quick Toll $3, Deep Pull $3; Burden $14, Gravity $18, Umbra $17, Hum II $9, Deep Shadow $18, Wide Field II
  $19; Deep Toll $19 and Knell $23 (situational: Deep Toll wins an Elite, a Boss, Menders and the Relay,
  Knell the crowds), Deep Pull II $14, Swift Shadow $22.
- **The field's own damage is small on purpose**: 10 a second, 20 with both Hums. The Relay's insulation eats it
  whole; the Relay counters this tower, and only the Sundial's hit gets through.
- **Toll's pace stays a stack a second.** A Walker crossing the field once leaves with 2 stacks (3 with Wide Field, 4
  with Wide Field and Quick Toll, a Runner with 1-2), so Knell needs an enemy held longer: a freeze, a Daze, a slow or
  a corner. That keeps Knell the rarer pick it should be.
- **Quick Toll is 0.75 s, not 0.5 s.** At 0.5 s it doubled everything Toll drives and was the tree's strongest
  multiplier.
- **Toll fades as soon as the enemy leaves.** A stack from the Sundial far from the field may fade before the enemy
  walks in; the shadow's worth is mostly its hit.
- **Frailty** is +4% damage taken a stack: +20% at 5, +40% at 10 under Deep Toll. One Toll per enemy, so two
  Obelisks never stack it; it adds to Priority's +15% and the Hexer's Vulnerable.
- **The three stones win apart.** Gravity reads time in the field, not Toll, so it stops a Runner too and wins the
  rest; Umbra wins an Elite, a Boss, the Juggernaut and the Relay (the shadow's hit passes the insulation that eats
  the field's damage); Burden wins no scenario alone and is the Toll road into Deep Toll (+15% of the build together,
  open question 4).
- **Gravity's Daze is 0.75 s.** At 1 s the cluster read `rebalance` ($14 to $23); at 0.5 s even, but Gravity won no
  scenario of its own.
- **Ward on paper**: a crowd with a Mender every third enemy (`menders`, about a Veteran's heal, 7 a second to every
  ally within 2 cells). It adds 0-2%. Ward's worth is what the paper has no scenario for: a Brood Nurse's ward and the
  Warding shields, a boss's calls, a Timekeeper's rewind. It is kept on judgement (open question 5).
- **Dropped on paper**: Long Toll (Toll held 4 s after the last stack) added nothing; Event Horizon added 0-2%, since
  few enemies die inside a field this small; Echo (a death passes its Toll on) added nothing. Brand (Toll fading a
  stack every 4 s) added 1-2% and needed a stone-made exception on the enemy's Toll. Overflow (a stack past full
  passes to the nearest enemy inside with less) won no scenario. A shadow payoff in Tier III (Long Shadow, the shadow
  out to 6 cells) added +3% and won only small crowds; giving it 3 Toll stacks a pass, or splitting a Toll node off
  it, brought it level with Knell only as two changes in one node or as an After the rules don't allow.
- **Its names**: the field, Toll, Drag, Frailty, the Sundial, Ward, Silence, Gravity, Daze, Burden, Umbra, Knell,
  Deep Toll. Thirteen. Its enemy debuffs are Toll (with its drag and Frailty) and Ward's Silence or Gravity's Daze.

### Bare tower

| Stat | Value | Today | Replaces |
|---|---|---|---|
| Price | $20 | $25 | |
| Damage | 10 magic a second to every enemy inside | 1 a tick (20 a second), less for every field overlapping | |
| Range | 1.5 cells | | |
| Drag | 10%, +4% for each Toll stack (30% at 5); a field's slows cap at 50% | 15% flat | |
| Reach | Everyone inside, hidden enemies included; needs no visible enemy | | |
| Reveals | Nothing: the Beacon reveals | nothing until Ward | |
| Passive | Toll: an enemy inside gains a stack every second, up to 5, and loses one a second outside every field; each stack deepens the drag and makes it take 4% more damage from every source. | only with Attune; scales the nodes' effects | base.attune, pulse.extra.bite.1 |

### Tiers

| Tier | Gate | Unlock | Colour |
|---|---|---|---|
| 0 | - | - | #8f86b8 |
| I | XP 60 | $20 | #a89fd2 |
| II | XP 120 | - | #c6c0e1 |
| III | XP 200 | - | #e2dad8 |

### Upgrade tree

| Tier | Node | Kind | Cluster | After | Glyph | Colour | Cost | Replaces | Does | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | Wide Field | stat | | | base.range | | | base.range | Range +0.5 cells | |
| 0 | Hum | stat | | | pulse.special.arc_discharge | | | | Damage +5 a second | Name is a proposal. |
| I | Sundial | ability | | | sonar.special.mark_on_sweep | | | | Every 3 s the stone's shadow turns once around it, out to 4 cells: every enemy it passes takes an aimed hit of 20 magic damage and a Toll stack. | The Radar's beam as the stone's shadow. Aimed, so agility dodges it. A path running away from the stone is swept along its length. |
| I | Ward | ability | | | pulse.head.ward.2 | | | pulse.head.ward.2 | An enemy in the field is Silenced while inside. | Today's Null Field. Against casters: Menders, shielders, a boss's calls. |
| I | Quick Toll | stat | | | pulse.extra.bite.3 | | | pulse.extra.bite.3 | Toll builds a stack every 0.75 s, from 1 s | Today's Resonant Coils. At 0.5 s it doubled everything Toll drives. Name is a proposal. |
| I | Deep Pull | stat | | | pulse.head.gravity.1 | | | pulse.head.gravity.1 | Drag +2% for each Toll stack | 30% to 40% at 5 stacks. Today's Undertow as a plain number. Name is a proposal. |
| II | Burden | ability | The stone | | pulse.head.gravity.1 | #c9a56c | | | Drag +3% for each Toll stack, and the field's slow may reach 75%, from 50%. | The steady weight: an earthen rune. Only inside the field, and two fields' drags never add, so more stones hold a longer stretch, not a deeper one. The Toll road into Deep Toll. |
| II | Gravity | ability | The stone | | pulse.head.gravity.4b | #7d9be6 | | pulse.head.gravity.2 | An enemy a second inside the field is Dazed for 0.75 s, once a visit. | The hard stop: a deep blue rune. Today's Snare and Stasis as one rule. It reads time inside, not Toll, so a Runner is stopped too. After a beat inside, not at the edge; a row of stones Dazes once for each. What the Aerie's freeze and the Scorcher's Thermal Shock read. |
| II | Umbra | ability | The stone | Sundial | sonar.head.long_reach.4b | #bcaaf0 | | | The shadow's hit deals double damage to an enemy in the field. | The shadow's bite: an eclipsed rune. Against an Elite, a Boss and the Relay. Name is a proposal. |
| II | Hum II | stat | | | pulse.special.arc_discharge | | | | Damage +5 a second | |
| II | Deep Shadow | stat | | Sundial | sonar.head.rapid_array.1 | | | | The shadow's hit is Shadow damage. | Name is a proposal. Shadow passes insulation (Terms), so the shadow's hit gets through a Relay as the field's small damage can't. Unweighed: it was +16 ($18). |
| II | Wide Field II | stat | | | base.range | | | base.range.2 | Range +0.5 cells | In Tier I it put Tier I a quarter over the reference. |
| III | Knell | ability | Payoff | | pulse.head.gravity.3 | | | | While an enemy with 5 or more Toll stacks is inside, the stone rings once a second: every enemy in the field takes 20 magic damage. | A crowd: each ring hits everyone inside. One beat for the stone, not one for each enemy at full Toll, so it grows with the crowd, never with its square. It rings for an enemy that walks in already full. |
| III | Deep Toll | ability | Payoff | | pulse.extra.bite.4 | | | pulse.extra.bite.4 | Toll builds to 10 in this field, from 5. | The one that stays: a boss walks deeper into Toll than anything else, and Frailty and Drag grow with it. Its stacks are ordinary ones and fade outside as any do. |
| III | Deep Pull II | stat | | | pulse.head.gravity.1 | | | | Drag +2% for each Toll stack | |
| III | Swift Shadow | stat | | Sundial | sonar.head.rapid_array.3 | | | | The shadow turns every 2 s, from 3 s. | Name is a proposal. |

### Terms

New or changed meanings of names the rows use. A name with no row here reads as the game says it today.

| Term | Glyph | Means |
|---|---|---|
| Field | | The circle around the Obelisk. Everyone inside takes its damage and its drag, hidden enemies included, with or without a visible enemy. Fields overlap without stacking: one Toll, the deepest drag. |
| Toll | | Time spent in a field, a stack a second, up to 5; the Sundial's shadow adds one as it passes. It is the enemy's: every field and the shadow add to the same stacks. Outside every field it loses a stack a second. Each stack deepens the drag and makes the enemy take more damage. Full Toll is 5 stacks or more. |
| Drag | pulse.head.gravity.1 | The field's slow: 10%, and more for each Toll stack. Two fields' drags never add; the deeper one holds. |
| Frailty | pulse.extra.bite.1 | Each Toll stack makes the enemy take 4% more damage from every source. |
| Sundial | sonar.special.mark_on_sweep | Every few seconds the stone's shadow turns once around it, past the field. Every enemy it passes takes an aimed hit and a Toll stack. |
| Ward | pulse.head.ward.2 | Everything inside the field is Silenced. |
| Burden | pulse.head.gravity.1 | The steady weight: each Toll stack drags deeper, and the field's slow may reach 75%. |
| Gravity | pulse.head.gravity.4b | The hard stop: an enemy a beat inside the field is Dazed, once a visit. |
| Umbra | sonar.head.long_reach.4b | The shadow's hit lands twice as hard on an enemy in the field. |
| Knell | pulse.head.gravity.3 | While anyone with full Toll is inside, the stone rings once a second: every enemy in the field takes a hit. |
| Deep Toll | pulse.extra.bite.4 | Toll builds to 10 in this field. |

### Look

A dark standing stone, square in plan, with a pyramidion at its tip that holds one rune. Calmer than today's spinning
star and three ripple rings. Its colour is `#8f86b8`; the stone's top is a darker step of it and its face, turned
to the player, darker still. It is the tallest thing on the board for its width. Drawn in the art pass (2026-11-13), in the raised view with a footprint that grows with the tier (`FEATURE-next-gen-tower-looks.md`, "Art direction"). The sheets show exactly what to build: [every tier](next-gen-art/catalogue.png), [on the board](next-gen-art/board.png), [the moving part and its cues](next-gen-art/heads.png), [shots and casts](next-gen-art/shots.png). Every measure, in cell units, is in `.claude/skills/run-jtd/TowerArt.java`, `obelisk` and `rune`.
[Its fields and the Sundial](next-gen-art/grounds.png) are on the grounds sheet.

| Tier | Foundation |
|---|---|
| 0 | the bare stone: a square, a smaller square on it, and a four-faced pyramidion lit from the top left; no plinth |
| I | the stone on one square plinth step |
| II | two steps; three small stones on the lower step, north, south-east and south-west |
| III | three steps; a larger stone in a henge: eight small stones round the lowest step |

- **The rune names the stone.** It sits on the pyramidion and is the only part that glows:

  | Stone | Rune | Glow | Field |
  |---|---|---|---|
  | plain | an upright bar with a short crossbar | the tier's colour | `#8f86b8` |
  | Burden | a bar over a downward chevron | earthen `#c9a56c` | `#7a6a52`, a denser fill |
  | Gravity | a spiral drawn inward | deep blue `#7d9be6` | `#4d6fb8`, darkening toward each stone |
  | Umbra | a pale ring with a dark disc over it, an eclipse | pale violet `#bcaaf0` round a dark disc `#2a2540` | `#5a4f8a` |

  The glow colours are lighter than the drafts' rune colours, which were too dark to read on the stone (Umbra's
  `#3a3550` stood at 1.6:1 on the floor). The rune glows brighter, not bigger, while an enemy at full Toll is inside.
- **The field is a ground zone**, drawn as the Mortar's and the healing grounds are: every Obelisk field of one look
  is one patch of ground with one even, faint fill and one outline around them all, so overlapping fields read as
  one stretch of held path, not a stack of circles. Fields of different picks are different patches. Nothing in a
  field moves while it holds nobody.
- **Toll shows on the enemy, not the field** ([marks](next-gen-art/marks.png)): five segments round the enemy in
  Toll's marker colour fill as the stacks land, bolder at full, and each stack gained sends one thin ripple out from
  the enemy. Motion means a stack landed.
- **The Sundial** turns once from the stone's foot and fades. The floors are near-black, so it cannot be a dark
  shadow: it is a cool violet veil, strongest at the stone and fading to nothing at its tip 4 cells out, with a
  lighter line along its leading edge. It moves only while it turns (1 s of every 3) and only while an enemy is
  within its reach, so the stone stands still between turns and over an empty path. A hit it lands flashes on the
  enemy, small, like the Sniper's.
- **Ward**: a thin dashed rune circle, in Silence's colour, runs just inside the patch's outline.
- **Burden**: the patch reads heavier, a fill twice as dense, with no motion of its own.
- **Gravity**: the patch darkens toward each stone. A Daze keeps today's mark.
- **Umbra**: the veil is darker and bluer where it crosses the patch, and a hit it lands there flashes larger.
- **Silence gets a mark over the enemy**, from every source of it (Ward, the Aerie's Shade, the Hexer): a small
  struck-out speech mark above its head while it lasts, so the board shows which enemy can't cast.
- **Knell**: the rune flashes white and one ring in the rune's colour runs out to the patch's edge on each ring.
  **Deep Toll**: the rune glows hotter past 5 stacks inside.

## Interconnections

- `FEATURE-next-gen-upgrade-tiers.md` (its node rules), `FEATURE-next-gen-tower-looks.md`. The Sundial needs no ability
  bar from `FEATURE-next-gen-tower-abilities.md`: it is a rhythm, like a tower's fire rate.
- **The Radar leaves** (`FEATURE-next-gen.md`). Its geometry comes here: `SonarSweep` (the arc swept since the last
  tick, never the instantaneous angle) and `SpinningBeam`. `td/tower/sonar/CLAUDE.md`'s constraint moves into
  `td/tower/pulse/CLAUDE.md`. Twin Beam, Horizon, Ping, Expose, Mark, Resonating, Fractured, Phased Array, Fault Line
  and the frequency line stay behind. The shadow's hit is aimed (`asAimed()`, through `landHit`), so it can be
  dodged, as the Sniper's shot can; the field's own damage is an area and never is.
- **Ground zones**: the field joins `ZonePatchDraw` as a patch of its own kind, though it is a tower's range, not a
  zone in `GameWorld.zones()`. The plan decides whether it becomes a zone or only borrows the drawing.
- **The Beacon reveals now** (`FEATURE-next-gen-aura-tower.md`). The Obelisk's Sight and True Sight go: the field
  still hits hidden enemies, as all area damage does, but it shows them to no tower.
- **Frailty is the game's one "everything hits harder here" effect** once the Beacon stops buffing damage. Priority
  (the Beacon's Spotlight) adds damage taken on one enemy; the two add.
- **The Beacon's Prism gives Toll**: an enemy in a light that holds a Lit Obelisk carries one stack while inside, and
  with it one stack's Frailty, an accepted exception (`FEATURE-next-gen-aura-tower.md`).
- **A Silence mark on the board** (Look) is new drawing for every tower that silences, so it belongs with the enemy
  markers (`shipped/FEATURE-effect-visuals.md`), not with the Obelisk alone.
- **Gravity's Daze and the Aerie's freeze.** Today's Undertow counts as a chill for a freeze; Drag and Burden don't, so
  the Aerie's freeze meter and the Scorcher's Thermal Shock find held enemies here only through Gravity's Daze.
- **Shadow damage** (`FEATURE-next-gen-seeker-tower.md`, Terms) gives Deep Shadow its rule.
- `td/tower/pulse/CLAUDE.md` describes Toll as an ordinary effect the field refreshes, `FieldHolds`, `FieldMode`,
  Ley Lines and Event Horizon's death count; Interference reads other fields through a `TowerVisitor`. Those lines
  change.

## Constraints and open risks

- **The paper counts what the towers around it gain.** Frailty is priced by the extra damage four bare reference
  towers deal an enemy carrying it, drag, Burden and Daze by the enemy-seconds held (`held_back` in
  `reference.json`), and those neighbours' fire also kills, so a crowd thins as it would in play. Silence is utility:
  Ward prices at nothing.
- **Toll shared between fields** means three Obelisks in a row build one Toll, not three. Several fields on one stretch
  are still worth more (a longer stretch held), but no longer stack.
- **A row of Gravity stones** Dazes once for each stone, each after a beat inside it: every Daze is a tower bought,
  as every freeze is an Aerie.
- **Per-tick cost.** A shadow sweeping 4 cells, Toll on every enemy it passed and Knell's rings stay inside the tick
  budget (`td.PerformanceHarness`).
- **Hold stacks with other slows.** Drag, Tar, Chill and a freeze can pin an enemy; the field's own cap is 50%, 75%
  with Burden.

## Decisions made

From the review rounds (2026-11-13):

- **The Radar's beam comes to the Obelisk as an ability**, with no pick of its shape: one beam, an aimed hit and a
  Toll stack on every enemy it passes.
- **The Beacon takes over revealing**; the Obelisk reveals nothing.
- **A dark stone with a rune**, the rune changing with the identity pick.
- **Fields draw as one ground patch**, as the other zones do, so overlaps don't clutter the board.
- **Toll stays a stack a second**: enemies around a kill box are held by other towers too, and Knell should be rare.
- **Under the floor on one enemy is fine**: the Obelisk is a soft-support tower; its worth is the towers around it.
- **Silence gets a mark over the enemy on the board**, from every tower that silences.
- **Damage reads per second** in the rows.
- **The field deals 10 a second, each Hum +5**: at 20 and +10, 40 a second over the whole field made it the
  strongest tower, as it was once before a rebalance. Under the band on paper is accepted (On paper).
- **Quick Toll is 0.75 s**: at 0.5 s it multiplied every rule that reads Toll.
- **Toll is only a number.** What a stack does never depends on the stone that gave it, so Frailty is part of Toll,
  and Deep Toll is a field that builds Toll higher, not a property the Toll carries.
- **No hold.** Toll fades a stack a second as soon as the enemy is outside every field.
- **Full Toll is 5 stacks or more**, so Knell works with Deep Toll.
- **Knell rings once a second while anyone with full Toll is inside**, also for one that walks in full.
- **Ward is a Tier I orphan.**
- **Dropped**: Brand and Overflow as Gravity's rival (both weighed; Brand also needed a stone-made exception on Toll).

Fifth and sixth rounds (2026-11-13):

- **Toll and Frailty are the passive.** Every pick in the fourth round read Toll, and Burden beat Gravity in every
  scenario: the Tier II pick was no choice.
- **Tier 0 is neutral**: Quick Toll and Deep Pull move to Tier I.
- **Gravity stays, and reads time in the field**: a beat inside Dazes, once a visit, whatever the Toll. Its Daze is
  0.75 s, where the cluster is situational and Gravity still wins on its own.
- **Umbra joins the stone's pick**: the shadow's hit doubled on an enemy in the field.
- **The node kinds don't shape the clusters.** A rule that every cluster holds one member of each kind called for a
  shadow payoff in Tier III; none weighed level with Knell and Deep Toll within the node rules (On paper). A cluster
  only needs members that win apart.
- **Knell keeps the full-Toll gate.** Ringing for anyone inside made it the payoff for every scenario.
- **The paper stops at the cluster verdicts.** Before the tower is planned its clusters show no `rebalance` and no
  one right answer, and each member wins somewhere; finer numbers wait for the bundle's balance pass
  (`FEATURE-next-gen.md`, "Balance waits"), since the model's blind spots are larger than the gaps between builds.
- **A field Spirit debuff is left out, unweighed.** Soul Drain was strong because its Sickened lasted after the enemy
  left (to -100 Spirit); held to the field, it would weaken heals that mostly land outside (open question 10).

Proposed, not yet reviewed:

- **The beam is the stone's shadow, the Sundial.** An obelisk is a sundial's gnomon; a radar dish's beam on a rune
  stone would be a borrowed look.
- **No Interference.** Toll is the enemy's and drags don't add, so overlapping fields need no hidden penalty.
- **After**: Umbra, Deep Shadow and Swift Shadow need the Sundial. Nothing else does.
- **No fork.** The rune changes with the identity, as the Aerie's spire and the Scorcher's flame do.
- **Burden's, Gravity's and Umbra's runes and patches** (Look).
- **Dropped**: Interference, Sight, Dead Zone, True Sight, Purge, Corrosion, Snare and Stasis as their own nodes
  (Gravity holds both), Gravity Well, Arc Discharge and Condemn (the Beacon's Spotlight gives Priority), Soul Drain,
  Ley Line and its pinning, Event Horizon (and its purpose gate), Long Toll, Echo, Frailty as a node, the first
  draft's beam pick (Twin Sweep, Long Sweep), the shadow holding enemies under the field's rules until its next
  turn, and Long Shadow with its Toll variants.

## Open questions

1. ✅ **The price.** $20, as the other next-gen towers at $15-20, against $25 today; the balance pass sets every
   price (final review, by "Balance waits").
2. ✅ **Toll's pace.** A stack a second (On paper): the Obelisk seldom stands alone, enemies in a kill box are frozen,
   Dazed and slowed by the towers around it, so Toll fills where they are held, and Knell stays rare.
3. ✅ **What sets off the Sundial.** A rhythm, every 3 s: the shadow is the Obelisk's second attack (the user,
   2026-11-13). The shadow turns only while an enemy is within its reach; its rhythm counts on meanwhile, so the
   first turn comes at once when an enemy walks in (final review, autonomously).
4. ✅ **Burden with Deep Toll.** Kept, and weighed again at the final balance (final review, by "Balance waits"). The tree's strongest pair, +15% of the build together: deeper Toll drags deeper up to
   the 75% cap. Its Tier III reads -3%, the best of the six builds, with Burden and Knell a point behind. Deep Toll at
   8 stacks instead of 10 left the pair at +14%.
   Both are Toll, so the pair is the Toll road, and it wins an Elite and a Boss, not every scenario. Making Deep
   Pull II weaker or dearer, the node that takes this build to the cap soonest, is the balance pass's lever.
5. ✅ **Ward on paper.** With Menders it adds 0-2% (On paper). Kept on the user's judgement: its worth is shields
   and calls. The paper models a shielder next (a Brood Nurse or the Warding affix: what a cast shield would have
   absorbed of the towers' fire), the scenario the Aerie's Shade needs too (its question 14).
6. ✅ **Tier gates.** XP 60, 120 and 200, as every tower (`FEATURE-next-gen.md`, "Answered by the user").
7. ✅ **Deep Shadow as Shadow damage.** Yes (the user, 2026-11-13): Shadow is a new term for the bundle
   (`FEATURE-next-gen.md`). Deep Shadow is weighed as it in the balance pass.
8. ✅ **XP here or in the Beacon.** In the Beacon: Kinship is its idea, and the Obelisk keeps its names. The Tithe
   is dropped (`FEATURE-next-gen-aura-tower.md`).
9. ✅ **Ley Line.** Two Obelisks linked by a beam was the tower's most original idea and its most complex (pinning,
   host picking). Dropped (Decisions made); it could return as a Tier III member after the bundle.
10. ✅ **A field Spirit debuff.** Left out (the user, 2026-11-14): Ward already answers casters and stays a Tier I
    orphan every build may take. A cluster with Ward (Silence or the Spirit debuff) and a second orphan beside Ward
    were the other options.
11. ✅ **Thirteen names.** Accepted (the user, 2026-11-14): Drag keeps its own term. Folding it into Toll's text as its
    slow, for twelve, was the other option.
12. ✅ **A shadow payoff.** None in Tier III (final review, by "Balance waits"): the shadow's kind ends in Umbra and Swift Shadow, and Umbra leads into
    Knell. If the Balance Report finds the shadow weak where the paper is blind (a path that runs away from the
    stone), a shadow member joins the Payoff cluster then, not before.
13. ✅ **Meltdown's name.** Deep Toll (the user, 2026-11-13): Meltdown is the Scorcher's.
