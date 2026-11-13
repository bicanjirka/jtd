# Feature Request: Next-gen Scorcher Tower

**Status:** draft, 2026-11-12, reviewed with the whole bundle 2026-11-13. Tiers 0 to III and the look, from the visual
request's handwritten Scorcher notes, written to the node rules in `FEATURE-next-gen-upgrade-tiers.md`. Part of
`FEATURE-next-gen.md`.

## Summary

The Scorcher stays the magic-only flamethrower, but it fires a stream instead of a wave: while it streams, every enemy
in its cone takes fuel every tick. The fuel goes into the enemy's own burn, which knows nothing of the tower: it burns
its fuel down by one rule, with no cap, so many Scorchers can feed one enemy and a heavy build is a real build. The
tower has no crit; its Core, the middle of the cone, pours 40% more. No fork: Soulfire is a pick in Tier II.

The burn rule is the game's one rule for every pool, so it reaches poison and Soulfire too (Interconnections).

## What this feature adds

The tables below are read by `td.ui.PreviewUpgrades` (run-jtd), which draws the tree beside today's panel and lists
what the nodes no row replaces leave unused. The column rules are in run-jtd's `SKILL.md`.

### How the tree builds

- **Fuel is the tower's damage.** A pool deals exactly the fuel it was given, before resists, insulation and
  tenacity. A tower shows what one stream gives an enemy: Flame × the stream's length, twice that in the Core.
- **Levers** are plain stats every build buys some of: Flame, the stream's length, reload, range and cone width.
  Attack speed (no next-gen tower gives another any; the rule stands for a buff that returns) shortens only the
  reload: a whole cycle sped up keeps the share of time it streams, and the
  buff would do nothing.
- **Reach is the Tier I pick**: Lance (further, narrower) or Fan (wider, closer).
- **The kind of fire is the Tier II pick**: Soulfire (the Scorcher turns into a soul-scorcher; a hotter fire that
  passes half of a magic resist and burns what is burn-immune) or Dragon's Breath (a stream twice as long with a short reload, at a weaker Flame).
- **The payoff is the Tier III pick**, each in its own situation: Flashover (a crowd), Meltdown (armored enemies) or
  Thermal Shock (enemies other towers hold).
- **On paper** (`tree_calc.py`, `trees/cinder.py`): every build stands between the floor (the bare Burst) and the
  ceiling (the Sniper on one enemy) at every tier. Per dollar of the tower, Tier 0 to III:

  | Build | One enemy | A crowd | vs the Sniper on one enemy |
  |---|---|---|---|
  | Crowd: Fan, Dragon's Breath, Flashover | 7.7, 11.0, 19.2, 22.5 | 12.3, 18.8, 32.0, 41.0 | +8%, +15%, +29%, +21% |
  | Reach: Lance, Soulfire, Flashover | 7.7, 14.0, 24.5, 30.9 | 12.3, 17.6, 30.0, 53.9 | +8%, +7%, +21%, +59% |
  | Heavy: Lance, Soulfire, Thermal Shock | 7.7, 14.0, 24.5, 33.0 | 12.3, 17.6, 30.0, 42.3 | +8%, +7%, +21%, +24% |
  | Floor (Burst) / ceiling (Sniper) | 5.7 / 11.4, 5.7 / 16.4, 11.4 / 26.3, 13.4 / 39.5 | 10.9, 10.9, 21.9, 25.8 | |

  A crowd here includes two Scorchers on a crowd of 1000 (`pair 1000`), which the Sniper's mix lacks, so the column
  against the Sniper reads a little high. With Bellows in Tier II, Tier I reads even on every build (+7-15%); in Tier
  I, Lance builds passed the Sniper on one enemy (16.6 against 16.4), and now stand at 14.0. Flashover makes the
  reach build's crowd the strongest at Tier III (+59%).
- **A 40° cone is a single-target tower.** On a straight path a cell away, with the target nearest, it reaches about
  1.25 enemies at a wave's default spacing (about 1.4 cells apart), so its crowd damage was its one-enemy damage and
  Fan bought nothing. At 60° it reaches about 1.6, and placed along a path far more: the Scorcher rewards placement.
- **Core × Thermal Shock.** At twice the Flame each (×4 on a held target in the Core) the heavy build passed the Sniper
  on one enemy by a third with every enemy held, and the Core alone passed it from Tier I. At +40% and +35% (×1.9)
  the heavy build stays under the Sniper with every enemy held; with a quarter held, 32.3.
- **Every node is priced halfway between Walkers and the enemy it counters** (`tree_calc.py prices`), and each
  cluster's members' fair prices: Nozzle $14 and $15, Flame $19 and $18, Burning $22, $18 and $12 (marked rebalance, retuned in the live game: question 3). Each
  member wins somewhere: Lance one enemy, Fan a crowd; Dragon's Breath a crowd, one enemy and a pair, Soulfire magic
  resist and insulation (a Knight, a Relay, a Templar) and an Elite or a Boss; Flashover a crowd and a pair, Meltdown
  armor and plate (+13% against a Golem), Thermal Shock
  one enemy with a quarter of the enemies held. Burning what is burn-immune is left out of Soulfire's price: against
  a Magma it adds the whole tower, which no price can follow. A Relay's insulation takes nearly all of a bare
  Scorcher's burn, so its stat nodes count most there.
- **Fair prices** run $7-24 for the stat nodes (White Flame $36) and $14-15 for the Tier I picks, like the other
  next-gen towers' ($0-30). Against what waves pay ($25-40 early, $50-100 mid-level, $100-275 late), every next-gen
  node is cheap: the bundle's balance pass sets the scale, by raising prices or lowering pay.
- **Its names**: Flame, the stream, the Core, Burn, Scorch, Soulfire, Dragon's Breath, Flashover, Meltdown, Thermal
  Shock. Its one enemy debuff is Scorch; the burn itself is the enemy's.

### Bare tower

| Stat | Value | Today | Replaces |
|---|---|---|---|
| Price | $20 | $28 | |
| Flame | 60 fuel a second to each enemy in the cone | a wave every 1 s | |
| Stream | 1 s | | |
| Reload | 2 s | | |
| Range | 2.2 cells | | |
| Cone | 60° | 40° | |
| Crit | none | can crit an ignition | |
| Targets | Nearest; holds its target through a stream while it lives and stays in range | | |
| Passive | Sear: the first touch of each stream gives an enemy 5 stacks of Scorch. | | cinder.extra.fuel.1 |

### Tiers

| Tier | Gate | Unlock | Colour |
|---|---|---|---|
| 0 | - | - | #e07a3a |
| I | XP 60 | $20 | #f0a040 |
| II | XP 120 | - | |
| III | XP 200 | - | |

### Upgrade tree

| Tier | Node | Kind | Cluster | After | Glyph | Colour | Cost | Replaces | Does | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | Hot Flame | stat | | | cinder.head.white_flame.2 | | | cinder.head.white_flame.2 | Flame +15 a second | |
| 0 | Long Stream | stat | | | cinder.extra.fuel.4 | | | | Stream +0.5 s | |
| 0 | Wide Nozzle | stat | | | cinder.head.wide_nozzle.1 | | | cinder.head.wide_nozzle.1 | Cone +50% | |
| 0 | Long Nozzle | stat | | | base.range | | | base.range | Range +0.5 cells | The one range node outside the Nozzle pick. |
| I | Core | ability | | | cinder.head.white_flame.4a | | | | The middle quarter of the cone pours 40% more Flame. | The Scorcher's crit: the target sits in the Core, so it is one enemy against the crowd. At twice the Flame it passed the Sniper on one enemy from Tier I. |
| I | Lance | stat | Nozzle | | base.range | | | base.range.2 | Range +0.65 cells, cone 40% narrower | Further: one enemy, a boss. |
| I | Fan | stat | Nozzle | | cinder.head.wide_nozzle.2 | | | cinder.head.wide_nozzle.2 | Cone +40% | Wider: a crowd. With -0.5 cells of range it lost to Lance everywhere, since range sets how long every enemy burns; at +60% the Fan builds led the Sniper by 37-43%. |
| I | Kindling | ability | | | Scorch | | | | Sear gives 10 stacks of Scorch, up from 5. | Utility: the towers around it crit more often. It adds no damage of its own, so the model counts nothing for it. |
| II | Soulfire | ability | Flame | | Soulfire | | | cinder.head.white_flame.3 | All its fire is Soulfire, Flame +10%; it passes half of a magic resist. | The soul-scorcher: its own look. As a burn with no more Flame it won only on the burn-immune, a counter no price can follow; with half the magic resist passed and no more Flame it priced at $7 against Dragon's Breath's $16. |
| II | Dragon's Breath | ability | Flame | | cinder.head.wide_nozzle.4b | | | cinder.head.wide_nozzle.4b | The stream runs twice as long and reloads in 1 s, at 65% of the Flame. | The long stream: twice as long as the tower's, then 1 s to breathe. With no reload at 55% it left Bellows nothing to do. |
| II | White Flame | stat | | | cinder.head.white_flame.1 | | | cinder.head.white_flame.1 | Flame +30 a second | The Tier II damage node; the flame's core burns white, in either kind of fire. |
| II | Bellows | stat | | | cinder.head.wide_nozzle.3 | | | | Reload -0.5 s | 2 s to 1.5 s, and Dragon's Breath's 1 s to 0.5 s: about 9% to either. At -0.25 s it added 5%; at -0.5 s about 12%, with the Core at +40% and Lance at +0.65 cells to stay under the Sniper. In Tier I it put Lance builds above the Sniper on one enemy; Tier II holds it. |
| III | Flashover | ability | Burning | | cinder.head.white_flame.4b | | | cinder.head.white_flame.4b | While it streams, an enemy in the cone whose burn holds 100 fuel or more flashes over: every enemy within 1.5 cells of it takes half the Flame too. | The crowd pick, and the uncapped burn's payoff: a hot pool spreads. Passing on a dead enemy's fuel added about 1% to a Scorcher alone, whose burns end before their enemies die. A Dragon's Breath alone never reaches the threshold (its pools settle near 90 fuel); two Scorchers on one crowd pour into the same pools and do, worth +57% on a crowd of 1000, which prices it at $22 against $10 for one. Sensitive to the threshold: at 75 fuel it is worth nearly twice as much. |
| III | Meltdown | ability | Burning | | cinder.special.pyromancers_mark | | | cinder.special.pyromancers_mark | A burning enemy's armor and plating burn it: each point adds 0.6% to its burn. | The armored pick: the knight cooks in his armor. A pure counter, priced halfway between Walkers (nothing) and a Knight (+11%); with a floor of 5% it outpriced the other two by half. A Sunder stack a second only helped the physical towers around it, about 2% against a Knight. |
| III | Thermal Shock | ability | Burning | | cinder.special.thermal_shock | | | cinder.special.thermal_shock | The flame pours 35% more Flame into an enemy that is Chilled, Frozen or Dazed. | The pick beside towers that hold enemies, weighed with a quarter of them held. With the Core, ×1.9 on a held target. |
| III | Hot Flame II | stat | | | cinder.head.white_flame.2 | | | | Flame +15 a second | |
| III | Long Stream II | stat | | | cinder.extra.fuel.4 | | | | Stream +1 s | At +0.5 s it added 5%; at +1 s about 9%. |

### Terms

| Term | Glyph | Means |
|---|---|---|
| Flame | cinder.head.white_flame.2 | Fuel a second the stream pours into each enemy in the cone. |
| Stream | | How long the Scorcher fires before it reloads. It keeps its target the whole stream while that enemy lives and stays in range, and picks the nearest again if it dies or leaves. With no enemy left in range it runs its time out into the air. |
| Core | cinder.head.white_flame.4a | The middle quarter of the cone's width; the target always stands in it. |
| Burn | | An enemy's own pool of fuel. It burns at a rate that grows with the square root of the fuel it holds, so it deals exactly that fuel: 100 fuel in 1.75 s, 400 in 3.5 s, 1600 in 7 s. Fuel added joins the pool, whoever adds it; there is no cap. Each pulse (four a second) meets magic resist and insulation; tenacity shrinks fuel as it comes in. It goes past shields, as all damage over time does. |
| Soulfire | | A pool like Burn's, by the same rule, that also burns what is burn-immune and earns Sicken as well as Scorch. A Scorcher that pours it burns hotter and passes half of a magic resist. |
| Dragon's Breath | cinder.head.wide_nozzle.4b | A stream twice as long with a short reload, at a weaker Flame. |
| Flashover | cinder.head.white_flame.4b | A hot burn in the stream spreads: the enemies beside it take part of the Flame too. |
| Thermal Shock | cinder.special.thermal_shock | More Flame on an enemy another tower holds. |
| Meltdown | cinder.special.pyromancers_mark | A burning enemy's armor and plating add to its burn, a share for each point. |

### Look

A squat flamethrower nozzle on a round tank. Keeps its orange, softened (`FEATURE-next-gen-tower-looks.md`); the
foundation grows a step richer per tier.

- **Foundation by tier** (proposed in the bundle review, for the art sheet to try):

  | Tier | Foundation |
  |---|---|
  | 0 | one round fuel tank under the nozzle |
  | I | two tanks joined by a hose |
  | II | the tanks on a brazier stand over a grate |
  | III | a squat furnace with the tanks at its back, its vents glowing while it streams |

  With Soulfire the metal blackens and the glow turns violet, from the same shapes.
- **The stream** is a flame cone, licking and flickering from the nozzle to its range, hottest (yellow-white) at the
  nozzle and red at its tips; its Core reads as a brighter band down the middle. It plays the whole stream and stops
  sharply at the reload, so the board shows when it fires.
- **Reload**: the nozzle glows down from red to dark and a pilot flame flickers at its tip.
- **White Flame**: the core of the stream burns white, in either kind of fire.
- **Soulfire** changes the whole look: a dark violet flame with a pale, soul-coloured core, slower and heavier, as if
  it burns the soul rather than the body. The tower's nozzle and foundation take the soul colour.
- **Dragon's Breath**: a thinner, long stream with a short breath between.
- **A burning enemy** keeps today's burn marker; a Soulfire enemy its own.
- **Flashover**: tongues of flame leap from a hot enemy in the stream to the ones beside it.

## Interconnections

- `FEATURE-next-gen-upgrade-tiers.md` (its node rules), `FEATURE-next-gen-tower-looks.md`. It uses no abilities and
  no shot flight (the stream lands as it pours).
- **One pool rule for burn, poison and Soulfire.** All three are today's `FuelPool` in `ActiveEffects`, so the new
  rule (fuel is total damage, a square-root rate, no cap, tenacity on the way in) lands on every pool. Today's other
  feeders move to fuel: the Mortar's Napalm, Tar and Fallout (`FEATURE-next-gen-mortar-tower.md`), the Burst's Blight
  (`FEATURE-next-gen-splash-tower.md`) and the enemies' affixes. Each gets the fuel its application deals today;
  balance waits for the bundle's pass. Bleed is its own rule and stays.
- `PoolTuning` (how much a pool holds, how fast it marks, what it carries) goes: no tower tunes a pool any more.
- Scorch keeps its rule: a pool earns a stack when it starts and every half second while it burns, so a long burn
  earns more.
- `td/tower/cinder/CLAUDE.md` describes ignitions, the `Burning` ledger, Stoke, Soulfire beside the burn and
  `PoolTuning`; those lines change with the stream.

## Decisions made

- **A stream, not a wave.** Each tick in the cone adds Flame to the enemy's burn, so a longer stream or a stronger
  flame deals more, and the enemy needs nothing from the tower.
- **No cap; a square-root rate.** A pool of F fuel burns at a rate growing with √F and empties in a time growing with
  √F: four times the fuel burns twice as long and twice as hard, and it ends cleanly, with no long tail. Today's pool
  decays exponentially under a soft cap, so a bigger one mostly burned harder, and a heavy build hit the cap.
- **Tenacity shrinks fuel as it comes in**: one number, and the burn's time and damage both shrink with it.
- **Shields don't hold a burn**: all damage over time goes past shields, burn, poison, Soulfire and Bleed alike.
  Shields as a counter to fire would be a rule of its own and make the Scorcher weak where it is the answer today.
- **No crit.** A crit rolled twenty times a second averages out where the player can't see it; the Core is the
  Scorcher's crit, in space instead of chance.
- **No fork.** Soulfire is a Tier II pick beside Dragon's Breath, not a side with its own tree: two sets of nodes
  would mean more names for one change of fire.
- **Sear is the passive**, today's Kindling idea: a debuff that helps the towers around it (Scorch lowers resilience,
  so crits come more often), like the Mortar's Crack, and that leaves the burn alone. Kindling is Tier I's utility:
  it doubles Sear, a choice for a Scorcher that serves the towers around it.
- **Dragon's Breath breathes.** A stream that never stopped left Bellows nothing to do; a stream twice as long with a
  1 s reload keeps Bellows worth the same to it as to any other build, and 65% of the Flame keeps it even with
  Soulfire.
- **A stream runs its time out.** With no enemy left in range it flames on into the air and reloads as usual: one
  fixed rhythm on the board, and a stream starts only when an enemy is in range. Stopping early changed about 1%.
- **No Tank.** Stream and reload stay plain numbers; a fuel tank drawn on the tower was one more thing to learn.
- **White Flame is a stat with a look**: the stream's core burns white, in either kind of fire, so it combines with
  Soulfire without a rule.
- **Dropped**: Stoke, today's Bellows (the Aura cone rule; the name now belongs to the Tier II reload node), Cauterize, Heat, Everburn, Lingering Flames
  (no zones), Inferno Ring, Flashpoint, Searing Flame, White Flame II, Pyromancer's Mark as a magic mark.

## Open questions

⭐ marks the preferred option.

1. ✅ **Tier gates.** XP 60, 120 and 200, as every tower (`FEATURE-next-gen.md`, "Answered by the user").
2. ✅ **Waves against Tier III.** Left to the balance pass (final review, by "Balance waits"). Two Tier III Scorchers (about $470) burn every Walker of a 300-health crowd at the
   default spacing, which brings only about 270 health a second; one alone burns 62% of it. Per dollar it matches the
   Sniper, so the question is the waves', not the tower's: whether a wave a player meets with that much bought holds
   more. The paper can't say; `BalanceReport` answers it once the tower is in the game.
3. ✅ **Burning is kept as drafted** and retuned in the live game, once the Scorcher is built (the user, 2026-11-14).
   It reads `rebalance` on paper: since Dragon's Breath breathes, Flashover prices $22 against Meltdown's $13 and
   Thermal Shock's $12. Half the Flame flashing over, down to 40%, prices it $16 and the reach build's crowd at
   Tier III +51%, from +60%, if the game asks for it.
4. ✅ **Meltdown's name.** The Scorcher keeps it; the Obelisk's is Deep Toll (the user, 2026-11-13).
