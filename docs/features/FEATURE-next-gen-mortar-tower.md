# Feature Request: Next-gen Mortar Tower

**Status:** draft, 2026-11-12, reviewed with the whole bundle 2026-11-13. Tiers 0 to III and the look, from the visual
request's handwritten Mortar notes, written to the node rules in `FEATURE-next-gen-upgrade-tiers.md`. Part of
`FEATURE-next-gen.md`.

## Summary

The Mortar is the classic area cannon: it fires slowly and far. What sets it apart is its **magazine**: a row of
shells it has ready, each drawn by lot when it joins the queue. Each shell carries its own stats. The tower has no
crit, no abilities and no currency: its odds are the shells it draws. A build either improves the Heavy shell, the
near-basic shell it puts its effects into, or fills the magazine with other kinds of shell and gets fewer Heavies.
The blast deals damage in zones, as the Burst's does, instead of falling off with distance.

## What this feature adds

The tables below are read by `td.ui.PreviewUpgrades` (run-jtd), which draws the tree beside today's panel and lists
what the nodes no row replaces leave unused. The column rules are in run-jtd's `SKILL.md`.

### How the tree builds

- **The magazine is the build.** Every kind of shell the tower owns has a weight; a new slot draws a kind with the
  chance of its weight over the sum. A heavy build raises the Heavy's weight and puts effects into the Heavy; a
  payload build adds kinds, and each new kind dilutes the Heavy. A hybrid takes some of each.
- **Levers** are plain stats every build buys some of: damage, range, reload, and the weights.
- **Order is the Tier III pick.** Fusion fuses two Heavies in a row into a Nuke; Rich Mix never draws the same kind
  twice in a row. Each rules the other out, so they form one cluster. A heavy build picks between bursts (Fusion) and
  a steady half of Heavies (Rich Mix: with only Normal and Heavy owned, the two alternate, whatever their weights);
  a payload build takes Rich Mix for a magazine that is never two of a kind.
- **The magazine at five builds** (weights Normal 9, Heavy 1 to start; shares over a long run of draws):

  | Build | Normal | Heavy | Nuke | Payload | Flare |
  |---|---|---|---|---|---|
  | Bare (N9, H1) | 90% | 10% | - | - | - |
  | Heavy bursts: Big Bore, Big Bore II, Fusion (N9, H4) | 75% | 17% | 8% | - | - |
  | Heavy steady: Rich Mix (N9, any H) | 50% | 50% | - | - | - |
  | Payload: a Payload, Flare, Rich Mix (N9, H1, P3, F1) | 43% | 13% | - | 32% | 13% |
  | Hybrid: Big Bore, a Payload (N9, H2, P3) | 64% | 14% | - | 21% | - |

- **The builds are situational.** Damage a second at the end of Tier III (Heavy Charge, Overcharge, Quick Reload),
  on a path one cell wide, where a blast reaches enemies in proportion to its radius rather than its area, against
  Walkers at a wave's default spacing (about 1.4 cells apart):

  | Build | One enemy | A crowd |
  |---|---|---|
  | Heavy steady (Rich Mix), Shrapnel | 33 | 67 |
  | Heavy bursts (Fusion), Shrapnel | 28 | 70 |
  | Heavy steady (Rich Mix), Concussion | 28 | 54 |
  | Heavy bursts (Fusion), Concussion | 26 | 66 |
  | Napalm (Thick Mix, Rich Mix, Flare), Lasting Mix | 22 | 75 (63 without) |
  | Tar, the same | 20 | 52, and 14 enemy-seconds slowed a shell |
  | Cryo, the same | 15 | 29, and 4 freezes a shell |

  With Shrapnel, Fusion wins a crowd and Rich Mix one enemy, and Fallout's Dead Zone makes Fusion the pick against
  enemies that heal or shield. With Concussion, Fusion leads a crowd but Rich Mix Dazes nearly three times as often:
  half its shells are Heavies, against a sixth, and a Nuke Dazes nothing. The payload builds win a crowd and lose one
  enemy; Tar and Cryo trade damage for control, about even with each other.
- **Against the Sniper**, the single-target yardstick (the Mortar was meant as the group one; that is in doubt). Its tree multiplies its damage
  about as much as the Sniper's does (×3.5-4.2 in a crowd, ×3.4), so the price sets the match: at $20, a Mortar's
  damage to a crowd a dollar comes within about 10% of a Sniper's on one enemy, bare or upgraded. It matches from
  enemies about a cell apart; denser waves, slow enemies and corners favour it, and a lone enemy takes about a third.
- **Its names**: Crack, the magazine, Heavy, Daze, Bleed, Payload (Napalm, Tar or Cryo), Flare and Reveal, Barrage
  or Carpet Bombing, Nuke and Fallout. Its enemy debuffs are Crack, Daze or Bleed, and its Payload's and Fallout's
  ground.

### Bare tower

| Stat | Value | Today | Replaces |
|---|---|---|---|
| Price | $20 | $30 | |
| Damage | 32 | | |
| Attacks | every 3.5 s | | |
| Range | 4.5 cells | | |
| Minimum range | 1.5 cells | | |
| Blast | 1 cell at 100% of the shell's damage | 1.75 cells, falling off with distance | |
| Blast edge | 1.75 cells at 50% | | |
| Targets | First | | |
| Magazine | 10 shells: Normal (weight 9) and Heavy (weight 1). A new tower's reads Normal, Normal, Heavy, four Normal, Heavy, two Normal. | | mortar.head.siege_rounds.3 |
| Passive | Crack: every shell Cracks what its blast reaches. | | |

### Tiers

| Tier | Gate | Unlock | Colour |
|---|---|---|---|
| 0 | - | - | #c39062 |
| I | XP 60 | $20 | #e0b070 |
| II | XP 120 | - | #ebcba1 |
| III | XP 200 | - | #f5e0b8 |

### Upgrade tree

| Tier | Node | Kind | Cluster | After | Glyph | Colour | Cost | Replaces | Does | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | Elevation | stat | | | base.range | | | base.range | Range +1 cell | |
| 0 | Heavy Charge | stat | | | mortar.head.siege_rounds.1 | | | mortar.head.siege_rounds.1 | Damage +10 | |
| 0 | Big Bore | stat | | | mortar.head.siege_rounds.2 | | | mortar.head.siege_rounds.2 | Heavy shells come up twice as often. | Heavy weight 1 to 2. |
| 0 | Predictive Fire | ability | | | mortar.extra.ballistics.2 | | | mortar.extra.ballistics.2 | Each shell aims where its target will be when it lands. | The Mortar's weakness is that fast enemies walk out of an unguided shell, so the fix comes first. |
| I | Concussion | ability | Heavy shell | | Daze | | | mortar.head.siege_rounds.3 | A Heavy shell Dazes every enemy its blast reaches: for 1 s in its 100% zone, 0.5 s at its edge. | Control. |
| I | Shrapnel | ability | Heavy shell | | mortar.head.fragmentation_rounds.4b | | | mortar.head.fragmentation_rounds.4b | A Heavy shell makes every enemy its blast reaches Bleed. | Damage that grows with how far the enemy walks, so it hurts the fast ones that dodge shells. Today's Shrapnel Storm. |
| I | Napalm | ability | Payload | | mortar.special.napalm | | | mortar.special.napalm | Adds Napalm shells to the magazine; each leaves burning ground. | Weight 3. The damage pick. |
| I | Tar | ability | Payload | | mortar.special.tar | | | mortar.special.tar | Adds Tar shells to the magazine; each leaves tar that slows. | Weight 3. The crowd-slow pick. Its poison is half today's (1.5% of damage a tick, from 3%), so it deals less than Napalm, and its tar lasts 3 s, from 4, as every payload's ground. |
| I | Cryo | ability | Payload | | mortar.special.cryo_shells | | | mortar.special.cryo_shells | Adds Cryo shells to the magazine; each leaves frost ground that freezes an enemy that stays in it. | Weight 3. The stop pick. |
| I | Flare | ability | | | Reveal | | | | Adds Flare shells to the magazine; each leaves a flare that burns until the wave ends and Reveals every enemy that enters it. | Weight 1. Good where enemies turn invisible, a small cost elsewhere: each Flare takes a slot from a shell that deals damage. |
| II | Barrage | ability | Volley | | mortar.extra.ballistics.4 | | | mortar.extra.ballistics.4 | Loads the 3 front shells at once and fires them one after another at one target; reloads slower. | Against a boss. |
| II | Carpet Bombing | ability | Volley | | mortar.head.fragmentation_rounds.4a | | | mortar.head.fragmentation_rounds.4a | Loads the 3 front shells at once and lays them along the path behind the target; reloads slower. | Against a crowd: the shells fall on the enemies that follow the target. Today's bombs fall ahead of the impact. |
| II | Overcharge | stat | | | mortar.head.siege_rounds.4b | | | | A Heavy shell deals ×3 damage, up from ×2. | The Heavy's strength. |
| II | Big Bore II | stat | | | mortar.head.siege_rounds.2 | | | | Heavy shells come up twice as often again. | Heavy weight 2 to 4. The Heavy's frequency. |
| II | Thick Mix | stat | | Payload | mortar.head.fragmentation_rounds.2 | | | mortar.head.fragmentation_rounds.2 | Payload shells' blast +0.5 cells, and their ground with it. | The Payload's strength. Name is a proposal. |
| III | Fusion | ability | Order | | mortar.head.siege_rounds.4a | | | mortar.head.siege_rounds.4a | A Heavy shell drawn right behind a Heavy fuses with it into a Nuke, which leaves Fallout. | Bursts, and the pick against enemies that heal or shield. |
| III | Rich Mix | ability | Order | | mortar.head.fragmentation_rounds.3 | | | mortar.head.fragmentation_rounds.3 | A new shell is never the same kind as the one in front of it. | Variety: a payload magazine is always mixed, and a heavy one with only Normal and Heavy alternates. |
| III | Lasting Mix | stat | | Thick Mix | mortar.extra.ballistics.3 | | | mortar.extra.ballistics.3 | Payload ground lasts 1 s longer. | 3 s to 4 s. The Payload's duration, beside Thick Mix's size. Name is a proposal. |
| III | Deep Crack | stat | | | Crack | | | | Crack takes 10 points of plating, up from 5. | |
| III | Quick Reload | stat | | | mortar.extra.ballistics.1 | | | | Attacks every 3 s, down from 3.5 s. | |
| III | Elevation II | stat | | | base.range | | | base.range.2 | Range +1 cell | |

### Terms

New or changed meanings of names the rows use. A name with no row here reads as the game says it today.

| Term | Glyph | Means |
|---|---|---|
| Magazine | mortar.head.siege_rounds.3 | The shells the Mortar has ready, in the order it fires them, shown in the tower pane. A new slot at the back draws its kind by weight: each kind's chance is its weight over the sum of the weights of every kind the tower owns. The front shell is the one loading. |
| Shell | | One round in the magazine. It carries its own damage, blast, flight speed and reach; hovering it in the pane shows them. |
| Heavy | mortar.head.siege_rounds.3 | A shell that deals ×2 damage (×3 with Overcharge) in a Normal's blast, with a slower flight. |
| Nuke | mortar.head.siege_rounds.4a | Two Heavies fused: a shell of its own kind with the damage of both, at 100% to 2 cells and 75% to 2.75 cells, leaving Fallout. It never fuses again, and Concussion and Shrapnel don't apply to it. |
| Fallout | | Ground a Nuke leaves over most of its blast for 4 s, as today. It applies Dead Zone and Sicken, as today, and Poison of 2% of the Mortar's damage a tick. |
| Payload | mortar.special.napalm | The kind of special shell a Mortar adds: Napalm, Tar or Cryo, one at a time. A payload shell deals half a Normal's damage in a Normal's blast and leaves its ground where it lands, for 3 s. |
| Flare | Reveal | A shell with a blast of 0.75 cells at 100% that leaves a flare on the ground, 1.5 cells in radius, until the wave ends. Every enemy that enters it is Revealed for 3 s. A Mortar keeps its newest 3 flares; an older one goes out. |
| Bleed | | Physical damage for every cell the enemy walks, past armor, plating and shields; a stopped enemy takes none. A Heavy's Bleed costs 5% of the shell's damage a cell, for 4 s. |
| Barrage | mortar.extra.ballistics.4 | Three shells loaded together and fired one after another at one target; a later one picks a new target if its target died. |
| Carpet Bombing | mortar.head.fragmentation_rounds.4a | Three shells loaded together and laid along the path, starting at the target and going back toward where the enemies come from. |
| Crack | | Takes 5 points of plating and 5 of insulation (10 of plating with Deep Crack). The only debuff in the game that takes plating. |

### Look

A squat mortar with a short, wide barrel. It keeps its brown, `#c39062`. Drawn in the art pass (2026-11-13), in the raised view with a footprint that grows with the tier (`FEATURE-next-gen-tower-looks.md`, "Art direction"). The sheets show exactly what to build: [every tier](next-gen-art/catalogue.png), [on the board](next-gen-art/board.png), [the moving part and its cues](next-gen-art/heads.png), [shots and casts](next-gen-art/shots.png). Every measure, in cell units, is in `.claude/skills/run-jtd/TowerArt.java`, `mortar`.

| Tier | Foundation |
|---|---|
| 0 | a round brown base with an iron turntable on it; no plinth |
| I | one octagonal plinth step; a ring of ten sandbags round the base |
| II | two steps; the sandbags round a riveted iron turntable with a brass ring |
| III | three steps; a small bastion: an octagonal stone wall with eight merlons round a dark yard, three shells racked along the wall, the turntable inside |

- **Head**: the barrel, turning to its aim: short and wide (0.21 of a cell long, 0.135 wide), a lighter muzzle band,
  and the bore open at its tip. A range node lengthens it to 0.25.
- **Reload**: on the shot the barrel slides back 0.05 of a cell behind a flash and the bore is empty; it slides out
  again over the reload. The draft's other idea, an arc growing round the barrel, is dropped: one cue is enough.
- **The loaded shell** sits in the bore from half the reload on, in its kind's look. With a Volley, a tray behind the
  breech holds three shells in a row.
- **Shell kinds**, each the same drawing in the bore, in the pane's magazine and in flight:

  | Kind | Look |
  |---|---|
  | Normal | a dark iron ball `#7b8496` with a pale glint |
  | Heavy | a quarter larger, a bright band `#f2d28a` across it |
  | Nuke | half again as large, pale `#e8e8dc` with a dark triangle, in a yellow-green hazard glow `#c8f04a` |
  | Napalm | red-orange `#ff7a2a` with a yellow core; a flame trail in flight |
  | Tar | black `#241d2a` and glossy, a pale violet rim and one white glint |
  | Cryo | pale blue `#aae6ff` with a white four-point star; a frost trail in flight |
  | Flare | a white-yellow four-point spark in a small glow; a short trail in flight |

- **Shot**: a lobbed projectile (`FEATURE-next-gen-shot-flight.md`) in its kind's look, with the same grey veil for
  a shadow as the Burst's stone. The 100% zone is a white ring at the centre, the 50% edge fainter; a Nuke's 75% zone
  is a yellow-green ring between them.
- **The magazine** in the tower pane: a row of slots, front first; the loading slot (or three, with a Volley) is
  marked loaded. Under it, one strip split into the kinds' colours with each kind's chance ("Normal 60%, Heavy
  40%"). A node's hover shows the chance it changes on this tower ("Heavy 18% → 31%"). Rich Mix's chances are what
  the draws come to over time, not one draw's odds.
- **Fusion** merges the two Heavies in the pane visibly; the Nuke keeps today's flash.
- **Concussion**: today's daze on every enemy hit. **Shrapnel**: fragments flying out over the blast, then today's
  bleed marker.
- **Payload grounds** keep today's burning ground, tar, frost ground and fallout ([grounds](next-gen-art/grounds.png)).
- **A flare** burns as one small spark in a faint, wide glow on the ground, calm beside the payload grounds.

## Interconnections

- `FEATURE-next-gen-upgrade-tiers.md` (its node rules), `FEATURE-next-gen-tower-looks.md`,
  `FEATURE-next-gen-shot-flight.md` (the lobbed projectile). It uses no abilities
  (`FEATURE-next-gen-tower-abilities.md`).
- The blast's zones are the Burst's (`FEATURE-next-gen-splash-tower.md`).
- The Mortar's crit reads as its Heavy: today's `CRIT_CHANCE` and `CRIT_DAMAGE` may become the Heavy's weight and
  damage, rather than new stats.
- Bleed is the Mortar's alone today: each source sets its damage a cell (`Effect.bleeding`), and the Mortar's is a
  share of its damage. It lands as periodic damage, so shields don't hold it.
- Fallout is today's `ZoneKind.FALLOUT` (`ShellType.NUKE`'s ground), which gains a Poison as Tar's ground has one.
- `td/tower/mortar/CLAUDE.md` describes Bracketing, `ShellPlan.typeOf` and the special shell's fixed number; those
  lines change with the magazine.
- ADR 0021 (a wave starts from its save) and the save text (`GameSaveText`): the magazine is saved (Decisions made).

## Decisions made

- **No abilities and no currency.** An earlier note gave the Mortar the Burst's bar; a second economy like Sparks
  and Souls would be one more thing to learn, and the magazine already sets the tower apart. The Tactical Nuke
  comes from Fusion, the Flare from the magazine.
- **No fork.** A fork without abilities would only change which shells come up, two lists of stats. The magazine
  makes the split itself: heavy and payload builds compete for the same slots, and a hybrid stays possible.
- **The magazine draws by weight.** Each new kind dilutes the others, so a payload build gets fewer Heavies, and a
  heavy build can reach 40% or more only by leaving other kinds out. The panel shows chances, never weights.
- **Zones, not falloff.** Today's damage falls with the square of the distance, to almost nothing at the edge
  (half on average). Zones say exactly what a shell deals and match the Burst: 100% to 1 cell, 50% to 1.75 cells.
- **The Heavy is a Normal that hits harder**: ×2, ×3 with Overcharge, in the same blast. At ×1.5 Overcharge doubled
  it alone; at ×2 Overcharge adds 15-24% to a heavy build, beside Big Bore II's 11-19%.
- **A Nuke is two Heavies summed** in a blast of its own, mostly at 100% (to 2 cells, then 75% to 2.75), about the
  size of today's, and never fuses again. It reaches about 1.9 times as much path as a Normal.
- **Fusion and Rich Mix are one cluster.** Rich Mix first lowered Normal's weight, which helped every build alike.
  Never drawing a kind twice in a row gives a payload build a mixed magazine, but with only Normal and Heavy owned
  it alternates them, half Heavies from one node; and it makes Fusion impossible. As one pick, it is the heavy
  build's steady road beside Fusion's bursts.
- **Concussion and Shrapnel are one cluster**: both answer what a Heavy adds, control or damage. Bought apart, a heavy
  build would take both and decide nothing. Their Daze (a second at most) barely cuts a Bleed (4 s).
- **Concussion Dazes the whole blast**, not only the enemy under the landing point, and follows its zones: 1 s in
  the 100% zone, 0.5 s at the edge. Half a second everywhere was a stop of about 7% of the time on a steady build.
- **Shrapnel Bleeds** instead of throwing a ring past the blast, so Bleed stays in the game. 5% a cell, down from
  today's 8%: with Overcharge, 8% came to about 144% of base damage on every enemy hit.
- **Flare stays apart from the Payload pick**: a flare reveals, the payloads hurt or hold. At weight 1 it costs a
  build about one damaging shell in ten.
- **Predictive Fire is in Tier 0**: it mends the Mortar's own weakness, and it makes room in Tier I.
- **The opener is fixed only when the tower is built.** The magazine is not redrawn between waves.
- **Flare only Reveals**, from a flare that stays until the wave ends: a fixed watch post over the path rather
  than one moment, so the enemies that turn invisible later in the wave are caught too. Expose would be a fourth
  debuff and a tenth name.
- **Volley is Barrage or Carpet Bombing**: one target or a crowd. Long range stays in Elevation.
- **The Payload is a road**: Thick Mix comes after Payload and Lasting Mix after Thick Mix, since neither does
  anything without a payload. The Heavy's nodes need no After: every magazine holds Heavies.
- **The save keeps the magazine**, shell by shell; how the save's text holds it is the plan's. A magazine kept across waves is state a wave builds that the next one starts from, which ADR 0021
  allows only for a player choice the save keeps, so the feature adds an ADR naming the magazine as build state.
  Redrawing it at each wave start would keep ADR 0021 as it is, but lose the magazine the player saw at the clear.
- **A shell the tower no longer owns leaves the magazine** (a downgrade, a Payload switched between waves): the
  shells behind it move up, and new ones are drawn at the back.
- **Rich Mix makes Big Bore idle in a magazine of only Normal and Heavy**, since it alternates them. A heavy player
  buys Big Bore and Big Bore II before Tier III, where they feed Heavies; picking Rich Mix there trades the Nuke for
  a steady half, and Big Bore can be sold between waves. The panel's chance strip shows why ("Heavy 50% → 50%").
  Rich Mix needing Big Bore would make the player buy a node that then does nothing. Rich Mix as "Normal never twice
  in a row" keeps the weights working, but lets Heavies follow each other, so it no longer rules Fusion out, and
  gives 59% Heavies from one node.
- **A Nuke takes neither Concussion nor Shrapnel**: it is a kind of its own, and its strength is the blast.
- **A Nuke leaves Fallout**, today's ground over today's blast, with Dead Zone and Sicken as today. Its huge area is
  the point. Without it, Rich Mix won every case, as half its shells are Heavies carrying the Heavy-shell pick; a
  Poison in the Fallout gives Fusion the crowd, and Dead Zone the enemies that heal. A 2× blast with the Heavy-shell
  pick on the Nuke would have done it too, but it reverses the decision above and makes the blast larger still.
  The Poison is 2% of damage a tick: at 2.5% with the Nuke's 100% and 75% zones, Fusion led a crowd by up to 27%.
- **Tar's poison is halved**: at today's 3% its ground dealt more than Napalm's (about 520 to 400 a shell in a
  crowd) and slowed too, so the slow pick beat the damage pick. Napalm keeps today's numbers.
- **Every payload's ground lasts 3 s, 4 s with Lasting Mix.** Tar drops from 4 s. At 4 s for all and 5 s with
  Lasting Mix, a Napalm build dealt about 25% more than a heavy build to a crowd; at 3 s and 4 s, about 10%.
- **Lasting Mix replaces Light Shells**, whose extra reach overlapped Elevation II. A strike of the ground on every
  enemy the blast reaches (+40% for Napalm), a payload weight of 4 (+10% under Rich Mix, which already spaces the
  shells) and full damage for payload shells (+12%) were the other candidates.
- **The Mortar costs $20, from $30.** At $30 its damage to a crowd a dollar was 60-78% of a Sniper's on one enemy,
  bare or upgraded, with its longer range counted. A third off its price fixes the base the tree multiplies,
  whatever the nodes come to cost.
- **Dropped**: Bracketing and its purpose gate, Siege Rounds as a line, Fragmentation Rounds, Cluster Shell and its
  bomblets, Bunker Buster, Rifled Barrel, Airburst.

## Open questions

1. ✅ **Tier gates.** XP 60, 120 and 200, as every tower (`FEATURE-next-gen.md`, "Answered by the user").
2. ✅ **Shell numbers.** The Heavy's flight, a payload shell's speed, the Nuke's flight and the Volley's slower reload
   are placeholders the balance pass sets, by "Balance waits" (2026-11-14, autonomously).
3. ✅ **Many flares.** Each Mortar keeps only its newest 3; an older one goes out (final review: a dozen flares a
   wave is against the calm board, and each is a patch the frame build pays for).
4. ✅ **The Payload pick** reads `rebalance` on paper (Cryo $1 against $19). It is kept as drafted and retuned once
   the Mortar is built (the user, 2026-11-14).
5. ✅ **The Dead zone's name.** The stat is Minimum range (the user, 2026-11-13); Fallout's Dead Zone keeps its
   name.
