# Feature Request: Next-gen Aerie Tower

**Status:** draft, 2026-11-13, fourth round, reviewed with the whole bundle. Tiers 0 to III and the look, from the visual request's handwritten Hive
notes, written to the node rules in `FEATURE-next-gen-upgrade-tiers.md`. Part of `FEATURE-next-gen.md`. Weighed once
on paper: damage 15 and price $30 are set; the rest are the balance pass's (open questions 8 and 14).

## Summary

The Hive (`SeekerTower`) becomes the Aerie, the game's control tower: it stops many enemies spread over the map, never
the same one twice in a row, the one about to leak first. Its passive is the freeze itself: every enemy a missile
damages is frozen. Its nest is built in and full at every wave start. A Tier I pick decides what a run of freezes
releases (a swarm or a hatching); a Tier II pick gives the nest a second kind of missile and the tower its identity:
a ruby crystal hive that freezes the freeze-immune and hits twice as hard, or a shade spire that passes shields and
silences what it froze. A Tier III pick decides what the ice does to the enemies around it. The tower is reworked
further than the other next-gen towers: today it is rarely built.

## What this feature adds

The tables below are read by `td.ui.PreviewUpgrades` (run-jtd), which draws the tree beside today's panel and lists
what the nodes no row replaces leave unused. The column rules are in run-jtd's `SKILL.md`.

### How the tree builds

- **A freeze is the Aerie's damage.** Its job is enemy-seconds stopped, spread over as many enemies as it can reach;
  its magic damage is small, but a freeze needs it: a hit a Shield takes whole freezes nothing. The
  freeze meter (time frozen shortens an enemy's next freeze, walking wins it back) stays as it is, so the Aerie gets
  full freezes by spreading them: its targeting does that by itself.
- **It hunts the enemy that leaks soonest**: the one that would reach its exit first at the speed it walks now. One
  number holds both of the Aerie's wants: a fast enemy far back and a slow one near the exit compare by the seconds
  they have left. A frozen enemy walks at no speed, so it comes last without a rule of its own, and a slowed one later.
- **Levers** are plain stats every build buys some of: how fast the nest loads, how long a freeze lasts, how many
  missiles the nest holds, range, missile speed and damage. Damage is a lever too: it is what gets a missile past a
  shield, and so what lets it freeze.
- **What a run of freezes releases is the Tier I pick**: Swarm (seven missiles at once, at seven different enemies:
  a wide wave) or Hatching (seven missiles hatch into the nest one after another: a long wave). Each comes on every
  14th freeze, like the Sniper's Fourth Shot: a count shown as a buff on the tower, not a currency. Every freeze
  counts, a released missile's too, and released missiles are drawn by the nest's lot like any other.
- **The second kind of missile is the Tier II pick, and the tower's identity.** Each answers one enemy the bare Aerie
  can't freeze: Ruby the freeze-immune (and hits twice as hard), Shade the shielded (and silences what it froze after
  it thaws). The nest then draws each new missile by weight, as the Mortar's magazine draws shells: one in three of
  the second kind, one in two with Rich Brood.
- **What the ice does around it is the Tier III pick**: Ice Wake (a frozen enemy chills the ones walking past: control)
  or Shatter (its ice bursts on the enemies around it as it ends: damage).
- **Its names**: the nest, Freeze, Swarm, Hatching, Ruby, Shade, Shield, Silence, Ice Wake, Chill, Shatter. Eleven,
  like the Scorcher's ten; Freeze, Shield, Silence and Chill read as the game says today. Its enemy debuffs are Freeze
  and its identity's Silence or the Tier III pick's Chill.

### Bare tower

| Stat | Value | Today | Replaces |
|---|---|---|---|
| Price | $30 | $30 | |
| Damage | 15 magic | 40 | |
| Loads | a missile every 2 s | fires every 2.25 s | |
| Freeze | 1 s | 1.5 s | |
| Range | 4 cells | 4.5 | |
| Missile speed | as today | | |
| Nest | 3 missiles, full at every wave start; stored missiles launch 0.2 s apart | only with Attune | base.attune |
| Targets | Soonest out: the enemy that would reach its exit first at the speed it walks now | Fastest | |
| Retarget | A missile whose enemy is frozen or dies before it lands turns to the soonest-out enemy within 2 cells of itself; with none, it flies home into the nest, or fades when the nest is full. | keeps its enemy; picks the nearest when it dies | |
| Passive | Freeze: every enemy a missile hits is Frozen, unless a Shield takes the whole hit. | every missile freezes | |

### Tiers

| Tier | Gate | Unlock | Colour |
|---|---|---|---|
| 0 | - | - | #5cc4e2 |
| I | XP 60 | $20 | #8fd0f2 |
| II | XP 120 | - | #b5e0f6 |
| III | XP 200 | - | #daeae3 |

### Upgrade tree

| Tier | Node | Kind | Cluster | After | Glyph | Colour | Cost | Replaces | Does | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | Quick Brood | stat | | | seeker.head.twin_warhead.2 | | | | Loads a missile every 1.6 s, from 2 s. | Name is a proposal. |
| 0 | Deep Freeze | stat | | | seeker.head.deep_freeze.1 | | | seeker.head.deep_freeze.1 | Freeze +0.3 s | |
| 0 | Wide Comb | stat | | | seeker.head.twin_warhead.1 | | | seeker.head.twin_warhead.1 | Nest +2 missiles | Name is a proposal. |
| 0 | Long Flight | stat | | | base.range | | | base.range | Range +1 cell | |
| I | Swarm | ability | Release | | seeker.head.twin_warhead.3 | | | seeker.head.twin_warhead.3 | Every 14th freeze releases 7 missiles at once, one at each of the 7 soonest-out enemies in range. | A wide wave: the missiles fan out over the board. Today's Brood (a salvo spreads) grown into a cast. |
| I | Hatching | ability | Release | | Nest | | | | Every 14th freeze hatches 7 missiles: they join the nest past its size, one every 0.4 s, and launch as usual. | A long wave: seven missiles over a few seconds, each picking as usual, so it keeps up with enemies as they thaw. It leaves the load alone. |
| I | Feeding | ability | | | seeker.head.twin_warhead.4b | | | seeker.head.twin_warhead.4b | Each freeze takes 0.25 s off the next load, never below half its time. | The request's node, on every freeze: a Ruby's, a Shade's and a released missile's too. Today's Rearm (a freeze launches a missile) made gentler. Seconds, not a share, so it grows with every faster load: +19% alone at a 1.6 s load, +24% at 1.3 s; beside a Release, +45% and +63% (open question 7). Name is a proposal. |
| I | Swift Wings | stat | | | seeker.special.hunters_mark | | | | Missile speed +50% | A fast enemy walks out of range before a slow missile lands. Name is a proposal. |
| II | Ruby | ability | Second missile | | seeker.special.arcane_warhead | #d0405a | | | Adds Ruby missiles to the nest, one in three: a Ruby deals ×2 damage, and its freeze also holds what is freeze-immune. | The crystal hive: the tower turns ruby. Against the Yeti, the Molten Core, the Monolith and the Warden; ×2 is the Mortar's Heavy, and it gets through more of a shield. |
| II | Shade | ability | Second missile | | Silence | #a6e8c8 | | seeker.extra.mixed_payloads.2 | Adds Shade missiles to the nest, one in three: a Shade deals Shadow damage, which goes past Shield and insulation, and its freeze leaves its enemy Silenced as long again after it thaws. | The shadow spire: the tower turns dark with a pale green glow. Against the shielded and casters: Menders, shielders, the Timekeeper, a boss's calls. Today's EMP Payload's Silence. |
| II | Sharp Sting | stat | | | seeker.head.deep_freeze.2 | | | seeker.head.deep_freeze.2 | Damage +10 | Name is a proposal. |
| I | Wide Comb II | stat | | | seeker.head.twin_warhead.1 | | | | Nest +2 missiles | |
| II | Long Flight II | stat | | | base.range | | | base.range.2 | Range +1 cell | |
| III | Ice Wake | ability | Around the ice | | Chill | | | seeker.head.deep_freeze.3 | An enemy it froze Chills every enemy within 0.5 cells of it by 30% while the freeze lasts. | Control: the ice block jams the enemies behind it. |
| III | Shatter | ability | Around the ice | | Shatter | | | seeker.head.twin_warhead.4a | When a freeze it landed ends, the ice bursts: every other enemy within 1 cell takes 30% of the missile's damage. | Damage to a crowd. Today's Shatterburst and Deep Freeze II's shatter on a kill, as one rule on every thaw. |
| III | Rich Brood | stat | | Second missile | seeker.extra.mixed_payloads.4 | | | seeker.extra.mixed_payloads.4 | The second kind comes up one in two, from one in three. | The identity's strength. Today's Full Rack (every missile a payload). Name is a proposal. |
| III | Deep Freeze II | stat | | | seeker.head.deep_freeze.1 | | | | Freeze +1 s | |
| III | Quick Brood II | stat | | | seeker.head.twin_warhead.2 | | | | Loads a missile every 1.3 s, from 1.6 s. | |

### Terms

New or changed meanings of names the rows use. A name with no row here reads as the game says it today.

| Term | Glyph | Means |
|---|---|---|
| Freeze | | The Aerie's passive: every enemy a missile hits is Frozen. A hit a Shield takes whole freezes nothing; one that gets partly through freezes. Insulation never blocks a freeze, even where it takes all the damage. The Freeze itself reads as the game says today. |
| Nest | | The missiles the Aerie holds ready, drawn circling the tower and in the pane. It loads one whenever it has room, with or without a target, so it is full at every wave start; with an enemy in range, stored missiles launch 0.2 s apart, each picking its own target. With a second kind owned, each new missile is drawn by lot, and the pane shows each kind's chance. |
| Soonest out | | The Aerie's targeting: the enemy with the fewest seconds left to its exit, its path left over the speed it walks now. A frozen enemy has none, so it comes last. |
| Release | | What every 14th freeze sets off: Swarm or Hatching. The count is a buff on the tower, shown in the pane as the Sniper's Fourth Shot is. Every freeze counts, a released missile's too; a wave starts the count at none. A Release gives fewer missiles than the freezes it takes, so it never pays for the next one by itself. |
| Swarm | seeker.head.twin_warhead.3 | Seven missiles at once, each at a different enemy, soonest out first; fewer if fewer are in range. |
| Hatching | Nest | Seven missiles hatch into the nest, past its size, one every 0.4 s; each launches as usual. |
| Ruby | seeker.special.arcane_warhead | A missile that deals ×2 damage and encases its enemy in red crystal: a Freeze by the same rule, which also holds what is freeze-immune. |
| Shade | Silence | A missile that deals Shadow damage, and whose Freeze leaves its enemy Silenced for as long again once it thaws. |
| Shadow | | Magic damage that passes Shield and insulation; magic resist still applies. New in the bundle (`FEATURE-next-gen.md`); the Obelisk's Deep Shadow deals it too. |
| Ice Wake | Chill | While an enemy the Aerie froze stays frozen, every enemy within 0.5 cells of it is Chilled. |
| Shatter | | When a freeze the Aerie landed ends, the ice bursts on the enemies around it. |

### Look

A crystal spire with its missiles circling it: the nest. It keeps a blue, moved to `#5cc4e2` so it stands clear of
the Stormcaller. There is no turning head: the moving part is the nest. Drawn in the art pass (2026-11-13), in the raised view with a footprint that grows with the tier (`FEATURE-next-gen-tower-looks.md`, "Art direction"). The sheets show exactly what to build: [every tier](next-gen-art/catalogue.png), [on the board](next-gen-art/board.png), [the moving part and its cues](next-gen-art/heads.png), [shots and casts](next-gen-art/shots.png). Every measure, in cell units, is in `.claude/skills/run-jtd/TowerArt.java`, `aerie`, `nest` and `missile`.

| Tier | Foundation | Ruby | Shade |
|---|---|---|---|
| 0 | a dark blue hexagonal base with a hexagonal crystal spire on it; no plinth | | |
| I | one round plinth step; a perch ring floating round the spire | | |
| II | two steps; a taller spire with four ice shards at its foot | the spire, the shards and the perch ring in ruby red `#d0405a` | dark stone `#3c4050`, three pale green slits in the spire's top, a pale green perch ring `#a6e8c8` |
| III | three steps; a six-pointed crystal star for a spire, six shards, three perch arms each ending in a small ring | a ruby cluster, from the same shapes | a dark tower with a crown of six pale green points round its top, in a green glow |

- **The nest circles the tower.** Every stored missile is drawn on one even ring just outside the foundation
  (0.06 of a cell past its edge) at the height of the perch, so a missile on the far half of the ring passes
  behind the spire and one on the near half in front of it. They are spaced evenly, in its kind's look: a Frost missile a blue dart, a Ruby a red
  diamond shard, a Shade a pale green wisp with a dark eye and a fading tail. They are the missiles themselves, one
  for one, not an effect: the missile that flies is the one that left its place on the ring. A new missile joins the
  ring as it loads, and the others spread to make room; a missile that flies home takes its place again. The ring
  holds still while no enemy is in range, and its path is not drawn.
- **A missile flies like an aircraft.** It leaves the ring along its orbit, turns toward its enemy on a smooth curve,
  never a jump in heading, and speeds up gradually to its top speed (the Missile speed stat). In flight it keeps
  turning after its enemy as the enemy moves, and turns harder the nearer it gets, so it never circles a fast enemy.
  It never misses: it always reaches its enemy, or turns to a new one (Retarget).
- **The Release cue**: once the next freeze will set off a Release, the ring's path shows as a thin line in the
  missiles' colour, each missile wears a small halo, and the perch ring turns near-white in a glow. All of it goes
  when the Release fires.
- **Swarm**: seven missiles appear on the ring at once and peel off one after another in a fan across the board.
  **Hatching**: its missiles join the ring one by one and leave it as usual.
- **Ruby and Shade change the look less than a fork does.** The Burst's Stormcaller and Hexer become other towers;
  the Aerie stays a spire with its nest, and its identity shows in the spire's material and glow and in its missiles.
- **One freeze look in three colours** ([marks](next-gen-art/marks.png)). Today a frozen enemy wears an ice block
  over its own body and bursts into ice shards as it thaws. Ruby and Shade reuse both shapes, recoloured: the plain
  freeze stays ice, a Ruby freeze is the block in ruby red with a pale pink rim, a Shade freeze is smoky dark ice
  with a pale green rim. They thaw into red shards and into dark shards with a green edge. The shape says "frozen",
  the colour says which missile froze it. A Shade's Silence after the thaw wears the Silence mark.
- **Ice Wake**: a faint dashed ring of frost on the ground round the ice block while it lasts
  ([grounds](next-gen-art/grounds.png)); the chilled enemies show today's chill. **Shatter**: the ice breaks into
  shards that fly out over the enemies around it.

## Interconnections

- `FEATURE-next-gen-upgrade-tiers.md` (its node rules), `FEATURE-next-gen-tower-looks.md`. Missiles fly today; they
  need nothing from `FEATURE-next-gen-shot-flight.md`.
- **No currency.** The Release picks are ability nodes triggered by a count, as the Sniper's Fourth Shot is; they use
  no ability bar from `FEATURE-next-gen-tower-abilities.md`.
- **The nest draws by weight as the Mortar's magazine does** (`FEATURE-next-gen-mortar-tower.md`), and shows the same
  chance strip in the pane. It holds a few missiles, not ten shells, and is not kept in the save (Decisions made).
- **Ruby beside Soulfire.** Both are "the same rule, and it reaches the immune" (`FEATURE-next-gen-cinder-tower.md`).
  Ruby needs a freeze that an immunity to Freeze doesn't stop; whether that is a kind of its own is for the plan.
- **Shade past Shield.** Today only damage over time goes past a shield; a Shade's hit is the first direct hit that
  does.
- **The freeze meter** (`FreezeMeter`) is unchanged. Ruby's freeze fills and follows the same meter.
- **Thermal Shock** (the Scorcher's Tier III pick) pours more into a frozen or chilled enemy: Ice Wake feeds it.
- `td/tower/seeker/CLAUDE.md` describes the nest once Attune is owned, spread salvos, the payload plan by number and
  the rearmed missile's flag; those lines change.
- The Yeti is written as the answer to "a Seeker-only defence". Ruby gives the Aerie its own answer, one missile in
  three (or two).

## Constraints and open risks

- **Ice Wake is a scan around every frozen enemy each tick.** With many enemies frozen at once the tick budget
  (`td.PerformanceHarness`) decides how it is done, not this doc.
- **Feeding and the Releases can feed themselves.** Feeding pays back part of a load for each freeze, and a Release's
  missiles freeze and count toward the next Release. Both stop in code (DESIGN §10): a Release gives 7 missiles for 14
  freezes, so its own freezes reach half of the next one, and all missiles come to ×2 the normal ones (1 / (1 - 7/14));
  Feeding never loads more than one missile a tick or takes a load below half its time (open question 7).
- **Shields now stop the bare Aerie.** An enemy whose Shield takes a whole missile isn't frozen, so shielding waves
  (a Brood Nurse's ward, a Warden's shield) hold it off until something breaks the shield or the tower picks Shade.
- **Soonest out across lanes.** On a level with several exits, each enemy counts its own path's length left.
- **On paper, the doc's numbers are far too strong.** The first model (tick by tick, over a steady stream:
  `trees/seeker.py` with `--set warm_up=20 --set seconds=120`, at the numbers the doc had then) priced a second of freeze on one enemy as what four towers around it deal it in that second, about
  62. Against the reference in its role:

  | | Tier 0 | I | II | III |
  |---|---|---|---|---|
  | Against the reference | +28% | +119-134% | +105-144% | +115-278% |
  | One enemy / the Sniper (ceiling) | 21.0 / 11.4 | 23-38 / 16.4 | 33-60 / 26.3 | 40-65 / 39.5 |

  Two causes: the bare Aerie deals 15 damage a second, as much as the bare Sniper, and it also holds one enemy about
  40% of the time.
- **A set of "many short freezes" brought Tiers I to III within ±15%**: damage 5 (from 30), range 3 cells (from 4),
  with a shorter load and freeze and a smaller Shatter. On one enemy the freeze meter caps what more missiles add; in a
  crowd each missile freezes a fresh enemy. Tier 0 then read -19%, Tier I -3% to -6%, Tier II -1% to +14%, Tier III
  -2% to +15% (Shade lowest, Swarm highest). The rest of its numbers were not kept; damage is set at 15 instead
  (Decisions made).
- **Fair node prices from that set**: Long Flight $11, Quick Brood $8, Deep Freeze $6, Wide Comb $0; Release $20
  (Swarm wins a big crowd, Hatching the rest), Feeding $8, Swift Wings $2; Long Flight II $27, Sharp Sting $18, Ruby
  $12, Shade $0; Around the ice $18 (Ice Wake and Shatter even), Deep Freeze II $12, Quick Brood II $10, Rich Brood $6.
- **Measured from a wave start, at damage 15** (`trees/seeker.py` as it stands: the first 25 s, the nest full and the Release count at none). The
  first Release takes longest, and a bigger nest brings it sooner: without both Wide Combs Tier I reads +53% against
  +95% (at damage 30). Damage alone can't bring the tower to the reference: even at damage 1, Tiers I to III stand
  over it, since the freezes carry them. Tier 0 meets it at 12-13 from a wave start and about 20 in a steady stream;
  15 is between. Against the reference in its role, Tier 0 to III, with Wide Comb II in Tier I, at $20 and (after
  the slash) at the $30 it costs (`--set` has no price: `PRICE` in the model):

  | Build | 0 | I | II | III |
  |---|---|---|---|---|
  | Swarm, Ruby, Ice Wake | +5% / -30% | +56% / +4% | +73% / +15% | +91% / +27% |
  | Swarm, Ruby, Shatter | +5% / -30% | +56% / +4% | +73% / +15% | +91% / +27% |
  | Hatching, Ruby, Ice Wake | +5% / -30% | +35% / -10% | +70% / +13% | +112% / +42% |
  | Hatching, Ruby, Shatter | +5% / -30% | +35% / -10% | +70% / +13% | +133% / +55% |
  | Swarm, Shade, Ice Wake | +5% / -30% | +56% / +4% | +44% / -4% | +58% / +5% |

  Each node's share of its tier (and its fair price): Long Flight +16% ($14), Quick Brood +11% ($9), Deep Freeze +7%
  ($7), Wide Comb +6% ($6); Hatching +22% ($30), Swarm +20% ($26), Feeding +15% ($20), Wide Comb II +4% ($6), Swift
  Wings +2% ($4); Sharp Sting +17% ($75, +83% against a Relay), Long Flight II +12% ($35), Ruby +10% ($24), Shade 0%;
  Shatter +14% ($47), Ice Wake +8% ($29), Deep Freeze II +8% ($31), Quick Brood II +14% ($50), Rich Brood +2% ($8).
  Swift Wings and Rich Brood are the weakest. At 100% of the damage and +0.3 s, Shatter stood at +30% ($131) and Deep
  Freeze II at +2%.
- **What the model can't see**: shields and Silence, a missile speeding up, and a level where only the Aerie kills.
  A Ruby counts only its ×2, not the freeze-immune it holds.

## Decisions made

From the request (its handwritten Hive notes):

- The nest stays and is built in, 3 missiles from the start, full at every wave start.
- Two kinds of missile: today's blue freeze and a red one that does something else; Ruby is the request's crystal that
  ignores freeze immunity.
- A cluster of release abilities, one pick: seven missiles at the seven fastest enemies in range, or the nest loading
  +300% for seven missiles.
- Targeting weighs speed and progress, so an enemy near its exit is the one the tower wants stopped.
- A missile doesn't take an enemy already frozen when another as urgent isn't, and one whose enemy is frozen in flight
  turns to the one behind.
- A node that shortens the load for each freeze (Feeding).
- A control tower that freezes many enemies spread over the map; reworked further than the Scorcher.

From the review rounds (2026-11-13):

- **The name is Aerie.** The Amber Hive's Hive Guard and Hivemind share the old word. The class, the
  `TowerFactory.Type` constant and the node ids keep `seeker`.
- **The passive is the freeze**: every enemy a missile hits is frozen, and a hit a Shield takes whole freezes
  nothing; insulation never blocks it (the user, 2026-11-13). It adds no name, gives damage a job on a control tower, and gives each identity an enemy to answer.
  Frost Trail (chilling along the flight) is dropped, and Hoarfrost with it.
- **The Release is a count of freezes**, shown as a buff on the tower as the Fourth Shot is, with a cue on the board:
  the ring brightens once the next freeze sets it off.
- **A Release counts its own freezes**: 7 missiles for every 14th freeze, every freeze counted and every missile drawn
  by the nest's lot, so a Release may send Rubies and Shades. A Release with fewer missiles than freezes stops itself,
  so no rule says released missiles don't count, and they need no look of their own. Hatching hatches its missiles
  into the nest and leaves the load alone.
- **Feeding is 0.25 s off the next load for every freeze**, in seconds so it grows with the tier.
- **Freeze 1 s, +0.3 s from Deep Freeze and +1 s from Deep Freeze II; Nest 3, +2 from Wide Comb and from Wide Comb II.**
- **Hailstone is dropped**: an area freeze would freeze enemies a missile never damaged, against the passive, or need
  an area hit of its own. The Release has two members.
- **Ruby freezes the freeze-immune and deals ×2 damage; Shade passes Shield and Silences after the thaw.** Each
  identity answers one enemy the bare Aerie can't freeze.
- **Ice Wake and Shatter form the Tier III cluster**, what the ice does around it: control or damage. Brittle is
  dropped: the freeze already serves the other towers (Agility -100), and Brittle in today's tree didn't make the
  tower built.

Proposed, not yet reviewed:

- **Soonest out is one number**: seconds left to the exit. It weighs speed and progress with no tuning weight, and
  passes over a frozen enemy without a rule of its own. "Within 10% of the same progress" or a blend of two scores
  would be a number the player can't see.
- **The identity is the second missile.** Ruby or Shade is both the red missile the request asked for and the
  shadow tower it offered, in one pick. Arcane (Unravel, more magic damage) and Holy (purging Shield and Heal) were
  weighed: Arcane is a damage pick on a control tower, and Shade already answers the shielded and the casters.
- **No fork.** As for the Scorcher, two sets of nodes would be more names for one change of missile.
- **No crit.** Ruby's ×2 is the Aerie's crit, by chance of the draw, as the Mortar's Heavy is its crit.
- **The nest is not saved.** It refills at every wave start, so a save loses nothing a player could see; with a second
  kind, its draws at the send come from the game's random. ADR 0021 stands as it is.
- **The Aerie costs $30, as today**, above the other next-gen towers' $15-20. Its Tier 0 nodes are open from the
  start and get bought with it: the bare tower reads -30% against the reference and falls under the floor in a
  crowd, and a tower bought with them stands near the reference from Tier I.
- **Damage 15 magic**, from 30: Tier 0 meets the reference between a wave start and a steady stream. What Tiers I
  to III stand over it is the nodes' to answer, not the damage's.
- **Kept as they price**: the Releases (strong by design; at most a higher tier, question 11), Sharp Sting, Long
  Flight, Quick Brood, Deep Freeze, Feeding, Long Flight II, Quick Brood II, Ruby and Shade.
- **Deep Freeze II is +1 s** (1.3 s to 2.3 s), from +0.3 s, which the freeze meter cut short on one enemy.
- **Both Tier III picks act around the ice**: Ice Wake is control (a Chill), Shatter is damage, 30% of the missile's
  to every other enemy within 1 cell, from 100%. Hitting the thawed enemy again, or freezes that fill no meter, would
  make Shatter a pick for one enemy too.
- **Wide Comb II moves to Tier I.** A bigger nest brings the first Release of a wave sooner, the one that takes
  longest; every later one comes as often either way.
- **Dropped**: Twin Warhead II (two missiles a launch), Frostbite, Arcane Payload, Tracer Payload, EMP Payload's
  dispel, Nullifier, Hunter's Mark and its Focus, Arcane Warhead, Brittle, Absolute Zero, Over the Horizon (Range
  III), the payload plan by missile number.

## Open questions

1. ✅ **Tier II's free slot**, where Hoarfrost stood, stays empty, and the plan doesn't wait for it: a tier has no
   fixed count. The user may still fill it (final review, autonomously).
2. ✅ **Where the second missile comes in.** Tier II, as the identity pick: a bare nest holds only plain missiles,
   and the lot arrives with it (final review: the summary, the tree and the look are all built on it).
3. ✅ **A missile with nothing left to hunt** flies home into the nest, or fades when the nest is full (final
   review: the Retarget row and the Look already say so).
4. ✅ **A shield a missile only dents.** Any hit that gets partly through freezes: one rule to read (final review,
   from the user's answer to question 10).
5. ✅ **Swift Wings' place.** Folded into question 12.
6. ✅ **Tier gates.** XP 60, 120 and 200, as every tower (`FEATURE-next-gen.md`, "Answered by the user").
7. ✅ **A floor under Feeding.** Feeding never takes a load below half its time (the user, 2026-11-13). No next-gen
   tower speeds another's attacks any more, so the buffs named below are a case that may return, not one in the
   bundle. With a Release, every normal missile brings about two freezes, so Feeding takes about
   0.5 s off each load. By hand (every missile freezing, missiles a second, Release and Feeding against the Release
   alone): 1.66 against 1.25 at a 1.6 s load, 2.50 against 1.54 at 1.3 s. That is the growth with the tier the user
   wants. Under attack-speed buffs it climbs fast: Rally and one Beacon node (+40%) bring a 1.3 s load to 0.93 s and
   the Aerie to about +100%; under 0.5 s a load comes from freezes alone, and only the enemies and one missile a tick
   hold it.
   - Feeding never takes a load below half its time: nothing changes at the tower's own numbers, and a buffed Aerie
     at most doubles its loads (5.8 missiles a second instead of 10 at +100% attack speed).
   - No floor: buffs are rare and their own towers cost; `BalanceReport` shows it once the tower is built.
   - A share instead of seconds (a quarter of the load): +33% at any load, but no longer stronger at higher tiers.
8. ✅ **On paper.** `trees/seeker.py` holds the numbers above, and its table stands under "Constraints and open
   risks" (from a wave start, at $20 and $30). The 25 s of a wave are a guess; how long a wave lasts sets what the
   nest and the first Release are worth, which the balance pass measures (2026-11-14, autonomously).
9. ✅ **Tier 0 over the ceiling on one enemy** (listed in the root where the bundle steps past its rules; accepted
   until the balance pass, final review, autonomously) (19.5 against 11.4 at damage 15; 14.0 even at damage 1). The freeze meter
   holds one enemy about 42% of the time whatever the tower does, and that control is priced high.
   - ⭐ Accept it for a control tower: the Aerie's own damage on one enemy stays under the ceiling, and the ceiling
     rule is about damage.
   - A mechanic: an enemy the Aerie froze is protected from its next freeze for a while. That adds a rule.
10. ✅ **The passive against insulation.** The passive blocks a freeze only on a Shield that takes the whole hit,
   never on insulation (the user, 2026-11-13). A Relay's insulation 15 takes all of a plain hit's damage and the
   Relay is frozen all the same. Sharp Sting's +83% against a Relay was priced under the old rule.
11. ✅ **Hatching and Swarm stay strong** (+22% and +20% of Tier I). Hatching on one enemy stands over the ceiling in
   Tiers I and II (27.6 against 16.4, 47.1 against 26.3): its stream freezes a boss again as soon as it thaws. The
   Release stays in Tier I (the user, 2026-11-13); the root lists the breach for the balance pass.
12. ✅ **Swift Wings.** A missile flies 4 cells a second (8 pixels a tick on a 40-pixel cell), about three times a
   Walker's 1.28; +50% adds +2% of Tier I ($4), +4% against a Runner. With Frost Trail gone it is a plain stat.
   It stays in Tier I as a cheap stat until the balance pass prices it (final review, autonomously).
13. ✅ **Ice Wake's Chill.** The doc gives no number. The model assumes a 30% slow (as a Rime hex and the probe rig
   chill), held 0.5 s after an enemy walks out of the 0.5-cell ring; the Scorcher's Thermal Shock chills 40%.
   Ice Wake adds nothing on a lone enemy or a boss: it needs enemies walking behind the ice.
   30%, as the model assumes, until the balance pass (final review, autonomously).
14. ✅ **Shade can't be weighed yet.** It prices 0% and its cluster reads as one right answer only because the model has
   no shielded scenario. Kept as drafted and judged once the Aerie is built: the user reads it as balanced as it is
   (the user, 2026-11-14). The paper needs no shielded scenario before the plan.
15. ✅ **Shadow damage.** Yes, a new term (the user, 2026-11-13): magic damage that passes Shield and insulation,
    not magic resist. A Shade deals it, so no Shield stops a Shade's freeze; Soulfire stays a burn that passes half a
    magic resist.
16. ✅ **The nest on an empty board.** The ring holds still while no enemy is in range and turns only while missiles
    launch and land, so its turning means the tower is fighting (final review: the looks doc names "a nest circling
    an empty board" among the loops the calm board cuts).
17. ✅ **How the Ruby and the Shade freezes look.** Today's freeze crystal and its thaw shards, recoloured (Look).
