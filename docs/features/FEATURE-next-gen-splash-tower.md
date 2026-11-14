# Feature Request: Next-gen Burst Tower

**Status:** implemented 2026-11-14; drafted 2026-11-11, reviewed with the whole bundle 2026-11-13. Tiers 0 to III, both sides drawn to the node
rules, and the look, from the visual request's Burst section (`FEATURE-next-gen.md`, appendix) and its handwritten
notes. Part of `FEATURE-next-gen.md`.

## Summary

The Burst (`SplashTower`) moves to the next-gen tier tree. It stays a cheap area tower that lobs stones at random
enemies; each stone explodes where it lands. Saturation and damage that falls off with distance go: damage comes in
zones, each a share of base damage. Tier I splits it into a Stormcaller or a Hexer, each with its own passive, a
currency the passive earns, and abilities that spend it. The hole artwork idea is dropped; the palette is retuned with
the other towers.

## What this feature adds

The tables below are read by `td.ui.PreviewUpgrades` (run-jtd), which draws the tree beside today's panel and lists
what the nodes no row replaces leave unused. Stormcaller or Hexer is a fork gate (`FEATURE-next-gen-upgrade-tiers.md`):
the pick sits in Tier I's gate, and Tiers I to III then show the nodes both sides share in a narrow column on the
left and the chosen side's in a column on the right. The column rules are in run-jtd's `SKILL.md`.

### Abilities (Stormcaller and Hexer)

They follow `FEATURE-next-gen-tower-abilities.md`: the tower earns its currency (Sparks, Souls) by fighting, and
whatever it earns, a whole unit or a fraction, goes into every owned ability's own bar at once. A cast never replaces
the base shot, so the stones keep flying. The base shot earns by the damage it deals, 1 Spark or Soul per 200; the
passive pays a large share at once.

Every number the player or a tuning edit meets is in Sparks and Souls: an ability's cost, a passive's payout, the
damage that makes one unit. There is no tower bar and no hidden capacity; whatever finer count the code keeps is never
shown and no tuning number is written in it.

### Bare tower

| Stat | Value | Today | Replaces |
|---|---|---|---|
| Price | $15 | | |
| Damage | 14 | | |
| Attacks | every 1 s | 0.95 s | |
| Range | 2.5 cells | 3.2 | |
| Splash | 0.75 cells at 100%, drawn as the white ring where the stone lands | | |
| Splash edge | 1.25 cells at 50% | | |
| Targets | Random | | |
| Passive | None: Stormcaller and Hexer each bring their own in Tier I. | | |

### Tiers

| Tier | Gate | Unlock | Colour | Fork |
|---|---|---|---|---|
| 0 | - | - | #e2746c | |
| I | XP 60 | $20 | #8496ff #b88ae8 | Stormcaller or Hexer |
| II | XP 120 | - | #c0c9ff #dbc3f3 | |
| III | XP 200 | - | #e0dee8 #eddce2 | |

### Upgrade tree

| Tier | Node | Kind | Cluster | After | Glyph | Colour | Cost | Replaces | Does | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | Long Throw | stat | | | base.range | | | base.range | Range +0.7 cells | |
| 0 | Wide Charge | stat | | | splash.wide_charge | | | splash.wide_charge | Splash 1.0 cell at 100% of base damage<br>Splash edge 1.75 cells at 50% | The Mortar's Normal blast; its Heavies and Nukes stay the wider ones. |
| 0 | Flint Stones | stat | | | splash.flint_stones | | | splash.flint_stones | Armor penetration +10 on the base shot | |
| I | Stormcaller | ability | Stormcaller or Hexer | | splash.stormcaller | #8496ff | | splash.stormcaller | Passive, Static: every enemy its base shot or Arc damages (past plating, shield and the rest) carries a stack, one per Stormcaller that hit it.<br>At 10% health or less it discharges 15 magic damage per stack into that enemy alone. A thin, quiet spark runs to each Stormcaller that tagged it and gives it Sparks: a quarter of a Spark for an enemy under 150 max health, a half under 500, 1 Spark under 1500, and 2 from 1500 up. | Was Ground Fault. "Charge" is taken by the Charged effect, so the currency is called Sparks here. |
| I | Hexer | ability | Stormcaller or Hexer | | splash.hexer | #b88ae8 | | splash.hexer | Passive, Soulbrand: every enemy its base shot damages carries a stack, one per Hexer that hit it.<br>When it dies it bursts for 10 magic damage per stack within 1 cell; the burst tags nothing. Its soul flies to each Hexer that tagged it and gives it Souls: a quarter of a Soul for an enemy under 150 max health, a half under 500, 1 Soul under 1500, and 2 from 1500 up. | Was Soul Tithe. |
| I | Arc | ability | | Stormcaller | splash.arc | #8496ff | 2 Sparks | splash.arc | Once full, the next base shot carries an Arc. | The Stormcaller's core: most of its nodes change the Arc. |
| I | Hex of Doom | ability | Hex | Hexer | splash.hex_of_doom | #b88ae8 | 2 Souls | splash.hex_of_doom | Curses one enemy with Doom. | The damage hex. |
| I | Hex of Blight | ability | Hex | Hexer | splash.hex_of_blight | #b88ae8 | 2 Souls | splash.hex_of_blight | Curses one enemy with Blight, one without it first. | The mass-control hex: it spreads before it deepens. Today's Blight is Poison only, and its popup never says how long it lasts. |
| II | Deep Doom | stat | | Hex of Doom | splash.deep_doom | #b88ae8 | | splash.deep_doom | Doom returns 45% of the damage, up from 30%. | |
| II | Blight Cloud | stat | | Hex of Blight | splash.blight_cloud | #b88ae8 | | splash.blight_cloud | Blight also curses every enemy within 1 cell of its target. | Mass control instead of a deeper slow, from Tier II, so Blight wins a crowd as soon as Doom wins one enemy with Deep Doom. Name is a proposal. |
| II | Agony | stat | | Hex | splash.agony | #b88ae8 | | splash.agony | A hexed enemy is Vulnerable while its hex lasts. | For either hex: Doom pays out more, Blight turns control into damage. |
| II | Contagion | stat | | Hex | splash.contagion | #b88ae8 | | splash.contagion | When a hexed enemy dies, its hex jumps to the nearest enemy without one, as it is. | Replaces Restless Doom and today's Spreading Curse. A jump carries the hex as it is (Decisions made). |
| III | Soul Harvest | stat | | Hexer | splash.soul_harvest | #b88ae8 | | splash.soul_harvest | The base shot earns Souls twice as fast: 1 Soul per 100 damage. | The Soul economy's one tuning node. Name is a proposal. |
| III | Hex of Inversion | ability | | Hexer | splash.hex_of_inversion | #b88ae8 | 3 Souls | splash.hex_of_inversion | Curses one enemy that heals, shields, vanishes or regenerates with Inversion. | Kept from today's tree: a counter with a target of its own. |
| III | Choking Blight | stat | | Hex of Blight | splash.choking_blight | #b88ae8 | | splash.choking_blight | Blight also Silences its enemy while it lasts. | |
| III | Twin Hex | stat | | Hex | splash.twin_hex | #b88ae8 | | splash.twin_hex | Each hex curses 2 enemies. | Name is a proposal. |
| II | Chain Lightning | stat | Arc's path | Arc | splash.chain_lightning | #8496ff | | splash.chain_lightning | Arc jumps 3 more times. | Spread: the mass pick. |
| II | Wild Arc | stat | Arc's path | Arc | splash.wild_arc | #8496ff | | splash.wild_arc | Arc may jump to an enemy it already hit. | Focus: with few enemies near, it zigzags between them, or strikes a lone one again and again. Today's Lightning Rod, renamed to its new meaning. Name is a proposal. |
| II | Conductor | stat | | Arc | splash.conductor | #8496ff | | splash.conductor | Arc's jumps can crit: Crit chance 15%, rolled per jump. | |
| II | Short Circuit | stat | | Arc | splash.short_circuit | #8496ff | | splash.short_circuit | Arc turns Armor against its enemy. | Armor raises Arc's damage by the share it would cut from a physical hit: Armor / (100 + Armor), so +30% at Armor 43 and never double. Magic resist and insulation still apply. Plating went: it added its points to each hit, up to five times a small jump, and magic already passes plating. A node of its own, not the Arc's nature (open question 1). |
| III | Thunderclap | ability | Storm | Stormcaller | splash.thunderclap | #8496ff | 3 Sparks | splash.thunderclap | Once full, discharges into every enemy in range for 40% of base damage as magic. | The mass pick; it looked good on big waves. |
| III | Thunderstrike | ability | Storm | Stormcaller | splash.thunderstrike | #8496ff | 3 Sparks | splash.thunderstrike | Once full, calls lightning onto the healthiest enemy in range for 200% of base damage as magic. | The boss pick: 28, or 42 with Heavy Stones. Thunderclap (40% to each enemy in range) matches it from 5 enemies up. |
| III | High Voltage | stat | | Arc | splash.high_voltage | #8496ff | | splash.high_voltage | Arc's jumps deal 60% of base damage, up from 40%. | |
| III | Feedback | stat | | Conductor | splash.feedback | #8496ff | | splash.feedback | An Arc crit gives half a Spark. | One sentence for the loop crit, Spark, Arc. A whole Spark gave back about half of each Arc's cost and added about 40%. The Stormcaller's counterpart to Soul Harvest. |
| III | Quick Hands | stat | | | splash.quick_hands | | | splash.quick_hands | Attacks every 0.85 s, down from 1 s. | For either fork. Name is a proposal. |
| II | Heavy Stones | stat | | | splash.heavy_stones | | | splash.heavy_stones | Damage +7 | For either fork. +50%: every ability is a share of base damage, so this node raises them all. |
| II | Long Throw II | stat | | | base.range.2 | | | base.range.2 | Range +1 cell | For either fork. |
| III | Piercing Stones | stat | | | splash.piercing_stones | | | splash.piercing_stones | Armor penetration +40 on the base shot | For either fork. With Flint Stones +50, an Elite Golem's armor: +17% against a Knight, +44% against an Elite Knight; a Boss Golem keeps 15. Name is a proposal. |

### Terms

New or changed meanings of names the rows use. A name with no row here reads as the game says it today.

| Term | Glyph | Means |
|---|---|---|
| Static | splash.head.arc.1 | The Stormcaller's passive. Every enemy its base shot or Arc damages carries a stack, one per Stormcaller that hit it.<br>At 10% health or less the enemy discharges 15 magic damage per stack into itself, and gives Sparks to each Stormcaller that tagged it: a quarter of a Spark for an enemy under 150 max health, a half under 500, 1 Spark under 1500, and 2 from 1500 up. |
| Arc | | Lightning a charged base shot carries from where the stone lands. It jumps from enemy to enemy, 3 times, to the nearest within 1.5 cells that it hasn't hit, each jump a magic hit for 40% of base damage. It applies Static as the base shot does. Jumps land one after another with visible pauses. |
| Soulbrand | splash.head.hex.1 | The Hexer's passive. Every enemy its base shot damages carries a stack, one per Hexer that hit it.<br>When the enemy dies it bursts for 10 magic damage per stack within 1 cell, and gives Souls to each Hexer that tagged it: a quarter of a Soul for an enemy under 150 max health, a half under 500, 1 Soul under 1500, and 2 from 1500 up. |
| Spark | Charge | The Stormcaller's currency. The base shot earns 1 Spark per 200 damage it deals, in fractions as it goes; Static pays at once by the dead enemy's max health. Every Spark and every fraction goes into each of the Stormcaller's abilities at once; one that reaches its cost casts: the Arc rides on the next base shot, a Storm discharges by itself. |
| Soul | Soulfire | The Hexer's currency. The base shot earns 1 Soul per 200 damage it deals, in fractions as it goes; Soulbrand pays at once by the dead enemy's max health. Every Soul and every fraction goes into each of the Hexer's abilities at once; one that reaches its cost casts. |
| Ability bar | | Each ability's own bar, shown only in the tower pane. It is as long as the ability's cost plus 1 unit, a faint Spark or Soul glyph behind each unit and a mark where one ends and the next begins; the fill shows the whole units and the fraction in. The ability casts once its cost is in and pays it from the bar; what is left stays. The extra unit keeps what comes while it waits for a target or a big payout lands at once; past a full bar the rest is lost. A wave starts with 1 unit in each. |
| Doom | | A hex for 4 s. When it ends, 30% of all damage the enemy took meanwhile (45% with Deep Doom) hits it again as magic. |
| Blight | | A hex for 4 s: Poison of 3% of base damage a tick, from today's 4%, and a slow that grows steadily from none to Speed -30% at the end. It grows with time, so a hex that Tenacity shortens slows less, and one it lengthens slows more.<br>Cast again on an enemy that has it, the time starts over and the slow never drops: it holds until the new hex's slow passes it. |

### Look

The earlier idea of a hole in the ground is dropped. The tower is defined by its split: a cheap stone thrower that
becomes an electric contraption, a tesla coil, or a witch's cauldron. Drawn in the art pass (2026-11-13), in the raised view with a footprint that grows with the tier (`FEATURE-next-gen-tower-looks.md`, "Art direction"). The sheets show exactly what to build: [every tier](next-gen-art/catalogue.png), [on the board](next-gen-art/board.png), [the moving part and its cues](next-gen-art/heads.png), [shots and casts](next-gen-art/shots.png). Every measure, in cell units, is in `TowerFoundations.thrower`, `stormcaller` and `hexer`, built from a `TowerPose`.

**In code** (`FEATURE-next-gen-tower-looks.md`, "Building a tower's look"). The looks are `TowerLook.SPLASH`,
`STORMCALLER` and `HEXER`, picked by `bodyLookOf(SplashTower)` from the fork (wired today). None aims: the body is
built from the pose.

| What the tower holds | In the pose |
|---|---|
| the throw and the reload (arm, spark, bubble) | `poseOfReload(coolDownFraction)` (sent today) |
| an ability full and waiting for a target | `TowerCue.CHARGED` |

- The arm's new stone, the spark and the bubble all come from the one reload value; no cue of their own.
- The stone in flight, the Arc's hops and the casts are `FEATURE-next-gen-shot-flight.md`'s and are not built.

- **A fork changes the look completely.** The fork is picked at Tier I's gate, so the plain stone thrower is Tier 0's
  alone. Colours: the thrower `#e2746c`, the Stormcaller `#8496ff`, the Hexer `#b88ae8`.

  | Tier | Stone thrower | Stormcaller | Hexer |
  |---|---|---|---|
  | 0 | a ring of nine stacked stones, two courses, round a dark pit; an A-frame holds a throwing arm that lies back to the south-west with a stone in its cup; no plinth | | |
  | I | | one round plinth step; the stone ring, one course, with a three-turn copper coil standing in it and a blue ball on top | one step; the stone ring holding an iron cauldron of violet brew, three bubbles on it |
  | II | | two steps; an octagonal riveted iron plate, a four-turn coil, one blue crown ring round its top | two steps; the cauldron raised on a tripod, a rune stone between each pair of legs |
  | III | | three steps; the plate, six white insulators round a five-turn coil, two crown rings and the ball | three steps; a rune circle on the top step, the rune stones, and three bone tusks arching round the cauldron |

  The Hexer's Tier III was a hut of bones and hide in the draft; a roof would hide the brew, which is the part that
  moves, so the tusks stand in for it.
- **The moving part** (no turning head, question 5): the thrower's arm flicks forward on each throw and lies back
  as it reloads, a new stone appearing in its cup at half the reload; the coil's ball sparks once, four short jagged
  lines; the brew throws up one big bubble and three drops. Nothing moves between throws.
- **A full ability waiting for a target**: the coil's ball turns white in a steady blue glow; the brew turns pale
  and glows violet. Steady, not pulsing, until it casts.
- **Shot**: a stone thrown on a high arc at the (random) target, with a soft grey veil under it on the ground for a
  shadow (the floors are near-black, so a dark shadow would not show). The 100% zone is a white ring at the centre,
  the 50% edge a fainter one. It is unguided: it lands where its target stood when it was thrown
  (`FEATURE-next-gen-shot-flight.md`, 2). A forked tower's stone takes the fork's colour; the Arc's charged stone is
  near-white in a blue glow.
- **Static** discharging has its own visual on the enemy; a thin, quiet spark, a third the width of an Arc's hop,
  runs to each Stormcaller that tagged it.
- **Soulbrand**: the dead enemy's soul, a pale wisp with a fading tail, flies from where it died to each Hexer that
  tagged it.
- **Arc**: the chain hops from enemy to enemy with visible pauses, each hop a white bolt in a blue halo, the hop
  before it fading; a crit's hop is twice as thick.
- **Wild Arc**: the zigzag reads as one lightning bouncing back and forth between two enemies.
- **Thunderclap**: one ring at the range's edge and short bolts from the coil out to it. **Thunderstrike**: a thick
  bolt from above the board onto its enemy, a white ring where it lands.
- **Hex casts** fly as curses: a dark-hearted bolt with a wisp behind it, in the colour of its effect's marker
  (Doom, Blight, Inversion), so a curse in flight and the curse it leaves match.
- **A hexed enemy** wears three arcs of a broken rune circle in the hex's colour, and the hex's sign over its head
  ([marks](next-gen-art/marks.png)). **Blight**: the arcs thicken and a tint fills the circle as the slow grows.
- **Contagion**: the hex flies visibly, as a curse, from the dead enemy to the next.
- **Ready abilities**: the board shows at most which abilities wait, full, for a target; the ability bars stay in the pane.

## Interconnections

- `FEATURE-next-gen-upgrade-tiers.md`, `FEATURE-next-gen-tower-looks.md`, `FEATURE-next-gen-shot-flight.md`,
  `FEATURE-next-gen-tower-abilities.md`.
- `shipped/FEATURE-effect-simplification.md` left Rime and Ash as one negative-Tenacity hex until the hexes were
  rethought; this doc drops them with Malaise.
- Mines (Range III) and Potency and Mastery go with today's tree, along with their looks.

## Decisions made

- **The Hexer follows "How a node reads".** Two hexes form one cluster, Doom for damage and Blight for control;
  every other Hexer node changes those two. Inversion stays as the one later hex.
- **Dropped hexes.** In play, most of today's hexes felt confusing and of little use: each changed some other effect
  instead of doing one visible thing. Gone: Malaise and the Rime and Ash Covens (Tenacity is a number the player
  never sees), Sympathy (no one can see who is linked), Reckoning and Doom Burst (a combo on a combo), Terror (Spirit
  and Sicken), and Agony and Silence as hexes of their own (Vulnerable and Silence come from nodes).
- **The Hexer's names**: Soulbrand, Soul, the ability bar, Doom, Blight, Vulnerable, Silence, Contagion, Inversion. Its
  enemy debuffs are Vulnerable and Silence.
- **The currency comes from damage too.** The passive alone earned too little, too late, and the base shot added
  nothing; Soul Harvest's crits tried to patch that. The base shot now earns by its damage and the passive pays a
  large share.
- **Each ability has its own bar; the tower has none.** A tower bar that turned into units which then charged the
  abilities was two steps for one thing. Now what the tower earns goes into every ability bar at once: 50 damage of
  the base shot is a quarter of a Spark in each. A bar is as long as its cost plus 1, so a 3-Spark ability's is longer than a
  2-Spark one's, and the units are drawn on it, so the game speaks only in Sparks, Souls and their fractions.
- **A bar holds 1 unit more than its cost.** A full ability with no target, or a big payout landing at once (a boss's
  2 Sparks), loses less. It casts at its cost, so the extra unit changes how much is lost, not how often it casts.
- **Tuning numbers are in Sparks and Souls.** The damage per unit (200, 100 for Souls with Soul Harvest), each
  passive step's payout, Feedback's half and every cost: each is one number in the unit the player reads. The old
  bar of 200 survives only as that rate; the ratios and counts are as they were.
- **Blight is mass control.** Its upgrades spread it to more enemies rather than deepen the slow; Tenacity alone
  moves the slow past 30% (to 60% at the lowest Tenacity), so it needs no cap.
- **A jumping hex goes on as it is.** Contagion carries its slow so far and its time left in authored seconds, which
  the next enemy's Tenacity scales again: a Blight dying with 2 s left at Tenacity -50 (1.33 s authored) slows an
  enemy at Tenacity 0 on from 30% for 1.33 s, to 40%.
- **A recast never weakens Blight.** It prefers an enemy without Blight. On one that has it, the time starts over
  and the slow holds where it is until the new hex's slow passes it, so one enemy is never slowed more than one hex
  can, and a recast never visibly lets it speed up.

- **The Stormcaller turns on the Arc.** Arc is its one early ability; most nodes change it, and a second ability
  comes late as a pick between mass (Thunderclap) and a boss (Thunderstrike). Ball Lightning and Supercell go:
  Thunderclap does the same job and has been tried. Overload (Daze) and Static Charge (Charged) go as more names.
- **Each side has its role.** The Stormcaller deals damage and counters armor, with no enemy debuff; the Hexer
  controls and counters healers.
- **The Stormcaller's names**: Static, Spark, the ability bar, Arc, Crit, Armor, Thunderclap, Thunderstrike.
- **The base shot and the Arc apply Static; the late abilities don't.** One Stormcaller gives an enemy one stack,
  and an enemy discharges once, so the loop Arc, Static, Spark, Arc ends with the enemies it tags. Chain Lightning
  tags more enemies and earns more Sparks; Wild Arc hits the same ones and deals more damage. Thunderclap applying it
  would tag the whole range and pay for itself on a big wave, while Thunderstrike earns almost nothing.
- **After names what a node needs.** The Arc's follow-ups come after Arc, Feedback after Conductor (whose crits it
  pays on), Agony, Contagion and Twin Hex after Hex: without it they do nothing, and the chain is a road to commit
  to. After stays where a node follows one pick of a cluster (Deep Doom after Doom), and it names the side for every
  other node, which the side's column already shows, so no line is drawn for it.
- **The fork is a fork gate.** Stormcaller or Hexer is picked in Tier I's gate, and the tiers above show only that
  side, beside a column of what both share. Shared nodes spread so each tier stays one row: Long Throw II and Heavy Stones in Tier II, Quick Hands and
  Piercing Stones in Tier III; the Hexer's Soul Harvest moves to Tier III for room.
- **Plating is the Burst's counter.** Its small hits lose most to it and no node takes it away: Rock Splitter
  became Piercing Stones, armor penetration. The Stormcaller's Arc is magic and passes plating; the Hexer has no
  answer.
- **The base is strong and the tiers above grow the abilities.** Damage while a stream of Walkers crosses the range,
  per dollar of the tower, against the Sniper (one enemy) and the Mortar (a crowd), as the Stormcaller:

  | Tier | Sniper | Mortar | Burst |
  |---|---|---|---|
  | Tier 0 bought | 12 | 12 | 11 |
  | II | 29 | 19 | 26-27 |
  | III | 37 | 36 | 37-42 |

  The Burst alone, killing what it hits, from enemies of 1000 health to 75; its Arc casts every 3.4-5.1 s in Tier III.
  Its passive pays by steps of the dead enemy's max health (a quarter of a Spark, a half, 1, 2), so
  what it earns follows the health it kills. Half a Spark a death made it strongest on many weak enemies (56 on
  enemies of 75 health) and weakest on tanks. A fifth of max health (at least 20, no cap) evened it out as well, but
  is a number the player can't read off an enemy. Other towers' kills also pay it, so in a full defence it earns more
  (open question 12). The draft (every 0.75 s, Heavy Stones +14, Quick
  Hands to 0.5 s, a whole Spark from Feedback) came to 87-174 in Tier III and 15 with Tier 0 bought. Heavy Stones at
  +14 doubled the base shot and every ability with it. Each step of Quick Hands from 1 s to 0.7 s adds about 3 in
  Tier III.
- **Soul Harvest stays the Hexer's**: Feedback is the Stormcaller's way to Sparks sooner.
- **Smaller casts, as often.** The aim is an Arc about every 2 s and a Hex of Doom about every 3 s. Arc's jumps deal
  40% of base damage (60% with High Voltage), from 50% and 75%; Thunderclap 40% and Thunderstrike 200%, from 50% and
  500% (question 13). At the old sizes, with casts as often, a full defence took the Burst to 42-82 a dollar. Arc stays at 2
  Sparks: at 1, with Feedback giving back about half a Spark, it cast every second. Hex of Doom stays at 2 Souls: a
  Hexer alone casts it every 3.2-3.9 s in Tier III. At 3 Souls and 45% it deals the same per Soul, every 4.7-5.9 s.
- **A unit is 200 damage of the base shot.** At 300 a Spark took 12-21 s of a base shot on one enemy, so against a
  boss alone the abilities barely came; at 200 it takes 8-14 s.
- **The passives have one-word names**: Static earns Sparks, Soulbrand earns Souls. They were Ground Fault and Soul
  Tithe.

## Open questions

1. ✅ **Short Circuit's place.** A node: the bare Arc stays simple, and countering armor is a pick (the user,
   2026-11-13).
2. ✅ **What the Arc nodes reach.** Conductor, Short Circuit and High Voltage change only the Arc, each node about
   one thing; Thunderclap and Thunderstrike stay as their rows say (the user, 2026-11-13).
3. ✅ **Soulbrand and Static.** A kill by Soulbrand's burst or Static's discharge earns kill credit and XP as any hit
   does, for the towers that tagged the enemy, shared by their stacks (final review). Tiers open on XP, so a
   passive that took a tower's kills from it would slow its own climb. A stack from a Beacon's light still credits
   no tower (`FEATURE-next-gen-aura-tower.md`).
4. ✅ **Tier I gate and costs.** Placeholders until the balance pass, by "Balance waits". The gate is the bundle's (`FEATURE-next-gen.md`, "Answered by the user"). XP 60, the $20 unlock, the ability costs (Arc 2, Thunderclap 3, Thunderstrike 3, Doom
   2, Blight 2, Inversion 3) are placeholders. Costs stay small whole numbers.
5. ✅ **A turning head.** None: it throws at random targets, so a head would jump with every throw. The thrower
   itself is the moving part (Look: the arm, the crown, the brew) (final review, autonomously).
6. ✅ **A forked tower's stones** take the fork's colour on the same arc: a stone crackling blue for the
   Stormcaller, a brew blob for the Hexer, so the fork reads in flight too; the Arc's charged stone is brighter
   still. The request asked for it (final review, autonomously).
7. ✅ **Tier gates.** XP 60, 120 and 200, as every tower (`FEATURE-next-gen.md`, "Answered by the user").
8. ✅ **Blight's mass node.** Blight Cloud (Blight also curses every enemy within 1 cell of its target), at Tier II
   beside Deep Doom, with Choking Blight at Tier III. Doom is for one enemy and pays on the damage of every tower
   around it; Blight is for a crowd. With four towers around, at Tier III Blight deals about twice Doom's damage to a
   crowd and Doom about 1.7 times Blight's to an Elite or a Boss. Deep Blight (slow to 45%) was single-target.
9. ✅ **Blight's name.** It keeps the name, its new meaning marked as changed: it still poisons, and its marker, its
   colour and Prism's list keep working. A control name (Hex of Binding) was the other option (final review, autonomously).
10. ✅ **Inversion's place.** A Tier III hex of its own, as drafted (the user, 2026-11-13).
11. ✅ **An ability at a wave's start.** 1 unit in each ability bar: with no tower bar, "full" and "one unit
    already given" are the same thing.
12. ✅ **The Burst in a full defence.** Where every enemy it tags dies, Tier III casts the Arc every 1.4-3.4 s and
    deals 42, 50 and 66 a dollar at 75, 300 and 1000 health, against the Sniper's 37 and the Mortar's 36, since other
    towers' kills pay it Sparks. Against a boss alone the Arc comes every 16-29 s. Left to BalanceReport once the
    Burst is built (final review, by "Balance waits"); the root lists it where the bundle steps past its ceiling.
    Gentler steps (a quarter, three eighths, a half and 1 Spark, so a boss gives one) read 42, 46 and 50, the Arc
    every 2.3-3.4 s, if the report asks for them.
13. ✅ **The Hex and Storm picks** were marked `rebalance` (Hex $3 and $9, Storm $6 and $13) and are retuned on paper
    (the user, 2026-11-14: "rebalance now"). Blight's poison drops to 3% of base damage a tick: $3 and $7, still
    Doom for an Elite, a Boss, a Knight and a Juggernaut, Blight for the rest; Blight Cloud prices $16, from $21.
    Raising Doom instead (40%, Deep Doom 55%) closed the gap further but took the Doom build at Tier III from +29% to
    +44% of the reference. Thunderclap rises to 40% and Thunderstrike drops to 200%: $10 each, even. At 25% the mass
    pick won only a Juggernaut; now the crowd build wins a crowd at Tier III (38.6 against 32.5 at 1000 health) and
    the boss build a boss (16.7 against 13.7). Tier III against the reference: the crowd build +26% (from +21%), the
    boss build +4% (from +7%), the Blight build +113% (from +154%), still the tree's strongest, left to the balance
    pass.
14. ✅ **How far an unguided stone may miss.** The stone lands where its target stood when it was thrown (the user,
    2026-11-13). The 100% zone is 0.75 cells and a Walker walks 1.28 cells a second, so a flight of half a second
    still lands it in the zone; a Runner is out of it sooner. The flight time is not set.
    It aims at where its target stands and never leads it; a fast enemy takes the 50% edge or nothing. No node
    mends it: leading a target stays the Mortar's Predictive Fire. The flight time is the balance pass's, held to
    one constraint: a Walker is still inside the 100% zone when the stone lands (final review, autonomously).
